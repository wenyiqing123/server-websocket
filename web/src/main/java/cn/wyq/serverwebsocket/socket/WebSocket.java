package cn.wyq.serverwebsocket.socket;

import cn.wyq.serverwebsocket.common.ApplicationHelper;
import cn.wyq.serverwebsocket.config.WebSocketConfig;
import cn.wyq.serverwebsocket.pojo.User;
import cn.wyq.serverwebsocket.pojo.socket.MessageFull;
import cn.wyq.serverwebsocket.pojo.socket.MessageInfo;
import cn.wyq.serverwebsocket.service.MessageService;
import cn.wyq.serverwebsocket.service.UserService;
import cn.wyq.serverwebsocket.util.MessageUtils;
import cn.wyq.serverwebsocket.utils.RedisUtil;
import com.alibaba.fastjson.JSON;
import jakarta.servlet.http.HttpSession;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * WebSocket 服务端逻辑
 * 💡 注意：WebSocket 对象是多例的，由容器管理，Service 需通过 ApplicationHelper 动态获取
 */
@Component
@Slf4j
@ServerEndpoint(value = "/websocket/{username}", configurator = WebSocketConfig.class)
public class WebSocket {

    // 💡 将 Service 定义为静态变量，供所有 WebSocket 实例共享
    private static MessageService messageService;
    private static RedisUtil redisUtil;
    private static UserService userService;

    // 在线用户 Session 映射：username -> Session
    private static final Map<String, Session> onlineUsers = new ConcurrentHashMap<>();
    // 当前在线人数
    private static AtomicInteger onlineClientNumber = new AtomicInteger(0);
    // 连接 ID 与 Session 的映射（如果需要根据 session id 查找）
    private static Map<String, Session> onlineClientMap = new ConcurrentHashMap<>();

    private HttpSession httpSession;

    /**
     * 💡 核心修复：延迟获取 Spring 管理的 Bean
     * 确保在 ApplicationContext 初始化完成后再进行调用
     */
    private void initServices() {
        if (userService == null) {
            userService = ApplicationHelper.getBean(UserService.class);
        }
        if (messageService == null) {
            messageService = ApplicationHelper.getBean(MessageService.class);
        }
        if (redisUtil == null) {
            redisUtil = ApplicationHelper.getBean(RedisUtil.class);
        }
    }

    @OnOpen
    public void onOpen(Session session, @PathParam("username") String username, EndpointConfig config) {
        // 1. 初始化 Service
        initServices();

        // 2. 获取握手时传入的协议信息（如 Token）
        List<String> protocols = session.getRequestParameterMap().get("Sec-WebSocket-Protocol");
        if (protocols != null && !protocols.isEmpty()) {
            String token = protocols.get(0);
            log.info("用户[{}]连接，携带 Token: {}", username, token);
        }

        // 3. 保存 Session
        this.httpSession = (HttpSession) config.getUserProperties().get(HttpSession.class.getName());
        onlineUsers.put(username, session);
        onlineClientMap.put(session.getId(), session);
        onlineClientNumber.incrementAndGet();

        // 4. 广播上线通知
        String message = MessageUtils.getMessage(true, null, getFriends());
        broadcastAllUsers(message);

        log.info("时间[{}]：用户[{}]连接成功，SessionID:[{}]，在线总数:[{}]",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                username, session.getId(), onlineClientNumber.get());
    }

    @OnClose
    public void onClose(Session session, @PathParam("username") String username) {
        onlineUsers.remove(username);
        onlineClientMap.remove(session.getId());
        onlineClientNumber.decrementAndGet();

        // 广播下线通知
        String message = MessageUtils.getMessage(true, null, getFriends());
        broadcastAllUsers(message);

        log.info("时间[{}]：用户[{}]连接关闭，在线总数:[{}]",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                username, onlineClientNumber.get());
    }

    @OnError
    public void onError(Throwable error, Session session, @PathParam("username") String username) {
        log.error("用户[{}]连接异常", username, error);
    }

    @OnMessage
    public void onMsg(Session session, String message, @PathParam("username") String username) throws IOException {
        // 确保 Service 已就绪
        initServices();

        log.info("收到来自[{}]的消息: {}", username, message);

        // 1. 保存消息到数据库
        MessageInfo messageInfo = JSON.parseObject(message, MessageInfo.class);
        Integer id = messageService.addMessage(messageInfo);

        // 2. 构建完整消息对象并回传/广播
        MessageFull messageFull = JSON.parseObject(message, MessageFull.class);
        messageFull.setId(id);
        String jsonResponse = JSON.toJSONString(messageFull);

        // 3. 广播给所有人
        sendAllMessage(jsonResponse);
    }

    private void sendAllMessage(String message) {
        onlineClientMap.values().forEach(session -> {
            if (session.isOpen()) {
                session.getAsyncRemote().sendText(message);
            }
        });
    }

    /**
     * 获取当前所有在线好友（用户对象集合）
     */
    public Set<User> getFriends() {
        initServices(); // 确保 userService 可用
        Set<String> userNames = onlineUsers.keySet();
        Set<User> userList = new HashSet<>();

        for (String name : userNames) {
            String path = userService.getPath(name);
            userList.add(new User().builder().userName(name).path(path).build());
        }
        return userList;
    }

    /**
     * 广播给所有用户
     */
    private void broadcastAllUsers(String message) {
        onlineUsers.forEach((name, session) -> {
            try {
                if (session.isOpen()) {
                    session.getAsyncRemote().sendText(message);
                }
            } catch (Exception e) {
                log.error("给用户[{}]发送广播失败", name, e);
            }
        });
    }
}