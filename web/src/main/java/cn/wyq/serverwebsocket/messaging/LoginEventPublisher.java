package cn.wyq.serverwebsocket.messaging;

import cn.wyq.serverwebsocket.service.LocalMessageService;
import cn.wyq.serverwebsocket.service.FriendService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

/** 登录模块消息生产者，将消息可靠地写入本地消息表。 */
@Component
@Slf4j
@RequiredArgsConstructor
public class LoginEventPublisher {
    private static final String LAST_IP_PREFIX = "login:last-ip:";

    private final StringRedisTemplate redisTemplate;
    private final LocalMessageService localMessageService;
    private final ObjectMapper objectMapper;
    private final FriendService friendService;

    /** MailUtil 发布的本地事件也必须先落本地消息表。 */
    @EventListener
    public void forwardMailEvent(MailMessage message) {
        publishMail(message);
    }

    @Transactional(rollbackFor = Exception.class)
    public void publishLoginSuccess(Integer userId, String username, String email, String eventType,
                                    String ipAddress, String userAgent) {
        String key = LAST_IP_PREFIX + userId;
        String previousIp = redisTemplate.opsForValue().get(key);
        boolean remoteLogin = previousIp != null && !sameIp(previousIp, ipAddress);
        if (ipAddress != null && !ipAddress.isBlank()) {
            redisTemplate.opsForValue().set(key, ipAddress, Duration.ofDays(30));
        }

        publishAudit(LoginAuditMessage.builder()
                .eventId(UUID.randomUUID().toString())
                .userId(userId)
                .username(username)
                .eventType(eventType)
                .success(true)
                .remoteLogin(remoteLogin)
                .previousIp(previousIp)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .occurredAt(LocalDateTime.now())
                .build());

        // 登录成功只通知好友，不再给登录者自己发送系统通知，也不把登录 IP 放进系统公告。
        publishFriendOnlineNotifications(userId, username, ipAddress, userAgent);

        if (remoteLogin && email != null && !email.isBlank()) {
            publishMail(MailMessage.builder()
                    .eventId(UUID.randomUUID().toString())
                    .toEmail(email)
                    .subject("云聊账号异地登录提醒")
                    .content("你的云聊账号刚刚从新的 IP 地址登录：" + safe(ipAddress)
                            + "。如果不是你本人操作，请立即修改密码并检查账号安全。")
                    .templateType("REMOTE_LOGIN_ALERT")
                    .build());
        }
    }

    /**
     * 失败审计必须独立提交。注册失败时外层业务事务会回滚，不能连带丢失这条审计记录。
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void publishLoginFailure(Integer userId, String username, String eventType,
                                    String ipAddress, String userAgent, String reason) {
        publishAudit(LoginAuditMessage.builder()
                .eventId(UUID.randomUUID().toString())
                .userId(userId)
                .username(username)
                .eventType(eventType)
                .success(false)
                .remoteLogin(false)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .failureReason(reason)
                .occurredAt(LocalDateTime.now())
                .build());
    }

    public void publishAudit(LoginAuditMessage message) {
        enqueue(LoginMessagingConstants.TYPE_AUDIT,
                LoginMessagingConstants.AUDIT_ROUTING_KEY, message);
    }

    public void publishNotification(LoginNotificationMessage message) {
        enqueue(LoginMessagingConstants.TYPE_NOTIFICATION,
                LoginMessagingConstants.NOTIFICATION_ROUTING_KEY, message);
    }

    /** 将好友上线通知分别写入本地消息表，由 RabbitMQ 异步投递给每个好友。 */
    private void publishFriendOnlineNotifications(Integer userId, String username,
                                                  String ipAddress, String userAgent) {
        if (username == null || username.isBlank()) {
            return;
        }
        try {
            java.util.List<String> friendNames = friendService.getMyFriendsNameByUsername(username);
            if (friendNames == null) {
                return;
            }
            for (String friendName : friendNames) {
                if (friendName == null || friendName.isBlank() || username.equals(friendName)) {
                    continue;
                }
                publishNotification(LoginNotificationMessage.builder()
                        .eventId(UUID.randomUUID().toString())
                        .userId(userId)
                        .username(friendName)
                        .sourceUsername(username)
                        .notificationType("FRIEND_ONLINE")
                        .ipAddress(ipAddress)
                        .userAgent(userAgent)
                        .content(username + "已上线！")
                        .build());
            }
        } catch (Exception exception) {
            // 上线提示属于附加能力，好友关系查询异常不能阻断登录主流程。
            log.warn("生成好友上线通知失败，username={}", username, exception);
        }
    }

    public void publishMail(MailMessage message) {
        if (message == null) {
            return;
        }
        enqueue(LoginMessagingConstants.TYPE_MAIL,
                LoginMessagingConstants.MAIL_ROUTING_KEY, message);
    }

    @Transactional(rollbackFor = Exception.class)
    public void publishRegistration(Integer userId, String username, String email,
                                    String ipAddress, String userAgent) {
        publishAudit(LoginAuditMessage.builder()
                .eventId(UUID.randomUUID().toString())
                .userId(userId)
                .username(username)
                .eventType("REGISTER_SUCCESS")
                .success(true)
                .remoteLogin(false)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .occurredAt(LocalDateTime.now())
                .build());
        if (email != null && !email.isBlank()) {
            publishMail(MailMessage.builder()
                    .eventId(UUID.randomUUID().toString())
                    .toEmail(email)
                    .subject("欢迎加入云聊")
                    .content("你好，" + safe(username) + "！欢迎加入云聊，祝你使用愉快。")
                    .templateType("REGISTRATION_WELCOME")
                    .build());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void publishVerificationRequested(Integer userId, String email, String ipAddress, String userAgent) {
        publishAudit(LoginAuditMessage.builder()
                .eventId(UUID.randomUUID().toString())
                .userId(userId)
                .username(email)
                .eventType("VERIFICATION_CODE_REQUEST")
                .success(true)
                .remoteLogin(false)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .occurredAt(LocalDateTime.now())
                .build());
    }

    private boolean sameIp(String first, String second) {
        return second != null && first.trim().equalsIgnoreCase(second.trim());
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "未知" : value;
    }

    /** 将业务消息序列化并写入本地消息表，实际投递由可靠消息调度器完成。 */
    private void enqueue(String messageType, String routingKey, Object message) {
        if (message == null) {
            return;
        }
        try {
            localMessageService.enqueue(messageType, LoginMessagingConstants.EXCHANGE,
                    routingKey, objectMapper.writeValueAsString(message));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("登录消息序列化失败，无法写入本地消息表", e);
        }
    }
}
