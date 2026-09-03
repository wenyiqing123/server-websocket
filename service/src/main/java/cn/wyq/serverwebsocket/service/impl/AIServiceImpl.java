package cn.wyq.serverwebsocket.service.impl;

import cn.wyq.serverwebsocket.annotation.CurrentUser;
import cn.wyq.serverwebsocket.mapper.AIMapper;
import cn.wyq.serverwebsocket.pojo.dto.ConversationMessageDTO;
import cn.wyq.serverwebsocket.pojo.dto.UpdateConversationNameDTO;
import cn.wyq.serverwebsocket.pojo.entity.Conversation;
import cn.wyq.serverwebsocket.pojo.entity.ConversationMessage;
import cn.wyq.serverwebsocket.pojo.entity.UserEntity;
import cn.wyq.serverwebsocket.service.AIService;
import cn.wyq.serverwebsocket.service.RealTimeRecommendService;
import cn.wyq.serverwebsocket.utils.BaseContext;
import cn.wyq.serverwebsocket.utils.CacheClient;
import cn.wyq.serverwebsocket.utils.QwenTagExtractor;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * AI 服务实现类：专注于对话和消息的数据存储与查询 (手动缓存高并发防御版)
 */
@Slf4j
@Service
public class AIServiceImpl implements AIService {

    @Autowired
    private AIMapper aiMapper;

    @Autowired
    private QwenTagExtractor qwenTagExtractor;

    @Autowired
    private RealTimeRecommendService recommendService;

    @Autowired
    private CacheClient cacheClient;


    /**
     * 获取历史对话列表 (防缓存穿透版)
     */
    @Override
    public List<Conversation> list(String userName) {
        return cacheClient.queryWithMutex(
                "ai:conversations:user", userName,
                new TypeReference<List<Conversation>>() {},
                id -> {
                    List<Conversation> conversations = aiMapper.findAllConversations(id);
                    return conversations == null ? new ArrayList<>() : conversations;
                }, 2, java.util.concurrent.TimeUnit.HOURS);
    }

    /**
     * 获取某个对话下的所有消息详情 (防缓存穿透版)
     */
    @Override
    public List<ConversationMessage> getMessages(Integer conversationId) {
        return cacheClient.queryWithMutex(
                "ai:conversation-messages", conversationId,
                new TypeReference<List<ConversationMessage>>() {},
                id -> {
                    List<ConversationMessage> messages = aiMapper.selectMessagesByConversationId(id);
                    return messages == null ? new ArrayList<>() : messages;
                }, 2, java.util.concurrent.TimeUnit.HOURS);
    }

    /**
     * 创建一个新的对话 (手动清除缓存)
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer createConversation(String userName) {
        Conversation conversation = Conversation.builder()
                .name("新对话")
                .userName(userName)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();

        aiMapper.insertConversation(conversation);

        // 🧹 发生写操作，手动清除该用户的对话列表缓存，保证下次查询是最新的
        cacheClient.evict("ai:conversations:user", userName);

        return conversation.getId();
    }

    /**
     * 发送消息到指定对话 (手动清除缓存)
     */
    @Override
    public void saveConversationMessage(ConversationMessageDTO conversationMessageDTO) {
        if (conversationMessageDTO.getConversationId() == 0 || conversationMessageDTO.getContent() == null) {
            return;
        }

        // 1. 插入数据库
        aiMapper.insertMessage(conversationMessageDTO);

        // 2. 🧹 手动清除该对话的消息列表缓存
        cacheClient.evict("ai:conversation-messages", conversationMessageDTO.getConversationId());

        // 3. 触发异步推荐闭环
        if (conversationMessageDTO.getRole() == 1) {
            Long currentId = BaseContext.getCurrentId();
            String userPrompt = conversationMessageDTO.getContent();
            recommendService.processRecommendationPipelineAsync(Math.toIntExact(currentId), userPrompt);
        }
    }

    /**
     * 修改对话名称 (手动清除缓存)
     */
    @Override
    public void updateConversationName(@CurrentUser UserEntity user, UpdateConversationNameDTO updateConversationNameDTO) {
        if (updateConversationNameDTO.getConversationId() == 0 || updateConversationNameDTO.getName() == null) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        aiMapper.updateConversationName(updateConversationNameDTO, now);

        // 🧹 手动清除该用户的对话列表缓存
        cacheClient.evict("ai:conversations:user", user.getUserName());
    }

    /**
     * 删除对话及消息 (手动双删缓存)
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteConversation(@CurrentUser UserEntity user, int id) {
        // 1. 删除数据库记录
        aiMapper.deleteConversationById(id);
        aiMapper.deleteMessagesByConversationId(id);

        // 2. 🧹 手动批量删除相关缓存 (相当于以前的 @Caching(evict = {...}))
        cacheClient.evict("ai:conversations:user", user.getUserName());
        cacheClient.evict("ai:conversation-messages", id);
    }
}
