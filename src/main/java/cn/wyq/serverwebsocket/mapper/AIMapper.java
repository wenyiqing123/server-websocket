package cn.wyq.serverwebsocket.mapper;

import cn.wyq.serverwebsocket.pojo.dto.ConversationMessageDTO;
import cn.wyq.serverwebsocket.pojo.dto.UpdateConversationNameDTO;
import cn.wyq.serverwebsocket.pojo.entity.Conversation;
import cn.wyq.serverwebsocket.pojo.entity.ConversationMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI 聊天应用的统一 Mapper 接口
 * 负责对话 (Conversation) 和消息 (ConversationMessage) 的所有数据库操作
 * 对应的 SQL 语句定义在 AiMapper.xml 中。
 */
@Mapper
public interface AIMapper {

    // --- 对话 (Conversation) 操作 ---

    /**
     * 查询所有对话，按 update_time 倒序排列。
     * 对应 AiMapper.xml 中的 findAllConversations
     *
     * @return 对话列表
     */
    List<Conversation> findAllConversations(String userName);

    /**
     * 插入新对话。
     * 对应 AiMapper.xml 中的 insertConversation，并利用 useGeneratedKeys 回填 ID
     *
     * @param conversation 要插入的对话实体
     * @return 影响的行数
     */
    int insertConversation(Conversation conversation);

    /**
     * 更新对话的更新时间。
     * 对应 AiMapper.xml 中的 updateConversationUpdateTime
     *
     * @param conversation 包含 ID 和 updateTime 的对话实体
     */



    // --- 消息 (ConversationMessage) 操作 ---

    /**
     * 获取某个对话的所有消息，按消息 ID (时间) 升序排列。
     * 对应 AiMapper.xml 中的 selectMessagesByConversationId
     *
     * @param conversationId 对话ID
     * @return 消息列表
     */
    List<ConversationMessage> selectMessagesByConversationId(@Param("conversationId") Integer conversationId);

    /**
     * 插入一条新消息（用户消息或 AI 消息）。
     * 对应 AiMapper.xml 中的 insertMessage
     *
     * @param conversationMessage 要插入的消息实体
     * @return 影响的行数
     */
    int insertMessage(ConversationMessageDTO conversationMessage);

    void updateConversationName(@Param("updateConversationNameDTO") UpdateConversationNameDTO updateConversationNameDTO, LocalDateTime now);
}