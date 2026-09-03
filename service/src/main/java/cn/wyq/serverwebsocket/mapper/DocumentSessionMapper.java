package cn.wyq.serverwebsocket.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DocumentSessionMapper {

    @Select("SELECT COUNT(1) FROM ai_chat_session WHERE id = #{sessionId} AND user_id = #{userName}")
    int countOwnedSession(@Param("sessionId") String sessionId, @Param("userName") String userName);

    @Select("SELECT document_id FROM ai_session_document WHERE session_id = #{sessionId} ORDER BY created_at ASC")
    List<String> selectDocumentIds(@Param("sessionId") String sessionId);

    @Delete("DELETE FROM ai_session_document WHERE session_id = #{sessionId}")
    int deleteBySessionId(@Param("sessionId") String sessionId);

    @Delete("DELETE FROM ai_session_document WHERE document_id = #{documentId}")
    int deleteByDocumentId(@Param("documentId") String documentId);

    @Insert("INSERT INTO ai_session_document (session_id, document_id, created_at) VALUES (#{sessionId}, #{documentId}, NOW())")
    int insertAssociation(@Param("sessionId") String sessionId, @Param("documentId") String documentId);
}
