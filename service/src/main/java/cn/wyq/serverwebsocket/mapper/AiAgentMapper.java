package cn.wyq.serverwebsocket.mapper;

import cn.wyq.serverwebsocket.pojo.entity.AiAgent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 智能体数据访问接口。
 */
@Mapper
public interface AiAgentMapper {
    List<AiAgent> selectByOwner(@Param("ownerUserId") Integer ownerUserId);

    AiAgent selectById(@Param("id") Long id);

    AiAgent selectOwned(@Param("id") Long id, @Param("ownerUserId") Integer ownerUserId);

    /** 查询当前用户拥有的智能体，允许软删除记录用于读取和清理历史会话。 */
    AiAgent selectOwnedIncludingDeleted(@Param("id") Long id, @Param("ownerUserId") Integer ownerUserId);

    int countByOwner(@Param("ownerUserId") Integer ownerUserId);

    int insert(AiAgent agent);

    int updateOwned(AiAgent agent);

    int softDeleteOwned(@Param("id") Long id, @Param("ownerUserId") Integer ownerUserId);

    int incrementUseCount(@Param("id") Long id);

    List<AiAgent> selectForAdmin(@Param("ownerUserName") String ownerUserName,
                                 @Param("name") String name,
                                 @Param("status") Integer status);

    int updateByAdmin(AiAgent agent);

    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    int softDeleteByAdmin(@Param("id") Long id);
}
