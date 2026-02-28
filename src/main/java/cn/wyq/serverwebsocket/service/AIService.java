package cn.wyq.serverwebsocket.service;

import cn.wyq.serverwebsocket.pojo.dto.ConversationMessageDTO;
import cn.wyq.serverwebsocket.pojo.dto.UpdateConversationNameDTO;
import cn.wyq.serverwebsocket.pojo.entity.Conversation;
import cn.wyq.serverwebsocket.pojo.entity.ConversationMessage;
import cn.wyq.serverwebsocket.pojo.entity.UserEntity;

import java.util.List;

/**
 * AI 聊天服务的业务逻辑接口
 * 职责：专注于对话和消息的存储与查询
 */
public interface AIService {

    /**
     * 获取历史对话列表。
     * 对应 Controller: @GetMapping("/history")
     *
     * @return 对话列表，按更新时间倒序
     */
    List<Conversation> list(String userName);

    /**
     * 获取指定对话ID下的所有消息详情。
     * 对应 Controller: @GetMapping("/messages/{conversationId}")
     *
     * @param conversationId 对话ID
     * @return 消息列表，按时间顺序排列
     */
    List<ConversationMessage> getMessages(Integer conversationId);

    /**
     * 创建一个新的对话。
     * 对应 Controller: @PostMapping("/new")
     *
     * @param username 新对话的默认名称（如 "新对话"）
     * @return 新创建对话的 ID
     */
    Integer createConversation(String username);

    // --- 以下是为前后端分离存储设计的接口 ---



    /**
     * 保存消息
     *
     * @param conversationMessageDTO
     */
    void saveConversationMessage(ConversationMessageDTO conversationMessageDTO);

    void updateConversationName(UserEntity user, UpdateConversationNameDTO updateConversationNameDTO);}