package cn.wyq.serverwebsocket.service;

import cn.wyq.serverwebsocket.pojo.entity.Chat;
import org.springframework.ai.chat.messages.Message;
import reactor.core.publisher.Flux;

import java.util.List;

public interface ChatService {

    // 发送消息并获取流式响应
    Flux<String> chatStream(String message, String sessionId, String userName);

    // 获取某个会话的历史消息
    List<Message> getHistory(String sessionId);

    // 获取用户的会话列表
    List<Chat> getSessionList(String userId);

    // 创建新会话，返回新生成的 sessionId
    Chat createSession(String userId, String title);

    // 彻底删除会话（包括数据库记录和 AI 记忆）
    void deleteSession(String sessionId);
}
