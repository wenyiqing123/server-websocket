package cn.wyq.serverwebsocket.mapper;


import cn.wyq.serverwebsocket.pojo.User;
import cn.wyq.serverwebsocket.pojo.dto.UserQueryDTO;
import cn.wyq.serverwebsocket.pojo.dto.UserQueryExportDTO;
import cn.wyq.serverwebsocket.pojo.entity.UserEntity;
import com.github.pagehelper.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserMapper {
    // 查询所有
    Page<UserEntity> selectAllUsers(UserQueryDTO userQueryDTO);

    // 删除
    int deleteById(Integer id);

    // 动态修改
    int updateUser(UserEntity user);
    @Select("select path from user where user_name=#{username}")
    String getStatusAndPathByUserName(String username);

    //登录

    User login(User user);

    //通过token查询用户
    User findByToken(String token);

    //通过userId查询用户
    User findById(int id);

    //注册
    int register(User user);

    User findByUsername(String userName);

    List<User> findAllUsers();

    String getPath(String username);

    List<UserEntity> export(UserQueryExportDTO userQueryExportDTO);
    List<Integer> findAllUserIds();
}
