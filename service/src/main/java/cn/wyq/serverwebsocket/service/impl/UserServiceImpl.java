package cn.wyq.serverwebsocket.service.impl;


import cn.wyq.serverwebsocket.common.PageResult;
import cn.wyq.serverwebsocket.common.Result;
import cn.wyq.serverwebsocket.constant.RedisKeyConstants;
import cn.wyq.serverwebsocket.exception.ServiceException;
import cn.wyq.serverwebsocket.mapper.UserMapper;
import cn.wyq.serverwebsocket.pojo.User;
import cn.wyq.serverwebsocket.pojo.dto.UserEmailDto;
import cn.wyq.serverwebsocket.pojo.dto.UserExportDTO;
import cn.wyq.serverwebsocket.pojo.dto.UserQueryDTO;
import cn.wyq.serverwebsocket.pojo.dto.UserQueryExportDTO;
import cn.wyq.serverwebsocket.pojo.entity.UserEntity;
import cn.wyq.serverwebsocket.pojo.vo.UserVo;
import cn.wyq.serverwebsocket.service.UserService;
import cn.wyq.serverwebsocket.utils.JWTUtil;
import cn.wyq.serverwebsocket.utils.MailUtil;
import cn.wyq.serverwebsocket.utils.RedisUtil;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class UserServiceImpl implements UserService {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private MailUtil mailUtil;
//    @Autowired
//    private AuthenticationManager authenticationManager;


    @Override
    //删除unless = "#result == null"，防止缓存穿透
    @Cacheable(
            value = RedisKeyConstants.MANAGE_USERS_CACHE,
            key = "'page=1,pageSize=9:list'",
            // 🌟 修复点：每一个字段的 "为空判断" 必须用小括号包起来！
            condition = "(#userQueryDTO.id == null || #userQueryDTO.id == '') && " +
                    "(#userQueryDTO.page == 1) && " +
                    "(#userQueryDTO.pageSize == 9) && " +
                    "(#userQueryDTO.userName == null || #userQueryDTO.userName == '') && " +
                    "(#userQueryDTO.role == null || #userQueryDTO.role == '')"
    )
    public PageResult<List<UserEntity>> userList(UserQueryDTO userQueryDTO) {
        log.info("userQueryDTO:{}", userQueryDTO);
        PageHelper.startPage(userQueryDTO.getPage(), userQueryDTO.getPageSize());
        Page<UserEntity> page = userMapper.selectAllUsers(userQueryDTO);
        long total = page.getTotal();
        List<UserEntity> userList = page.getResult();
        return PageResult.success(total, userList);
    }

    @Override
    @CacheEvict(value = RedisKeyConstants.MANAGE_USERS_CACHE, key = "'page=1,pageSize=9:list'")
    public int deleteUserById(Integer id) {
        return userMapper.deleteById(id);
    }

    @Override
    @CacheEvict(value = RedisKeyConstants.MANAGE_USERS_CACHE, key = "'page=1,pageSize=9:list'")
    public int updateUser(UserEntity user) {
        return userMapper.updateUser(user);
    }

    @Override
    public Result login(User user) {
        User loginUser = userMapper.login(user);
        if (loginUser == null) throw new ServiceException("用户名或密码错误", 401);
        if (!passwordEncoder.matches(user.getPassword(), loginUser.getPassword())) {
            return Result.error("用户名或密码错误");
        } else {
            // 生成双 Token
            String accessToken = JWTUtil.generateToken(loginUser.getId(), loginUser.getUserName());
            String refreshToken = JWTUtil.generateRefreshToken(loginUser.getId(), loginUser.getUserName());

            UserEntity userEntity = new UserEntity();
            BeanUtils.copyProperties(loginUser, userEntity);
            UserVo userVo = new UserVo(accessToken, refreshToken, userEntity);

            // ❌ 删除这行：不再把 accessToken 存入 Redis！
            // redisUtil.set("accessToken:userId:" + loginUser.getId(), accessToken, 30, TimeUnit.MINUTES);

            // ✅ 只保留这行：将 refreshToken 存入 Redis，保留踢人或单点登录的控制权
            redisUtil.set("refreshToken:userId:" + loginUser.getId(), refreshToken, 7, TimeUnit.DAYS);

            return Result.success(userVo);
        }
    }

    @Override
    public List<User> findAllUsers() {
        return userMapper.findAllUsers();
    }


    @Override
    public String getPath(String username) {
        String path = userMapper.getPath(username);
        return path;
    }

    @Override
    public Map<String, String> refreshToken(String refreshToken) {
        if (!JWTUtil.verifyToken(refreshToken)) {
            throw new ServiceException("登录已失效，请重新登录", 401);
        }

        // --- 补充安全校验：必须检查传入的 refreshToken 是否和 Redis 里存的一致！ ---
        int userId = JWTUtil.getUserId(refreshToken);
        String redisRt = redisUtil.get("refreshToken:userId:" + userId);
        if (redisRt == null || !redisRt.equals(refreshToken)) {
            // 如果 Redis 里没有，或者和 Redis 里的不一致（比如在别处登录被顶掉了）
            throw new ServiceException("账号已在其他设备登录或被强制下线", 401);
        }
        // -------------------------------------------------------------

        String username = JWTUtil.getUsername(refreshToken);
        User user = new User().builder().id(userId).userName(username).build();

        // 生成新的双 Token
        String accessToken = JWTUtil.generateToken(user.getId(), user.getUserName());
        String newRefreshToken = JWTUtil.generateRefreshToken(user.getId(), user.getUserName());

        // ✅ 更新 Redis 中的 Refresh Token (续期防盗)
        redisUtil.set("refreshToken:userId:" + userId, newRefreshToken, 7, TimeUnit.DAYS);

        // ❌ 删除这行：不再更新 accessToken 到 Redis
        // redisUtil.set("accessToken:userId:" + userId, accessToken, 30, TimeUnit.MINUTES);

        Map<String, String> map = new HashMap<>();
        map.put("accessToken", accessToken);
        map.put("refreshToken", newRefreshToken);
        return map;
    }

    @Override
    public List<UserExportDTO> export(UserQueryExportDTO userQueryExportDTO) {
        // 1. 从数据库查询原始数据 (此时接收的是数据库实体，path 是 String 类型)
        // 注意：请确保 userMapper.export 返回的是 List<User> 或者包含 String 类型 path 的对象
        List<UserEntity> userList = userMapper.export(userQueryExportDTO);

        List<UserExportDTO> exportDTOList = new ArrayList<>();

        // 2. 遍历并进行安全的数据转换
        for (UserEntity user : userList) {
            UserExportDTO dto = new UserExportDTO();
            dto.setId(user.getId());
            dto.setUserName(user.getUserName());
            dto.setRole(user.getRole());

            // 3. 🛡️ 安全处理图片 URL，防止某一个坏数据导致整体崩溃
            String rawPath = user.getPath();
            if (rawPath != null && !rawPath.trim().isEmpty()) {
                try {
                    // 如果数据库存的不是完整的 http 开头，可以在这里补全，比如：
                    // dto.setPath(new URL("http://localhost:8000" + rawPath));
                    dto.setPath(new java.net.URL(rawPath));
                } catch (MalformedURLException e) {
                    // 仅仅记录日志，不要抛出异常，这样有问题的单元格留空，不影响其他人导出
                    log.error("用户 [{}] 的头像路径格式非法: {}", user.getUserName(), rawPath);
                    dto.setPath(null);
                }
            } else {
                dto.setPath(null); // 没有头像就置空
            }

            exportDTOList.add(dto);
        }

        // 4. 返回处理好的、专供 Excel 导出的 DTO 列表
        return exportDTOList;
    }

    @Override
    public User findByToken(String token) {
        return userMapper.findByToken(token);
    }

    @Override
    public User findById(int id) {
        return userMapper.findById(id);
    }

    /**
     * 实现用户注册功能。
     * 首先检查传入用户的用户名是否已存在于数据库中，若存在则返回 0 表示注册失败；
     * 若不存在则调用 UserMapper 的 register 方法执行注册操作，并返回注册结果。
     *
     * @param userEmailDto 包含用户注册信息的 User 对象
     * @return 若用户名已存在，返回 0；若注册成功，返回 UserMapper.register 方法的执行结果（通常为受影响的数据库记录行数）
     */
    @Override
    @CacheEvict(
            value = RedisKeyConstants.MANAGE_USERS_CACHE,
            key = "'page=1,pageSize=9:list'"
    )
    public int register(UserEmailDto userEmailDto) {
        String code = userEmailDto.getCode();
        String email = userEmailDto.getEmail();
        boolean flag = mailUtil.verifyCode(email, code);
        if (!flag) throw new ServiceException("验证码错误",400);
        // 根据传入用户对象的用户名，从数据库中查找对应的用户记录
        User byUsername = userMapper.findByUsername(userEmailDto.getUserName());
        // 若查找到的用户记录不为 null，说明该用户名已存在
        if (byUsername != null) {
            // 用户名已存在，返回 0 表示注册失败
            return 0;
        }
        User user = new User();
        BeanUtils.copyProperties(userEmailDto,user);
        // 用户名不存在，调用 UserMapper 的 register 方法执行注册操作，并返回注册结果
        return userMapper.register(user);
    }

}
