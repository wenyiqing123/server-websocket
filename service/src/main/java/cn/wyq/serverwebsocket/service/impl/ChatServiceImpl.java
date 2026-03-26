package cn.wyq.serverwebsocket.service.impl;

import cn.wyq.serverwebsocket.exception.ServiceException;
import cn.wyq.serverwebsocket.mapper.ChatMapper;
import cn.wyq.serverwebsocket.pojo.entity.Chat;
import cn.wyq.serverwebsocket.service.ChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class ChatServiceImpl implements ChatService {

    private final ChatClient girlfriendClient;
    private final ChatClient serviceClient; // 💡 改名为 serviceClient
    private final ChatMemory chatMemory;
    private final ChatMapper chatMapper;

    public ChatServiceImpl(
            @Qualifier("girlfriendChatClient") ChatClient girlfriendClient,
            @Qualifier("serviceChatClient") ChatClient serviceClient, // 💡 注入 serviceChatClient
            ChatMemory chatMemory,
            ChatMapper chatMapper) {
        this.girlfriendClient = girlfriendClient;
        this.serviceClient = serviceClient;
        this.chatMemory = chatMemory;
        this.chatMapper = chatMapper;
    }

    /**
     * 生成隔离的专属记忆 ID
     */
    private String getIsolatedMemoryId(String agentType, String sessionId) {
        String safeAgentType = StringUtils.hasText(agentType) ? agentType : "girlfriend";
        // 现在的格式会是 "service:session_xxx" 或 "girlfriend:session_xxx"
        return safeAgentType + ":" + sessionId;
    }

    @Override
    public Flux<String> chatStream(String message, String sessionId, String userName, String agentType) {
        try {
            // 💡 动态路由判定：如果是 service，就用客服大脑；否则兜底用女友大脑
            ChatClient activeClient = "service".equalsIgnoreCase(agentType) ? serviceClient : girlfriendClient;

            String memoryId = getIsolatedMemoryId(agentType, sessionId);

            return activeClient.prompt()
                    .user(message)
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, memoryId))
                    .stream()
                    .content();
        } catch (Exception e) {
            log.error("调用大模型发生异常", e);
            throw new ServiceException("对不起，大模型脑子暂时短路了", 400);
        }
    }

    @Override
    public List<Message> getHistory(String sessionId, String agentType) {
        String memoryId = getIsolatedMemoryId(agentType, sessionId);
        return chatMemory.get(memoryId);
    }

    @Override
    public List<Chat> getSessionList(String userId, String agentType) {
        // 先查出该用户所有的会话
        List<Chat> allSessions = chatMapper.selectByUserId(userId);

        // 💡 核心魔法：在 Java 层利用 Stream 过滤！
        return allSessions.stream()
                .filter(chat -> {
                    // 客服就严格只查 service_ 开头的
                    return chat.getId().startsWith(agentType + "_");
                })
                .toList();
    }

    @Override
    public Chat createSession(String userId, String title, String agentType) {
        // 💡 核心魔法：把身份标识拼装到原生的 sessionId 里！
        String prefix = StringUtils.hasText(agentType) ? agentType : "girlfriend";
        String sessionId = prefix + "_session_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);

        chatMapper.insertSession(sessionId, userId, title);
        return new Chat(sessionId, userId, title, LocalDate.now());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSession(String sessionId) {
        log.info("执行删除会话操作: {}", sessionId);
        chatMapper.deleteById(sessionId);

        // 💡 同步清理所有相关智能体的记忆
        chatMemory.clear(getIsolatedMemoryId("girlfriend", sessionId));
        chatMemory.clear(getIsolatedMemoryId("service", sessionId));
    }
}