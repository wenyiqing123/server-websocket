package cn.wyq.serverwebsocket.mapper;


import cn.wyq.serverwebsocket.annotation.AutoFill;
import cn.wyq.serverwebsocket.enumeration.OperationType;
import cn.wyq.serverwebsocket.pojo.entity.Test;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TestMapper {

    @AutoFill(OperationType.INSERT)
    @Insert("insert into test (username,create_time,create_user,update_time,update_user) values (#{username},#{createTime},#{createUser},#{updateTime},#{updateUser})")
    void insert(Test test);
}
