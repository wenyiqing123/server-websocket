package cn.wqk.serverwebsocket.mapper;


import cn.wqk.serverwebsocket.pojo.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper {
    User login(String userName, String password);
}
