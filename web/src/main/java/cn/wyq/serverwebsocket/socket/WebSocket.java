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

    @OnMessage
    public void onMsg(Session session, String message, @PathParam("username") String username) throws IOException {
        initServices();
        log.info("收到来自[{}]的消息: {}", username, message);

        // 1. 解析并保存消息到数据库
        MessageInfo messageInfo = JSON.parseObject(message, MessageInfo.class);
        Integer id = messageService.addMessage(messageInfo);

        // 2. 构建完整消息对象返回
        MessageFull messageFull = JSON.parseObject(message, MessageFull.class);
        messageFull.setId(id);
        String jsonResponse = JSON.toJSONString(messageFull);

        // 3. 🌟 核心路由逻辑：不再全服广播，而是区分私聊和群聊
        String toName = messageInfo.getToName();

        if (toName != null && toName.startsWith("GROUP_")) {
            // 【群聊分发】
            // 通过 GroupService 查询这个群里所有的成员账号
            List<String> groupMembers = groupService.getGroupMemberNames(toName);
            for (String memberName : groupMembers) {
                Session targetSession = onlineUsers.get(memberName);
                // 如果该成员在线，且不是发送者本人（前端已在本地push），则发送
                if (targetSession != null && targetSession.isOpen() && !memberName.equals(username)) {
                    targetSession.getAsyncRemote().sendText(jsonResponse);
                }
            }
        } else {
            // 【私聊分发】
            Session targetSession = onlineUsers.get(toName);
            if (targetSession != null && targetSession.isOpen()) {
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