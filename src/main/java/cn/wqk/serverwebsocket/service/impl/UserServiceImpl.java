package cn.wqk.serverwebsocket.service.impl;

import cn.wqk.serverwebsocket.framework.common.Result;
import cn.wqk.serverwebsocket.framework.security.impl.LoginUser;
import cn.wqk.serverwebsocket.mapper.UserMapper;
import cn.wqk.serverwebsocket.pojo.User;
import cn.wqk.serverwebsocket.service.UserService;
import cn.wqk.serverwebsocket.utils.JWTUtil;
import cn.wqk.serverwebsocket.utils.RedisUtil;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private AuthenticationManager authenticationManager;

    @Override
    public Result login(User user, HttpSession session) {
        //传入用户名和密码
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(user.getUserName(), user.getPassword());
        //实现登录逻辑，调用UserDetailsServiceImpl中重写的loadUserByUsername方法
        //返回userDeyails
        Authentication authenticate = null;
        try {
            authenticate = authenticationManager.authenticate(authenticationToken);
        } catch (AuthenticationException e) {
            return Result.error("用户名或密码错误");
        }
        LoginUser loginUser = (LoginUser) authenticate.getPrincipal();
        User user1 = loginUser.getUser();
        //此处采用“普通用户类和用户详细类分开的方式”
        String token = JWTUtil.generateToken(user1);
        RedisUtil redisUtil = new RedisUtil();
        /**
         * K:userId
         * V:token
         * TimeOut: 1
         * TimeUnit: hour
         */
        redisUtil.set(String.valueOf(user1.getId()), token, 1, TimeUnit.HOURS);
        return Result.toToken(token);
    }

    @Override
    public List<User> findAllUsers() {
        return userMapper.findAllUsers();
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
    public int register(User user) {
        User byUsername = userMapper.findByUsername(user.getUserName());
        if (byUsername != null) {
            return 0;
        }
        return userMapper.register(user);
    }
}
