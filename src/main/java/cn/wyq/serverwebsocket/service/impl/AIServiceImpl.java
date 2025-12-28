package cn.wyq.serverwebsocket.service.impl;

import cn.wyq.serverwebsocket.mapper.AIMapper;
import cn.wyq.serverwebsocket.pojo.dto.ChatRequestDto;
import cn.wyq.serverwebsocket.pojo.dto.ConversationMessageDTO;
import cn.wyq.serverwebsocket.pojo.dto.UpdateConversationNameDTO;
import cn.wyq.serverwebsocket.pojo.entity.Conversation;
import cn.wyq.serverwebsocket.pojo.entity.ConversationMessage;
import cn.wyq.serverwebsocket.service.AIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI 服务实现类：专注于对话和消息的数据存储与查询
 * 使用 AiMapper 统一处理所有数据库操作。
 */
@Service
public class AIServiceImpl implements AIService {

    @Autowired
    private AIMapper aiMapper; // 注入整合后的 Mapper

    /**
     * 获取历史对话列表
     */
    @Override
    public List<Conversation> list() {
        return aiMapper.findAllConversations();
    }

    /**
     * 获取某个对话下的所有消息详情
     */
    @Override
    public List<ConversationMessage> getMessages(Integer conversationId) {
        return aiMapper.selectMessagesByConversationId(conversationId);
    }

    /**
     * 创建一个新的对话
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer createConversation(String username) {
        Conversation conversation = Conversation.builder()
                .name("新对话")
                .userName(username)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
        // 使用 AiMapper 插入对话
        aiMapper.insertConversation(conversation);
        return conversation.getId();
    }

    /**
     * 保存【用户】发送的消息到数据库，并更新对话的更新时间
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveUserMessage(ChatRequestDto request) {
        if (request.getConversationId() == null || request.getContent() == null) {
            return;
        }
        // 1. 存储用户消息 (Role = 1)
        ConversationMessage userMsg = new ConversationMessage();
        userMsg.setConversationId(request.getConversationId());
        userMsg.setContent(request.getContent());
        userMsg.setRole(1); // 1 代表用户
        // 2. 更新对话的 update_time
        Conversation conversation = Conversation.builder()
                .id(request.getConversationId())
                .updateTime(LocalDateTime.now())
                .build();
        aiMapper.updateConversationUpdateTime(conversation); // 使用 AiMapper 更新对话
    }

    /**
     * 保存【AI】返回的回复消息到数据库
     */
    @Override
    public void saveConversationMessage(ConversationMessageDTO conversationMessageDTO) {
        if (conversationMessageDTO.getConversationId() == 0 || conversationMessageDTO.getContent() == null) {
            return;
        }
        // 强制设置为 AI 角色 (Role = 2)
//        conversationMessage.setRole(2);
        aiMapper.insertMessage(conversationMessageDTO); // 使用 AiMapper 插入消息
    }

    @Override
    public void updateConversationName(UpdateConversationNameDTO updateConversationNameDTO) {
        if (updateConversationNameDTO.getConversationId() == 0 || updateConversationNameDTO.getName() == null) {
            return;
        }

        aiMapper.updateConversationName(updateConversationNameDTO); // 使用 AiMapper 更新对话名称
    }
}