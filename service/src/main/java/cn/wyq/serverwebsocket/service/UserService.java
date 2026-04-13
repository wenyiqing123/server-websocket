package cn.wyq.serverwebsocket.service;


import cn.wyq.serverwebsocket.common.PageResult;
import cn.wyq.serverwebsocket.common.Result;
import cn.wyq.serverwebsocket.pojo.User;
import cn.wyq.serverwebsocket.pojo.dto.*;
import cn.wyq.serverwebsocket.pojo.entity.UserEntity;

import java.net.MalformedURLException;
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
// 在你的 UserService.java 中添加：

    /**
     * 根据关键词搜索用户
     * @param keyword 搜索关键词
     * @return 搜索结果
     */
    Result searchUser(String keyword);
    User findByToken(String token);

    User findById(int id);

    int register(UserEmailDto userEmailDto);


    List<User> findAllUsers();



    String getPath(String username);

    Map<String, String> refreshToken(String refreshToken);

    List<UserExportDTO> export(UserQueryExportDTO userQueryExportDTO) throws MalformedURLException;

    void updatePassword(UserUpdatePasswordDTO userUpdatePasswordDTO);
}
