package cn.wyq.serverwebsocket.service;

import cn.wyq.serverwebsocket.pojo.dto.AiReplyDto;
import cn.wyq.serverwebsocket.pojo.entity.Chat;
import org.springframework.ai.chat.messages.Message;
import reactor.core.publisher.Flux;

import java.util.List;

public interface ChatService {
    Flux<String> chatStream(String message, String sessionId, String userName, String agentType, List<String> docIds);

    List<Message> getHistory(String sessionId, String agentType);
    List<Chat> getSessionList(String userId, String agentType);
    Chat createSession(String userId, String title, String agentType);
    void deleteSession(String sessionId);
    String generateSuggestReply(AiReplyDto dto, String currentUserName);
}
