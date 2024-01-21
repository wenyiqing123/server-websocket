package cn.wqk.serverwebsocket.mapper;


import cn.wqk.serverwebsocket.pojo.User;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface UserMapper {
    //通过token查询用户
    User findByToken(String token);

    //通过userId查询用户
    User findById(int id);

    //注册
    int register(User user);

    User findByUsername(String userName);

    List<User> findAllUsers();

}
