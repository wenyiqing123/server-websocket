package cn.wqk.serverwebsocket.mapper;


import cn.wqk.serverwebsocket.pojo.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserMapper {
    @Select("select path from user where user_name=#{username}")
    String getStatusAndPathByUserName(String username);

    //登录
    @Select("select * from user where user_name=#{userName}")
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
}
