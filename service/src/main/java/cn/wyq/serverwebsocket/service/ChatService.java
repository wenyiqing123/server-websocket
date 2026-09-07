package cn.wyq.serverwebsocket.service;

import cn.wyq.serverwebsocket.pojo.dto.AiReplyDto;
import cn.wyq.serverwebsocket.pojo.entity.Chat;
import org.springframework.ai.chat.messages.Message;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * ChatService：业务服务接口，定义对应领域的业务操作契约。
 */
public interface ChatService {
    Flux<String> chatStream(String message, String sessionId, String userName, String agentType, List<String> docIds);

    /** 自定义智能体流式聊天。agentId 为空时保持旧模式。 */
    Flux<String> chatStream(String message, String sessionId, String userName, String agentType,
                            Long agentId, Integer userId, List<String> docIds);

    List<Message> getHistory(String sessionId, String agentType);
    List<Message> getHistory(String sessionId, String agentType, Long agentId);
    List<Chat> getSessionList(Integer ownerUserId, String agentType);
    List<Chat> getSessionList(Integer ownerUserId, String agentType, Long agentId);
    Chat getSession(String sessionId);
    Chat createSession(String userName, Integer ownerUserId, String title, String agentType);
    Chat createSession(String userName, Integer ownerUserId, String title, String agentType,
                       Long agentId, Integer promptVersion);
    void deleteSession(String sessionId);
    void deleteSession(String sessionId, String agentType, Long agentId);
    /** 删除用户全部 AI 会话及对应聊天记忆。 */
    void deleteSessionsForOwner(Integer ownerUserId);
    String generateSuggestReply(AiReplyDto dto, String currentUserName);
}
