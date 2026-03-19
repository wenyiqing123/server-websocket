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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
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
    @Autowired
    private QwenTagExtractor qwenTagExtractor;

    @Autowired
    private RealTimeRecommendService recommendService;



    /**
     * 获取历史对话列表
     */
    @Override
    //删除unless = "#result == null"，防止缓存穿透
    @Cacheable(value = RedisKeyConstants.HISTORY_CONVERSATIONS_CACHE, key = "'userName:'+#userName")
    public List<Conversation> list(String userName) {
        return aiMapper.findAllConversations(userName);
    }

    /**
     * 获取某个对话下的所有消息详情
     */
    @Override
    //删除unless = "#result == null"，防止缓存穿透
    @Cacheable(value = RedisKeyConstants.CONVERSATION_MESSAGES_CACHE, key = "'conversationId:'+#conversationId")
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
    @CacheEvict(value = RedisKeyConstants.CONVERSATION_MESSAGES_CACHE, key = "'conversationId:' + #conversationMessageDTO.getConversationId()")
    public void saveConversationMessage(ConversationMessageDTO conversationMessageDTO) {
        if (conversationMessageDTO.getConversationId() == 0 || conversationMessageDTO.getContent() == null) {
            return;
        }
        // 1. 原有逻辑：使用 AiMapper 插入消息到数据库
        aiMapper.insertMessage(conversationMessageDTO);
        // 2. 🌟 核心新增逻辑：触发异步推荐闭环
        // 注意：这里需要判断一下这条消息是不是“用户”发的。
        // 假设你的 DTO 里 role 为 1 代表用户 (根据你的实际字段调整)
        if (conversationMessageDTO.getRole() == 1) {
            // 获取用户ID (假设你的 DTO 里有，或者从上下文中获取)
            Long currentId = BaseContext.getCurrentId();
            String userPrompt = conversationMessageDTO.getContent();
            // 把繁重的打标和矩阵计算彻底扔给后台线程池，主线程瞬间返回，绝对不卡顿！

            recommendService.processRecommendationPipelineAsync(Math.toIntExact(currentId), userPrompt);
        }
    }

    @Override
    @CacheEvict(value = RedisKeyConstants.HISTORY_CONVERSATIONS_CACHE, key = "'userName:'+#user.userName")
    public void updateConversationName(@CurrentUser UserEntity user , UpdateConversationNameDTO updateConversationNameDTO) {
        if (updateConversationNameDTO.getConversationId() == 0 || updateConversationNameDTO.getName() == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        aiMapper.updateConversationName(updateConversationNameDTO, now); // 使用 AiMapper 更新对话名称
    }

    @Override
    @Transactional(rollbackFor = Exception.class) // 💡 核心加固：保证两条删除语句的原子性，任何异常立刻回滚
    @Caching(evict = {
            // 清理用户的会话列表缓存
            @CacheEvict(value = RedisKeyConstants.HISTORY_CONVERSATIONS_CACHE, key = "'userName:' + #user.userName"),
            // 清理该会话下的具体消息缓存
            @CacheEvict(value = RedisKeyConstants.CONVERSATION_MESSAGES_CACHE, key = "'conversationId:' + #id")
    })
    public void deleteConversation(@CurrentUser UserEntity user, int id) {
        // 1. 删除主会话记录
        aiMapper.deleteConversationById(id);
        // 2. 删除该会话关联的所有聊天记录
        aiMapper.deleteMessagesByConversationId(id);
    }
}