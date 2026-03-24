package cn.wyq.serverwebsocket.mapper;

import cn.wyq.serverwebsocket.pojo.entity.Chat;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ChatMapper {

    // 查询用户的会话列表
    List<Chat> selectByUserId(String userId);

    // 新增一条会话记录
    void insertSession(@Param("id") String id, @Param("userId") String userId, @Param("title") String title);

    // 根据 ID 删除会话
    void deleteById(String id);
}
