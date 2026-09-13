package cn.wyq.serverwebsocket.messaging;

import cn.wyq.serverwebsocket.utils.MailUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

/** 登录模块消息生产者，将审计、邮件和通知投递到 RabbitMQ。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginEventPublisher {
    private static final String LAST_IP_PREFIX = "login:last-ip:";

    private final RabbitTemplate rabbitTemplate;
    private final StringRedisTemplate redisTemplate;
    private final MailUtil mailUtil;

    /** MailUtil 发布的本地事件由此转发到 RabbitMQ。 */
    @EventListener
    public void forwardMailEvent(MailMessage message) {
        publishMail(message);
    }

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

        publishNotification(LoginNotificationMessage.builder()
                .eventId(UUID.randomUUID().toString())
                .userId(userId)
                .username(username)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .content(remoteLogin
                        ? "检测到你的账号从新的 IP 地址登录：" + safe(ipAddress)
                        : "账号登录成功，登录 IP：" + safe(ipAddress))
                .build());

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
        try {
            rabbitTemplate.convertAndSend(LoginMessagingConstants.EXCHANGE,
                    LoginMessagingConstants.AUDIT_ROUTING_KEY, message);
        } catch (Exception e) {
            log.warn("登录审计消息投递失败，主业务继续执行，eventId={}", message == null ? null : message.getEventId(), e);
        }
    }

    public void publishNotification(LoginNotificationMessage message) {
        try {
            rabbitTemplate.convertAndSend(LoginMessagingConstants.EXCHANGE,
                    LoginMessagingConstants.NOTIFICATION_ROUTING_KEY, message);
        } catch (Exception e) {
            log.warn("登录通知消息投递失败，eventId={}", message == null ? null : message.getEventId(), e);
        }
    }

    public void publishMail(MailMessage message) {
        if (message == null) {
            return;
        }
        try {
            rabbitTemplate.convertAndSend(LoginMessagingConstants.EXCHANGE,
                    LoginMessagingConstants.MAIL_ROUTING_KEY, message);
        } catch (Exception e) {
            // RabbitMQ 暂不可用时保留原有直发兜底，避免验证码功能完全不可用。
            log.warn("邮件消息投递失败，使用本地异步兜底，eventId={}", message.getEventId(), e);
            mailUtil.sendMailAsync(message.getToEmail(), message.getSubject(), message.getContent());
        }
    }

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
}
