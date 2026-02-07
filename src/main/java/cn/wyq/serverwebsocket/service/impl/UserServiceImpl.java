package cn.wyq.serverwebsocket.service.impl;

import cn.wyq.serverwebsocket.framework.common.PageResult;
import cn.wyq.serverwebsocket.framework.common.Result;
import cn.wyq.serverwebsocket.framework.exception.ServiceException;
import cn.wyq.serverwebsocket.mapper.UserMapper;
import cn.wyq.serverwebsocket.pojo.User;
import cn.wyq.serverwebsocket.pojo.dto.UserQueryDTO;
import cn.wyq.serverwebsocket.pojo.entity.UserEntity;
import cn.wyq.serverwebsocket.pojo.vo.UserVo;
import cn.wyq.serverwebsocket.service.UserService;
import cn.wyq.serverwebsocket.utils.JWTUtil;
import cn.wyq.serverwebsocket.utils.RedisUtil;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private RedisUtil redisUtil;
//    @Autowired
//    private AuthenticationManager authenticationManager;


    @Override
    public PageResult<List<UserEntity>> userList(UserQueryDTO userQueryDTO) {
        PageHelper.startPage(userQueryDTO.getPage(), userQueryDTO.getPageSize());
        Page<UserEntity> page = userMapper.selectAllUsers(userQueryDTO);
        long total = page.getTotal();
        List<UserEntity> userList = page.getResult();
        return PageResult.success(total, userList);
    }

    @Override
    public int deleteUserById(Integer id) {
        return userMapper.deleteById(id);
    }

    @Override
    public int updateUser(UserEntity user) {
        return userMapper.updateUser(user);
    }

    @Override
    public Result login(User user) {
        User loginUser = userMapper.login(user);
        if(loginUser==null)  throw new ServiceException("用户名或密码错误",401);
        if (!passwordEncoder.matches(user.getPassword(), loginUser.getPassword())) {
            return Result.error("用户名或密码错误");
        } else {
            String accessToken = JWTUtil.generateToken(loginUser);
            String refreshToken = JWTUtil.generateRefreshToken(loginUser);
            UserEntity userEntity = new UserEntity();
            BeanUtils.copyProperties(loginUser, userEntity);
            UserVo userVo = new UserVo(accessToken, refreshToken, userEntity);
            /**
             * K:userId
             * V:token
             * TimeOut: 1
             * TimeUnit: hour
             */
            redisUtil.set("accessToken:userId:" + loginUser.getId(), accessToken, 30, TimeUnit.MINUTES);
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
        // 2. 获取用户信息
        int userId = JWTUtil.getUserId(refreshToken);
        String username = JWTUtil.getUsername(refreshToken);
        User user = new User().builder().id(userId).userName(username).build();
        // 3. 生成新的 Access Token
        String accessToken = JWTUtil.generateToken(user);
        // 4. (可选) 处于安全考虑，有时也会同时轮换 Refresh Token
        String newRefreshToken = JWTUtil.generateRefreshToken(user);
        // 5. 更新 Redis 中的 Refresh Token
        redisUtil.set("refreshToken:userId:" + userId, newRefreshToken, 7, TimeUnit.DAYS);
        // 5. 更新 Redis 中的 Access Token
        redisUtil.set("accessToken:userId:" + userId, accessToken, 30, TimeUnit.MINUTES);
        // 6. 返回新的 Access Token 和 Refresh Token
        Map<String, String> map = new HashMap<>();
        map.put("accessToken", accessToken);
        map.put("refreshToken", newRefreshToken);
        return map;
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
     * @param user 包含用户注册信息的 User 对象
     * @return 若用户名已存在，返回 0；若注册成功，返回 UserMapper.register 方法的执行结果（通常为受影响的数据库记录行数）
     */
    @Override
    public int register(User user) {
        // 根据传入用户对象的用户名，从数据库中查找对应的用户记录
        User byUsername = userMapper.findByUsername(user.getUserName());
        // 若查找到的用户记录不为 null，说明该用户名已存在
        if (byUsername != null) {
            // 用户名已存在，返回 0 表示注册失败
            return 0;
        }
        // 用户名不存在，调用 UserMapper 的 register 方法执行注册操作，并返回注册结果
        return userMapper.register(user);
    }

}
