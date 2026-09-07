package cn.wyq.serverwebsocket.service.impl;

import cn.wyq.serverwebsocket.exception.ServiceException;
import cn.wyq.serverwebsocket.mapper.AIMapper;
import cn.wyq.serverwebsocket.pojo.dto.ConversationMessageDTO;
import cn.wyq.serverwebsocket.pojo.dto.UpdateConversationNameDTO;
import cn.wyq.serverwebsocket.pojo.entity.Conversation;
import cn.wyq.serverwebsocket.pojo.entity.ConversationMessage;
import cn.wyq.serverwebsocket.pojo.entity.UserEntity;
import cn.wyq.serverwebsocket.service.AIService;
import cn.wyq.serverwebsocket.service.RealTimeRecommendService;
import cn.wyq.serverwebsocket.service.SysConfigService;
import cn.wyq.serverwebsocket.utils.BaseContext;
import cn.wyq.serverwebsocket.utils.CacheClient;
import cn.wyq.serverwebsocket.utils.QwenTagExtractor;
import cn.wyq.serverwebsocket.utils.SystemConfigUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * AI 服务实现类：专注于对话和消息的数据存储与查询 (手动缓存高并发防御版)
 */
/**
 * AIServiceImpl：业务服务实现类，封装对应领域的业务规则。
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

    @Autowired
    private SysConfigService sysConfigService;


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
    public List<ConversationMessage> getMessages(Integer conversationId, String userName) {
        assertConversationOwned(conversationId, userName);
        String cacheId = userName + "|" + conversationId;
        return cacheClient.queryWithMutex(
                "ai:conversation-messages", cacheId,
                new TypeReference<List<ConversationMessage>>() {},
                ignored -> {
                    List<ConversationMessage> messages = aiMapper.selectMessagesByConversationIdAndUserName(
                            conversationId, userName);
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
    public void saveConversationMessage(ConversationMessageDTO conversationMessageDTO, String userName) {
        if (conversationMessageDTO == null || conversationMessageDTO.getConversationId() <= 0
                || !StringUtils.hasText(conversationMessageDTO.getContent())) {
            throw new ServiceException("消息参数不完整", 400);
        }
        if (conversationMessageDTO.getRole() != 1 && conversationMessageDTO.getRole() != 2) {
            throw new ServiceException("消息角色不正确", 400);
        }
        int maxMessageLength;
        try {
            maxMessageLength = SystemConfigUtils.intValue(sysConfigService.getConfigMap(),
                    "chat_max_message_length", 10000, 1, 1_000_000);
        } catch (RuntimeException e) {
            maxMessageLength = 10000;
        }
        if (conversationMessageDTO.getContent().length() > maxMessageLength) {
            throw new ServiceException("单条消息不能超过 " + maxMessageLength + " 个字符", 400);
        }
        assertConversationOwned(conversationMessageDTO.getConversationId(), userName);

        // 1. 插入数据库
        aiMapper.insertMessage(conversationMessageDTO);

        // 2. 🧹 手动清除该对话的消息列表缓存
        cacheClient.evict("ai:conversation-messages", userName + "|" + conversationMessageDTO.getConversationId());

        // 3. 触发异步推荐闭环
        if (conversationMessageDTO.getRole() == 1 && BaseContext.getCurrentId() != null) {
            Long currentId = BaseContext.getCurrentId();
            String userPrompt = conversationMessageDTO.getContent();
            recommendService.processRecommendationPipelineAsync(Math.toIntExact(currentId), userPrompt);
        }
    }

    /**
     * 修改对话名称 (手动清除缓存)
     */
    @Override
    public void updateConversationName(UserEntity user, UpdateConversationNameDTO updateConversationNameDTO) {
        if (user == null || !StringUtils.hasText(user.getUserName()) || updateConversationNameDTO == null
                || updateConversationNameDTO.getConversationId() <= 0 || !StringUtils.hasText(updateConversationNameDTO.getName())) {
            throw new ServiceException("会话名称参数不完整", 400);
        }

        assertConversationOwned(updateConversationNameDTO.getConversationId(), user.getUserName());
        LocalDateTime now = LocalDateTime.now();
        if (aiMapper.updateConversationName(updateConversationNameDTO, now, user.getUserName()) == 0) {
            throw new ServiceException("会话不存在或无权操作", 404);
        }

        // 🧹 手动清除该用户的对话列表缓存
        cacheClient.evict("ai:conversations:user", user.getUserName());
    }

    /**
     * 删除对话及消息 (手动双删缓存)
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteConversation(UserEntity user, int id) {
        if (user == null || !StringUtils.hasText(user.getUserName())) {
            throw new ServiceException("请先登录", 401);
        }
        assertConversationOwned(id, user.getUserName());

        // 1. 先删除子表消息，再删除会话，兼容存在外键约束的数据库。
        aiMapper.deleteMessagesByConversationId(id);
        if (aiMapper.deleteConversationByIdAndUserName(id, user.getUserName()) == 0) {
            throw new ServiceException("会话不存在或无权操作", 404);
        }

        // 2. 🧹 手动批量删除相关缓存 (相当于以前的 @Caching(evict = {...}))
        cacheClient.evict("ai:conversations:user", user.getUserName());
        cacheClient.evict("ai:conversation-messages", user.getUserName() + "|" + id);
    }

    /** 旧版 AI 会话也必须在所有数据库操作前验证所属用户。 */
    private void assertConversationOwned(Integer conversationId, String userName) {
        if (conversationId == null || conversationId <= 0 || !StringUtils.hasText(userName)) {
            throw new ServiceException("会话参数不完整", 400);
        }
        if (aiMapper.countConversationByIdAndUserName(conversationId, userName) == 0) {
            throw new ServiceException("会话不存在或无权操作", 404);
        }
    }
}
