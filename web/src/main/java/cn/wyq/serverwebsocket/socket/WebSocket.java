package cn.wyq.serverwebsocket.socket;

import cn.wyq.serverwebsocket.common.ApplicationHelper;
import cn.wyq.serverwebsocket.config.WebSocketConfig;
import cn.wyq.serverwebsocket.pojo.User;
import cn.wyq.serverwebsocket.pojo.socket.MessageFull;
import cn.wyq.serverwebsocket.pojo.socket.MessageInfo;
import cn.wyq.serverwebsocket.service.GroupService;
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
 */
@Component
@Slf4j
@ServerEndpoint(value = "/websocket/{username}", configurator = WebSocketConfig.class)
public class WebSocket {

    // 💡 静态变量，供所有 WebSocket 实例共享
    private static MessageService messageService;
    private static RedisUtil redisUtil;
    private static UserService userService;
    private static GroupService groupService; // 🌟 新增 GroupService 用于群聊路由

    // 在线用户 Session 映射：username -> Session
    private static final Map<String, Session> onlineUsers = new ConcurrentHashMap<>();
    // 当前在线人数
    private static AtomicInteger onlineClientNumber = new AtomicInteger(0);
    // 连接 ID 与 Session 的映射
    private static Map<String, Session> onlineClientMap = new ConcurrentHashMap<>();

    private HttpSession httpSession;

    /**
     * 初始化 Spring 管理的 Bean
     */
    private void initServices() {
        if (userService == null) userService = ApplicationHelper.getBean(UserService.class);
        if (messageService == null) messageService = ApplicationHelper.getBean(MessageService.class);
        if (redisUtil == null) redisUtil = ApplicationHelper.getBean(RedisUtil.class);
        if (groupService == null) groupService = ApplicationHelper.getBean(GroupService.class); // 🌟 新增
    }

    @OnOpen
    public void onOpen(Session session, @PathParam("username") String username, EndpointConfig config) {
        initServices();
        List<String> protocols = session.getRequestParameterMap().get("Sec-WebSocket-Protocol");
        if (protocols != null && !protocols.isEmpty()) {
            String token = protocols.get(0);
            log.info("用户[{}]连接，携带 Token: {}", username, token);
        }

        this.httpSession = (HttpSession) config.getUserProperties().get(HttpSession.class.getName());
        onlineUsers.put(username, session);
        onlineClientMap.put(session.getId(), session);
        onlineClientNumber.incrementAndGet();

        String message = MessageUtils.getMessage(true, null, getFriends());
        broadcastAllUsers(message);

        log.info("时间[{}]：用户[{}]连接成功，在线总数:[{}]",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                username, onlineClientNumber.get());
    }

    @OnClose
    public void onClose(Session session, @PathParam("username") String username) {
        onlineUsers.remove(username);
        onlineClientMap.remove(session.getId());
        onlineClientNumber.decrementAndGet();

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
    // =======================================================
    // 🌟 新增：供外部(Service)调用的静态路由分发方法
    // =======================================================
    public static void sendToTarget(String toName, String jsonMessage, String senderName) {
        if (toName == null) return;

        if (toName.startsWith("GROUP_")) {
            // 【群聊撤回分发】
            if (groupService == null) groupService = ApplicationHelper.getBean(GroupService.class);
            List<String> groupMembers = groupService.getGroupMemberNames(toName);
            for (String memberName : groupMembers) {
                Session targetSession = onlineUsers.get(memberName);
                // 广播给群成员，但不发给撤回者本人（因为前端本人页面已经处理过了）
                if (targetSession != null && targetSession.isOpen() && !memberName.equals(senderName)) {
                    targetSession.getAsyncRemote().sendText(jsonMessage);
                }
            }
        } else {
            // 【私聊撤回分发】
            Session targetSession = onlineUsers.get(toName);
            if (targetSession != null && targetSession.isOpen()) {
                targetSession.getAsyncRemote().sendText(jsonMessage);
            }
        }
    }
    @OnMessage
    public void onMsg(Session session, String message, @PathParam("username") String username) throws IOException {
        initServices();
        log.info("收到来自[{}]的消息: {}", username, message);

        // 1. 解析前端发来的消息
        MessageInfo messageInfo = JSON.parseObject(message, MessageInfo.class);

        // 2. 保存到数据库
        messageService.addMessage(messageInfo);

        // 🌟 核心修改 1：从对象中提取 MyBatis 自动注入的真实 ID，而不是接收方法的返回值
        Integer realId = messageInfo.getId();
        log.info("消息落库成功，真实生成的 ID = {}", realId);

        // 3. 构建包含真实 ID 的完整消息对象
        MessageFull messageFull = JSON.parseObject(message, MessageFull.class);
        messageFull.setId(realId);
        String jsonResponse = JSON.toJSONString(messageFull);

        // =================== 🌟 核心修改 2：消息必须抄送给自己 ===================
        // 只有把带 ID 的消息发给发送者本人，前端的 webSocketOnMessage 才能触发替换逻辑！
        Session mySession = onlineUsers.get(username);
        if (mySession != null && mySession.isOpen()) {
            mySession.getAsyncRemote().sendText(jsonResponse);
        }

        // 4. 将消息发给接收方
        String toName = messageInfo.getToName();
        if (toName != null && toName.startsWith("GROUP_")) {
            // 【群聊分发】
            List<String> groupMembers = groupService.getGroupMemberNames(toName);
            for (String memberName : groupMembers) {
                Session targetSession = onlineUsers.get(memberName);
                // 已经发给自己了，跳过本人
                if (targetSession != null && targetSession.isOpen() && !memberName.equals(username)) {
                    targetSession.getAsyncRemote().sendText(jsonResponse);
                }
            }
        } else {
            // 【私聊分发】
            Session targetSession = onlineUsers.get(toName);
            // 发给对方，确保对方不是自己
            if (targetSession != null && targetSession.isOpen() && !toName.equals(username)) {
                targetSession.getAsyncRemote().sendText(jsonResponse);
            }
        }
    }

    public Set<User> getFriends() {
        initServices();
        Set<String> userNames = onlineUsers.keySet();
        Set<User> userList = new HashSet<>();
        for (String name : userNames) {
            String path = userService.getPath(name);
            userList.add(new User().builder().userName(name).path(path).build());
        }
        return userList;
    }

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