package cn.wqk.serverwebsocket.service.impl;

import cn.wqk.serverwebsocket.mapper.UserMapper;
import cn.wqk.serverwebsocket.pojo.User;
import cn.wqk.serverwebsocket.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserMapper userMapper;

    @Override
    public User login(User user) {
        return userMapper.login(user.getUserName(),user.getPassword());
    }
}
