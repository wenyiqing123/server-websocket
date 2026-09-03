package cn.wyq.serverwebsocket.service;


import cn.wyq.serverwebsocket.common.PageResult;
import cn.wyq.serverwebsocket.common.Result;
import cn.wyq.serverwebsocket.pojo.User;
import cn.wyq.serverwebsocket.pojo.dto.*;
import cn.wyq.serverwebsocket.pojo.entity.UserEmailApplyEntity;
import cn.wyq.serverwebsocket.pojo.entity.UserEntity;

import java.net.MalformedURLException;
import java.util.List;
import java.util.Map;

/**
 * UserService：业务服务接口，定义对应领域的业务操作契约。
 */
public interface UserService {
    // 查询所有用户
    PageResult<List<UserEmailApplyEntity>> userList(UserQueryDTO userQueryDTO);
    PageResult<List<UserEntity>> userAppendList(UserQueryDTO userQueryDTO);

    // 根据ID删除用户
    int deleteUserById(Integer id);

    // 修改用户信息
    int updateUser(UserEntity user);
    Result login(User user,Integer applyRole);
    /**
     * 换绑/修改邮箱
     */
    Result updateEmail(UpdateEmailDto dto);
    void importUsers(List<UserImportDTO> dtoList);
// 在你的 UserService.java 中添加：

    /**
     * 根据关键词搜索用户
     * @param keyword 搜索关键词
     * @return 搜索结果
     */
    Result searchUser(String keyword);
    User findByToken(String token);

    User findById(int id);

    int register(UserEmailDto userEmailDto,Integer applyRole);


    List<User> findAllUsers();



    String getPath(String username);

    Map<String, String> refreshToken(String refreshToken);

    List<UserExportDTO> export(UserQueryExportDTO userQueryExportDTO) throws MalformedURLException;

    void updatePassword(UserUpdatePasswordDTO userUpdatePasswordDTO);
    /**
     * 获取所有待审批的管理员申请列表
     */
    List<UserEmailApplyEntity> getPendingAdmins();

    /**
     * 审批通过：将用户状态从 0 改为 1
     */
    boolean approveAdmin(Integer userId);

    /**
     * 审批拒绝：将用户状态从 0 改为 2
     */
    boolean rejectAdmin(Integer userId);

    UserEmailApplyEntity getCurrentUserById(int currentId);
    Result loginByEmail(EmailLoginDto dto);
    Result forgotPassword(ForgotPasswordDto dto);


    String getPathByEmail(String email);

    void updateUsernameInGroupAndFriend(String oldUserName, String newUserName);
}
