package cn.wyq.serverwebsocket.service.impl;

import cn.wyq.serverwebsocket.exception.ServiceException;
import cn.wyq.serverwebsocket.mapper.ChatMapper;
import cn.wyq.serverwebsocket.pojo.entity.Chat;
import cn.wyq.serverwebsocket.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {
    private final ChatClient chatClient;
    private final ChatMemory chatMemory;
    private final ChatMapper chatMapper; // 注入 MyBatis Mapper

    @Override
    public Flux<String> chatStream(String message, String sessionId, String userName) {
        try {
            return chatClient.prompt()
                    .user(message)
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, sessionId))
                    .stream()
                    .content();
        } catch (Exception e) {
            log.error("调用大模型发生异常", e);
            throw new ServiceException("对不起，大模型脑子暂时短路了", 400);
        }
    }

    @Override
    public List<Message> getHistory(String sessionId) {
        return chatMemory.get(sessionId);
    }

    @Override
    public List<Chat> getSessionList(String userId) {
        return chatMapper.selectByUserId(userId);
    }

    @Override
    public Chat createSession(String userId, String title) {
        // 生成唯一 ID
        String sessionId = "session_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        chatMapper.insertSession(sessionId, userId, title);
        return new Chat(sessionId, userId, title, LocalDate.now());
    }

    @Override
    public void deleteSession(String sessionId) {
        log.info("执行删除会话操作: {}", sessionId);
        // 1. 删除 MySQL 中的会话记录
        chatMapper.deleteById(sessionId);

        // 2. 清空 Spring AI 中的历史记忆
        chatMemory.clear(sessionId);
    }
}
