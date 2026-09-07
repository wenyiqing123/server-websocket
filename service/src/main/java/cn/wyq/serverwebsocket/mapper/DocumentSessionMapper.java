package cn.wyq.serverwebsocket.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * DocumentSessionMapper：MyBatis 数据访问接口，负责数据库查询与持久化操作。
 */
@Mapper
public interface DocumentSessionMapper {

    @Select("SELECT COUNT(1) FROM ai_chat_session WHERE id = #{sessionId} AND owner_user_id = #{ownerUserId}")
    int countOwnedSession(@Param("sessionId") String sessionId, @Param("ownerUserId") Integer ownerUserId);

    /**
     * 只查询归属于当前用户的文档，历史无归属文档不会随会话暴露给普通用户。
     */
    @Select("SELECT association.document_id "
            + "FROM ai_session_document association "
            + "INNER JOIN sys_document document ON document.id = association.document_id "
            + "WHERE association.session_id = #{sessionId} "
            + "AND document.owner_user_id = #{ownerUserId} "
            + "ORDER BY association.created_at ASC")
    List<String> selectDocumentIds(@Param("sessionId") String sessionId,
                                   @Param("ownerUserId") Integer ownerUserId);

    @Delete("DELETE FROM ai_session_document WHERE session_id = #{sessionId}")
    int deleteBySessionId(@Param("sessionId") String sessionId);

    @Delete("DELETE FROM ai_session_document WHERE document_id = #{documentId}")
    int deleteByDocumentId(@Param("documentId") String documentId);

    @Insert("INSERT INTO ai_session_document (session_id, document_id, created_at) VALUES (#{sessionId}, #{documentId}, NOW())")
    int insertAssociation(@Param("sessionId") String sessionId, @Param("documentId") String documentId);
}
