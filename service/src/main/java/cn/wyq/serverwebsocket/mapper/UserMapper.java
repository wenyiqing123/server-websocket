package cn.wyq.serverwebsocket.mapper;


import cn.wyq.serverwebsocket.pojo.User;
import cn.wyq.serverwebsocket.pojo.dto.UserQueryDTO;
import cn.wyq.serverwebsocket.pojo.dto.UserQueryExportDTO;
import cn.wyq.serverwebsocket.pojo.entity.UserEmailApplyEntity;
import cn.wyq.serverwebsocket.pojo.entity.UserEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.pagehelper.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    // 查询所有
    Page<UserEmailApplyEntity> selectAllUsers(UserQueryDTO userQueryDTO);

    Page<UserEntity> selectAllAppendUsers(UserQueryDTO userQueryDTO);

    // 删除
    int deleteById(Integer id);

    // 动态修改
    int updateUser(UserEntity user);

    @Select("select path from user where user_name=#{username}")
    String getStatusAndPathByUserName(String username);

    //登录

    User login(User user, Integer applyRole);

    //通过token查询用户
    User findByToken(String token);

    //通过userId查询用户
    User findById(int id);

    //注册
    int register(UserEmailApplyEntity userEmailApplyEntity);

    User findByUsername(String userName);

    List<User> findAllUsers();


    @Update("UPDATE user SET status = 1 WHERE id = #{userId}")
    int approveAdmin(Integer userId);

    @Update("UPDATE user SET status = 2 WHERE id = #{userId}")
    int rejectAdmin(Integer userId);

    String getPath(String username);

    List<UserEmailApplyEntity> export(UserQueryExportDTO userQueryExportDTO);

    List<Integer> findAllUserIds();

    UserEmailApplyEntity findOneByUsername(String username);

    String findEmailByUsername(String userName);

    UserEmailApplyEntity selectUserByEmail(String email);

    @Select("SELECT * FROM user WHERE status = 0")
    List<UserEmailApplyEntity> selectPendingAdminList();

    @Select("select  * from user where  id=#{currentId}")
    UserEmailApplyEntity getCurrentUserById(int currentId);

    @Update("update user set email=#{email} where id=#{id};")
    void updateEmailById(UserEmailApplyEntity currentUser);

    @Update("update user set user_name=#{userName},path=#{path} where id=#{id}")
    void updateUsernameAvatar(UserEmailApplyEntity existingUser);

    @Update("update user set password=#{password} where id=#{id}")
    void updatePasswordById(UserEmailApplyEntity user);

    @Select("select path from user where email=#{email};")
    String getPathByEmail(String email);

    @Update("update chat_group set owner_name =#{newUserName}  where owner_name=#{oldUserName}")
    int updateUserNameInGroupOwner(String oldUserName, String newUserName);

    @Update("UPDATE friend SET " +
            "user_name = CASE WHEN user_name = #{oldUserName} THEN #{newUserName} ELSE user_name END, " +
            "friend_name = CASE WHEN friend_name = #{oldUserName} THEN #{newUserName} ELSE friend_name END " +
            "WHERE user_name = #{oldUserName} OR friend_name = #{oldUserName}")
    int updateUserNameInFriend(String oldUserName, String newUserName);

    @Update("update group_member set user_name =#{newUserName}  where user_name=#{oldUserName}")
    int updateUserNameInGroupMember(String oldUserName, String newUserName);

    @Update("UPDATE message SET " +
            "from_name = CASE WHEN from_name = #{oldUserName} THEN #{newUserName} ELSE from_name END, " +
            "to_name = CASE WHEN to_name = #{oldUserName} THEN #{newUserName} ELSE to_name END " +
            "WHERE from_name = #{oldUserName} OR to_name = #{oldUserName}")
    int updateUserNameInMessage(String oldUserName, String newUserName);

    @Update("update conversation set user_name=#{newUserName} where user_name=#{oldUserName}")
    int updateUserNameInConversation(String oldUserName, String newUserName);

    int insertBatch(@Param("list") List<UserEmailApplyEntity> list);}
