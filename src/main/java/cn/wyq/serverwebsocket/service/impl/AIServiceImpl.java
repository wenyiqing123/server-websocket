package cn.wyq.serverwebsocket.service.impl;

import cn.wyq.serverwebsocket.framework.annotation.CurrentUser;
import cn.wyq.serverwebsocket.framework.constant.RedisKeyConstants;
import cn.wyq.serverwebsocket.mapper.AIMapper;
import cn.wyq.serverwebsocket.pojo.dto.ConversationMessageDTO;
import cn.wyq.serverwebsocket.pojo.dto.UpdateConversationNameDTO;
import cn.wyq.serverwebsocket.pojo.entity.Conversation;
import cn.wyq.serverwebsocket.pojo.entity.ConversationMessage;
import cn.wyq.serverwebsocket.pojo.entity.UserEntity;
import cn.wyq.serverwebsocket.service.AIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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
    @Cacheable(value = RedisKeyConstants.HISTORY_CONVERSATIONS_CACHE, key = "'userName:'+#userName", unless = "#result == null")
    public List<Conversation> list(String userName) {
        return aiMapper.findAllConversations(userName);
    }

    /**
     * 获取某个对话下的所有消息详情
     */
    @Override
    @Cacheable(value = RedisKeyConstants.CONVERSATION_MESSAGES_CACHE, key = "'conversationId:'+#conversationId", unless = "#result==null")
    public List<ConversationMessage> getMessages(Integer conversationId) {
        return aiMapper.selectMessagesByConversationId(conversationId);
    }

    /**
     * 创建一个新的对话
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = RedisKeyConstants.HISTORY_CONVERSATIONS_CACHE, key = "'userName:'+#userName")
    public Integer createConversation(String userName) {
        Conversation conversation = Conversation.builder()
                .name("新对话")
                .userName(userName)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
        // 使用 AiMapper 插入对话
        aiMapper.insertConversation(conversation);
        return conversation.getId();
    }


    /**
     * 发送消息到指定对话
     */
    @Override
    @CacheEvict(value = RedisKeyConstants.CONVERSATION_MESSAGES_CACHE, key = "'conversationId:'+#conversationMessageDTO.getConversationId()")
    public void saveConversationMessage(ConversationMessageDTO conversationMessageDTO) {
        if (conversationMessageDTO.getConversationId() == 0 || conversationMessageDTO.getContent() == null) {
            return;
        }
        aiMapper.insertMessage(conversationMessageDTO); // 使用 AiMapper 插入消息
    }

    @Override
    @CacheEvict(value = RedisKeyConstants.HISTORY_CONVERSATIONS_CACHE, key = "'userName:'+#user.userName")
    public void updateConversationName(@CurrentUser UserEntity user ,UpdateConversationNameDTO updateConversationNameDTO) {
        if (updateConversationNameDTO.getConversationId() == 0 || updateConversationNameDTO.getName() == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        aiMapper.updateConversationName(updateConversationNameDTO, now); // 使用 AiMapper 更新对话名称
    }
}