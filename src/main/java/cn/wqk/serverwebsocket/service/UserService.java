package cn.wqk.serverwebsocket.service;


import cn.wqk.serverwebsocket.framework.common.Result;
import cn.wqk.serverwebsocket.pojo.User;
import jakarta.servlet.http.HttpSession;

import java.util.List;

public interface UserService {
    Result login(User user, HttpSession session);

    User findByToken(String token);

    User findById(int id);

    int register(User user);


    List<User> findAllUsers();
}
