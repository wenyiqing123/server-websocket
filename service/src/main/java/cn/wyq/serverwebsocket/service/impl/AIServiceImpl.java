package cn.wyq.serverwebsocket.service.impl;

import cn.wyq.serverwebsocket.annotation.CurrentUser;
import cn.wyq.serverwebsocket.constant.RedisKeyConstants;
import cn.wyq.serverwebsocket.mapper.AIMapper;
import cn.wyq.serverwebsocket.pojo.dto.ConversationMessageDTO;
import cn.wyq.serverwebsocket.pojo.dto.UpdateConversationNameDTO;
import cn.wyq.serverwebsocket.pojo.entity.Conversation;
import cn.wyq.serverwebsocket.pojo.entity.ConversationMessage;
import cn.wyq.serverwebsocket.pojo.entity.UserEntity;
import cn.wyq.serverwebsocket.service.AIService;
import cn.wyq.serverwebsocket.service.RealTimeRecommendService;
import cn.wyq.serverwebsocket.utils.BaseContext;
import cn.wyq.serverwebsocket.utils.QwenTagExtractor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

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

    // 💡 注入 RedisTemplate 进行手动缓存控制
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;


    /**
     * 获取历史对话列表 (防缓存穿透版)
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<Conversation> list(String userName) {
        String cacheKey = RedisKeyConstants.HISTORY_CONVERSATIONS_CACHE + ":userName:" + userName;

        // 1. 查缓存
        Object cachedData = redisTemplate.opsForValue().get(cacheKey);
        if (cachedData != null) {
            log.debug("命中历史对话缓存，userName: {}", userName);
            // 💡 如果是空集合，也会直接返回，成功挡住针对不存在用户的穿透攻击！
            return (List<Conversation>) cachedData;
        }

        // 2. 缓存未命中，查数据库
        List<Conversation> list = aiMapper.findAllConversations(userName);

        // 3. 🛡️ 核心防御：缓存空值策略
        if (list == null || list.isEmpty()) {
            // 如果数据库也没有（可能是新用户，或恶意攻击的假用户），存入空集合，并设置极短的 1 分钟过期时间
            redisTemplate.opsForValue().set(cacheKey, new ArrayList<>(), 1, TimeUnit.MINUTES);
            log.info("用户 {} 无对话，已存入空缓存防穿透", userName);
        } else {
            // 如果是正常数据，存入缓存，设置较长的 2 小时过期时间
            redisTemplate.opsForValue().set(cacheKey, list, 2, TimeUnit.HOURS);
        }

        return list != null ? list : new ArrayList<>();
    }

    /**
     * 获取某个对话下的所有消息详情 (防缓存穿透版)
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<ConversationMessage> getMessages(Integer conversationId) {
        String cacheKey = RedisKeyConstants.CONVERSATION_MESSAGES_CACHE + ":conversationId:" + conversationId;

        // 1. 查缓存
        Object cachedData = redisTemplate.opsForValue().get(cacheKey);
        if (cachedData != null) {
            return (List<ConversationMessage>) cachedData;
        }

        // 2. 查数据库
        List<ConversationMessage> messages = aiMapper.selectMessagesByConversationId(conversationId);

        // 3. 🛡️ 核心防御：缓存空值策略
        if (messages == null || messages.isEmpty()) {
            // 如果黑客拿一个不存在的 conversationId 来请求，这里会缓存一个空集合（存活 1 分钟），挡住后续攻击
            redisTemplate.opsForValue().set(cacheKey, new ArrayList<>(), 1, TimeUnit.MINUTES);
        } else {
            // 正常数据缓存 2 小时
            redisTemplate.opsForValue().set(cacheKey, messages, 2, TimeUnit.HOURS);
        }

        return messages != null ? messages : new ArrayList<>();
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
        String cacheKey = RedisKeyConstants.HISTORY_CONVERSATIONS_CACHE + ":userName:" + userName;
        redisTemplate.delete(cacheKey);

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
        String cacheKey = RedisKeyConstants.CONVERSATION_MESSAGES_CACHE + ":conversationId:" + conversationMessageDTO.getConversationId();
        redisTemplate.delete(cacheKey);

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
        String cacheKey = RedisKeyConstants.HISTORY_CONVERSATIONS_CACHE + ":userName:" + user.getUserName();
        redisTemplate.delete(cacheKey);
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
        String historyCacheKey = RedisKeyConstants.HISTORY_CONVERSATIONS_CACHE + ":userName:" + user.getUserName();
        String messagesCacheKey = RedisKeyConstants.CONVERSATION_MESSAGES_CACHE + ":conversationId:" + id;

        // RedisTemplate 支持批量删除，提升性能
        redisTemplate.delete(Arrays.asList(historyCacheKey, messagesCacheKey));
    }
}