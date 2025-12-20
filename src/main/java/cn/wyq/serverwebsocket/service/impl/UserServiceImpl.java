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

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;
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
        System.out.println("loginUser = " + loginUser);
        if (!passwordEncoder.matches(user.getPassword(), loginUser.getPassword())) {
            return Result.error("用户名或密码错误");
        } else {
            //此处采用“普通用户类和用户详细类分开的方式”
            String token = JWTUtil.generateToken(loginUser);
            UserEntity userEntity = new UserEntity();
            BeanUtils.copyProperties(loginUser,userEntity);
            UserVo userVo = new UserVo(token, userEntity);
            RedisUtil redisUtil = new RedisUtil();
            /**
             * K:userId
             * V:token
             * TimeOut: 1
             * TimeUnit: hour
             */
            redisUtil.set(String.valueOf(loginUser.getId()), token, 1, TimeUnit.HOURS);
            return Result.success(userVo);
        }
    }

    @Override
    public List<User> findAllUsers() {
        return userMapper.findAllUsers();
    }

    @Override
    public User validateToken(String token) {
        if (token == null || token.isEmpty()) {
            throw new ServiceException("请先登录", 401);
        }
        int userId;
        try {
//            userId = JWTUtil.decodeToken(token);
            userId = 1;
        } catch (Exception e) {
            throw new ServiceException("请勿伪造token(解析token失败)", 401);
        }
        RedisUtil redisUtil = new RedisUtil();
        String redisToken = redisUtil.get(String.valueOf(userId));
        if (redisToken == null) {
            throw new ServiceException("登录已过期)", 401);
        }
        if (!redisToken.equals(token)) {
            throw new ServiceException("请勿伪造token(携带的token和登陆拿到的token不一致))", 401);
        }
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new ServiceException("请勿伪造token(解析成功，但userid是伪造的))", 401);
        }
        if (!JWTUtil.verifyToken(token)) {
            throw new ServiceException("登录已过期)", 401);
        }
        return user;
    }

    @Override
    public String getPath(String username) {
        String path = userMapper.getPath(username);
        return path;
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
