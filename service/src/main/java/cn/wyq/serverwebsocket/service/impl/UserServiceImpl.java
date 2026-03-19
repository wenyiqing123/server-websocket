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
import org.springframework.data.redis.core.RedisTemplate;
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

    // 💡 核心新增：注入 RedisTemplate 用于手动精细化控制缓存
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    @SuppressWarnings("unchecked")
    public PageResult<List<UserEntity>> userList(UserQueryDTO userQueryDTO) {
        log.info("userQueryDTO:{}", userQueryDTO);

        // 1. 提取缓存条件判断 (替代原 condition 注解)
        // 确保只有在查第一页且无任何筛选条件时，才走缓存
        boolean isCacheable = (userQueryDTO.getId() == null || String.valueOf(userQueryDTO.getId()).isEmpty()) &&
                userQueryDTO.getPage() == 1 &&
                userQueryDTO.getPageSize() == 9 &&
                (userQueryDTO.getUserName() == null || userQueryDTO.getUserName().isEmpty()) &&
                (userQueryDTO.getRole() == null || userQueryDTO.getRole()== 0);

        // 保持原本 Spring Cache 生成的 Key 格式完全一致
        String cacheKey = RedisKeyConstants.MANAGE_USERS_CACHE + ":page=1,pageSize=9:list";

        // 2. 查缓存
        if (isCacheable) {
            Object cachedData = redisTemplate.opsForValue().get(cacheKey);
            if (cachedData != null) {
                log.debug("命中用户首页分页缓存！");
                return (PageResult<List<UserEntity>>) cachedData;
            }
        }

        // 3. 缓存未命中，查数据库
        PageHelper.startPage(userQueryDTO.getPage(), userQueryDTO.getPageSize());
        Page<UserEntity> page = userMapper.selectAllUsers(userQueryDTO);
        long total = page.getTotal();
        List<UserEntity> userList = page.getResult();
        PageResult<List<UserEntity>> pageResult = PageResult.success(total, userList);

        // 4. 🛡️ 核心防御：写缓存并防穿透
        if (isCacheable) {
            if (userList == null || userList.isEmpty()) {
                // 若数据库为空，存入一个极短命 (1 分钟) 的空结果外壳
                redisTemplate.opsForValue().set(cacheKey, pageResult, 1, TimeUnit.MINUTES);
                log.info("用户列表为空，已存入空缓存防穿透");
            } else {
                // 正常数据，存入 2 小时
                redisTemplate.opsForValue().set(cacheKey, pageResult, 2, TimeUnit.HOURS);
            }
        }

        return pageResult;
    }

    @Override
    public int deleteUserById(Integer id) {
        int result = userMapper.deleteById(id);

        // 🧹 手动清除缓存 (替代 @CacheEvict)
        String cacheKey = RedisKeyConstants.MANAGE_USERS_CACHE + ":page=1,pageSize=9:list";
        redisTemplate.delete(cacheKey);

        return result;
    }

    @Override
    public int updateUser(UserEntity user) {
        int result = userMapper.updateUser(user);

        // 🧹 手动清除缓存 (替代 @CacheEvict)
        String cacheKey = RedisKeyConstants.MANAGE_USERS_CACHE + ":page=1,pageSize=9:list";
        redisTemplate.delete(cacheKey);

        return result;
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

            // ✅ 将 refreshToken 存入 Redis，保留踢人或单点登录的控制权
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
        return userMapper.getPath(username);
    }

    @Override
    public Map<String, String> refreshToken(String refreshToken) {
        if (!JWTUtil.verifyToken(refreshToken)) {
            throw new ServiceException("登录已失效，请重新登录", 401);
        }

        int userId = JWTUtil.getUserId(refreshToken);
        String redisRt = redisUtil.get("refreshToken:userId:" + userId);
        if (redisRt == null || !redisRt.equals(refreshToken)) {
            throw new ServiceException("账号已在其他设备登录或被强制下线", 401);
        }

        String username = JWTUtil.getUsername(refreshToken);
        User user = new User().builder().id(userId).userName(username).build();

        String accessToken = JWTUtil.generateToken(user.getId(), user.getUserName());
        String newRefreshToken = JWTUtil.generateRefreshToken(user.getId(), user.getUserName());

        redisUtil.set("refreshToken:userId:" + userId, newRefreshToken, 7, TimeUnit.DAYS);

        Map<String, String> map = new HashMap<>();
        map.put("accessToken", accessToken);
        map.put("refreshToken", newRefreshToken);
        return map;
    }

    @Override
    public List<UserExportDTO> export(UserQueryExportDTO userQueryExportDTO) {
        List<UserEntity> userList = userMapper.export(userQueryExportDTO);
        List<UserExportDTO> exportDTOList = new ArrayList<>();

        for (UserEntity user : userList) {
            UserExportDTO dto = new UserExportDTO();
            dto.setId(user.getId());
            dto.setUserName(user.getUserName());
            dto.setRole(user.getRole());

            String rawPath = user.getPath();
            if (rawPath != null && !rawPath.trim().isEmpty()) {
                try {
                    dto.setPath(new java.net.URL(rawPath));
                } catch (MalformedURLException e) {
                    log.error("用户 [{}] 的头像路径格式非法: {}", user.getUserName(), rawPath);
                    dto.setPath(null);
                }
            } else {
                dto.setPath(null);
            }
            exportDTOList.add(dto);
        }
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

    @Override
    public int register(UserEmailDto userEmailDto) {
        String code = userEmailDto.getCode();
        String email = userEmailDto.getEmail();
        boolean flag = mailUtil.verifyCode(email, code);
        if (!flag) throw new ServiceException("验证码错误",400);

        User byUsername = userMapper.findByUsername(userEmailDto.getUserName());
        if (byUsername != null) {
            return 0;
        }
        User user = new User();
        BeanUtils.copyProperties(userEmailDto,user);
        int result = userMapper.register(user);

        // 🧹 手动清除缓存 (注册了新用户，第一页的列表数据可能发生了变化)
        String cacheKey = RedisKeyConstants.MANAGE_USERS_CACHE + ":page=1,pageSize=9:list";
        redisTemplate.delete(cacheKey);

        return result;
    }
}