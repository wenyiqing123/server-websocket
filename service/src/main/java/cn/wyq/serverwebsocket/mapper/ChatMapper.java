package cn.wyq.serverwebsocket.mapper;

import cn.wyq.serverwebsocket.pojo.entity.Chat;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * ChatMapper：MyBatis 数据访问接口，负责数据库查询与持久化操作。
 */
@Mapper
public interface ChatMapper {

    /** 按不可变用户编号查询会话列表。 */
    List<Chat> selectByOwnerUserId(@Param("ownerUserId") Integer ownerUserId);

    /** 新增一条内置智能体会话记录。 */
    void insertSession(@Param("id") String id, @Param("userName") String userName,
                       @Param("ownerUserId") Integer ownerUserId, @Param("title") String title);

    /** 新增绑定自定义智能体的会话。 */
    void insertAgentSession(@Param("id") String id, @Param("userName") String userName,
                            @Param("ownerUserId") Integer ownerUserId, @Param("title") String title,
                            @Param("agentId") Long agentId,
                            @Param("promptVersion") Integer promptVersion);

    // 根据 ID 删除会话
    void deleteById(String id);

    /** 清理指定用户的全部 AI 会话，用于账户物理删除。 */
    void deleteByOwnerUserId(@Param("ownerUserId") Integer ownerUserId);

    Chat selectById(String id);

}
