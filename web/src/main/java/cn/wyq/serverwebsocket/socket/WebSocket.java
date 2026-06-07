package cn.wyq.serverwebsocket.socket;

import cn.wyq.serverwebsocket.common.ApplicationHelper;
import cn.wyq.serverwebsocket.config.WebSocketConfig;
import cn.wyq.serverwebsocket.constant.RedisKeyConstants;
import cn.wyq.serverwebsocket.pojo.User;
import cn.wyq.serverwebsocket.pojo.socket.MessageFull;
import cn.wyq.serverwebsocket.pojo.socket.MessageInfo;
import cn.wyq.serverwebsocket.service.GroupService;
import cn.wyq.serverwebsocket.service.MessageService;
import cn.wyq.serverwebsocket.service.UserService;
import cn.wyq.serverwebsocket.util.MessageUtils;
import cn.wyq.serverwebsocket.utils.RedisUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import jakarta.servlet.http.HttpSession;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
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
    private static RedisTemplate<String, Object> redisTemplate;
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
    private static void initServices() {
        if (userService == null) userService = ApplicationHelper.getBean(UserService.class);
        if (messageService == null) messageService = ApplicationHelper.getBean(MessageService.class);
        if (redisUtil == null) redisUtil = ApplicationHelper.getBean(RedisUtil.class);
        if (groupService == null) groupService = ApplicationHelper.getBean(GroupService.class); // 🌟 新增
        if (redisTemplate == null) redisTemplate = (RedisTemplate<String, Object>) ApplicationHelper.getBean("redisTemplate");
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
    // 🌟 新增：发送系统广播的专属大喇叭
    // =======================================================
    public static void sendSystemBroadcast(String targetUsername, String content) {
        if (targetUsername == null || content == null) return;
        initServices(); // 确保组件已注入

        // 构造消息体
        MessageFull sysMsg = new MessageFull();
        sysMsg.setFromName("SYSTEM_BROADCAST");
        sysMsg.setToName(targetUsername);
        sysMsg.setMessage(content);
        sysMsg.setTime(LocalDateTime.now());

        String jsonMessage = JSON.toJSONString(sysMsg);

        Session targetSession = onlineUsers.get(targetUsername);
        if (targetSession != null && targetSession.isOpen()) {
            // 🟢 用户在线：直接通过 WebSocket 发送
            targetSession.getAsyncRemote().sendText(jsonMessage);
        } else {
            // 🔴 用户离线：存入 Redis 离线信箱！
            // 使用 List 结构，向队列尾部 (RightPush) 追加消息
            String offlineKey = RedisKeyConstants.offlineKey + targetUsername;
            redisTemplate.opsForList().rightPush(offlineKey, jsonMessage);
            // 可选：设置离线消息的过期时间（比如 7 天没上线就作废，避免 Redis 撑爆）
            redisTemplate.expire(offlineKey, 7, TimeUnit.DAYS);
            log.info("用户 [{}] 离线，广播已存入 Redis 离线信箱", targetUsername);
        }
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

        // ==========================================================
        // 🌟 简便魔法：先解析成通用的 JSONObject
        // ==========================================================
        JSONObject jsonObject = JSON.parseObject(message);
        String type = jsonObject.getString("type");

        // ==========================================================
        // 🌟 核心拦截区：合并后的音视频信令拦截 (包含 offer, answer, ice, hangup)
        // ==========================================================
        if ("offer".equals(type) || "answer".equals(type) || "ice".equals(type) || "hangup".equals(type)) {

            String toName = jsonObject.getString("toName");
            Session targetSession = onlineUsers.get(toName);

            // 原封不动地把整串 JSON 转发给对方
            if (targetSession != null && targetSession.isOpen()) {
                // ✅ 修改：使用线程安全方法
                sendMessageSafe(targetSession, message);
                log.info("【音视频信令】转发成功 | 类型: {} | {} -> {}", type, username, toName);
            } else {
                // 对方不在线的情况
                log.warn("【音视频信令】转发失败，对方不在线 | 类型: {} | 目标: {}", type, toName);
            }

            return; // 🛑 拦截成功，直接退出，绝对不走下面的数据库逻辑！
        }

        // ==========================================================
        // 🌟 以下是你原封不动的聊天落库逻辑
        // 当走到这里，说明是普通聊天消息。
        // ==========================================================
        MessageInfo messageInfo = JSON.parseObject(message, MessageInfo.class);

        // 2. 保存到数据库
        messageService.addMessage(messageInfo);

        // 从对象中提取 MyBatis 自动注入的真实 ID
        Integer realId = messageInfo.getId();
        log.info("消息落库成功，真实生成的 ID = {}", realId);

        // 3. 构建包含真实 ID 的完整消息对象
        MessageFull messageFull = JSON.parseObject(message, MessageFull.class);
        messageFull.setId(realId);
        String jsonResponse = JSON.toJSONString(messageFull);

        // 消息必须抄送给自己
        Session mySession = onlineUsers.get(username);
        // ✅ 修改：使用线程安全方法 (内部自带判空验证)
        sendMessageSafe(mySession, jsonResponse);

        // 4. 将消息发给接收方
        String toName = messageInfo.getToName();
        if (toName != null && toName.startsWith("GROUP_")) {
            // 【群聊分发】
            List<String> groupMembers = groupService.getGroupMemberNames(toName);
            for (String memberName : groupMembers) {
                // 不发给自己
                if (!memberName.equals(username)) {
                    Session targetSession = onlineUsers.get(memberName);
                    // ✅ 修改：使用线程安全方法
                    sendMessageSafe(targetSession, jsonResponse);
                }
            }
        } else {
            // 【私聊分发】
            if (!toName.equals(username)) {
                Session targetSession = onlineUsers.get(toName);
                // ✅ 修改：使用线程安全方法
                sendMessageSafe(targetSession, jsonResponse);
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
    /**
     * 🌟 线程安全的发送消息方法（解决 TEXT_FULL_WRITING 报错）
     */
    private void sendMessageSafe(Session session, String message) {
        if (session != null && session.isOpen()) {
            // 对当前 Session 加锁，确保同一时刻只有一个线程在通过该 Session 发消息
            synchronized (session) {
                try {
                    // 🌟 必须改成 getBasicRemote() 同步发送！
                    session.getBasicRemote().sendText(message);
                } catch (IOException e) {
                    log.error("WebSocket 消息发送失败, 目标SessionID: {}", session.getId(), e);
                }
            }
        }
    }
}