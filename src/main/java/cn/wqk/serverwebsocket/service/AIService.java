package cn.wqk.serverwebsocket.service;

import cn.wqk.serverwebsocket.pojo.dto.ChatRequestDto;
import cn.wqk.serverwebsocket.pojo.entity.Conversation;
import cn.wqk.serverwebsocket.pojo.entity.ConversationMessage;

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
    List<Conversation> list();

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
     * @param name 新对话的默认名称（如 "新对话"）
     * @return 新创建对话的 ID
     */
    Integer createConversation(String name);

    // --- 以下是为前后端分离存储设计的接口 ---

    /**
     * 保存【用户】发送的消息到数据库，并更新对话的更新时间。
     * 对应 Controller: @PostMapping("/send/user")
     *
     * @param request 包含 conversationId 和 content 的请求DTO
     */
    void saveUserMessage(ChatRequestDto request);

    /**
     * 保存消息
     *
     * @param conversationMessage
     */
    void saveConversationMessage(ConversationMessage conversationMessage);
}