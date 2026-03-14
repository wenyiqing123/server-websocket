package cn.wyq.serverwebsocket.service;


import cn.wyq.serverwebsocket.framework.common.PageResult;
import cn.wyq.serverwebsocket.framework.common.Result;
import cn.wyq.serverwebsocket.pojo.User;
import cn.wyq.serverwebsocket.pojo.dto.UserEmailDto;
import cn.wyq.serverwebsocket.pojo.dto.UserQueryDTO;
import cn.wyq.serverwebsocket.pojo.entity.UserEntity;

import java.util.List;
import java.util.Map;

public interface UserService {
    // 查询所有用户
    PageResult<List<UserEntity>> userList(UserQueryDTO userQueryDTO);

    // 根据ID删除用户
    int deleteUserById(Integer id);

    // 修改用户信息
    int updateUser(UserEntity user);
    Result login(User user);

    User findByToken(String token);

    User findById(int id);

    int register(UserEmailDto userEmailDto);


    List<User> findAllUsers();



    String getPath(String username);

    Map<String, String> refreshToken(String refreshToken);
}
