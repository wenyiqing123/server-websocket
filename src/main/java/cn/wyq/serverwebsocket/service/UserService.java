package cn.wyq.serverwebsocket.service;


import cn.wyq.serverwebsocket.framework.common.Result;
import cn.wyq.serverwebsocket.pojo.User;

import java.util.List;

public interface UserService {
    Result login(User user);

    User findByToken(String token);

    User findById(int id);

    int register(User user);


    List<User> findAllUsers();

    User validateToken(String token);

    String getPath(String username);
}
