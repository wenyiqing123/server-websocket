package cn.wyq.serverwebsocket.messaging;

import cn.wyq.serverwebsocket.service.LoginAuditService;
import cn.wyq.serverwebsocket.socket.WebSocket;
import cn.wyq.serverwebsocket.utils.MailUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/** 登录模块 RabbitMQ 消费者：落库审计、发送邮件和实时通知。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginRabbitConsumer {
    private final LoginAuditService loginAuditService;
    private final MailUtil mailUtil;

    @RabbitListener(queues = LoginMessagingConstants.AUDIT_QUEUE)
    public void consumeAudit(LoginAuditMessage message) {
        loginAuditService.save(message);
    }

    @RabbitListener(queues = LoginMessagingConstants.MAIL_QUEUE)
    public void consumeMail(MailMessage message) {
        if (message == null || message.getToEmail() == null || message.getToEmail().isBlank()) {
            return;
        }
        mailUtil.sendMailNowOrThrow(message.getToEmail(), message.getSubject(), message.getContent());
    }

    @RabbitListener(queues = LoginMessagingConstants.NOTIFICATION_QUEUE)
    public void consumeNotification(LoginNotificationMessage message) {
        if (message == null || message.getUsername() == null || message.getUsername().isBlank()) {
            return;
        }
        // 兼容升级前已经写入本地消息表的登录成功/异地登录自通知。
        // 这些消息不再符合当前产品规则，消费成功即可，不能继续投递到用户端。
        if (isLegacyLoginNotice(message)) {
            log.info("忽略历史登录自通知，eventId={}, username={}",
                    message.getEventId(), message.getUsername());
            return;
        }
        try {
            WebSocket.sendSystemBroadcast(message.getUsername(), message.getContent(),
                    message.getNotificationType());
        } catch (Exception e) {
            log.warn("登录实时通知发送失败，username={}", message.getUsername(), e);
            throw new IllegalStateException("登录实时通知发送失败，等待 RabbitMQ 重试", e);
        }
    }

    /** 判断升级前产生的、面向登录者自己的登录提示。 */
    private boolean isLegacyLoginNotice(LoginNotificationMessage message) {
        if (message.getNotificationType() != null && !message.getNotificationType().isBlank()) {
            return false;
        }
        String content = message.getContent();
        return content != null
                && (content.startsWith("账号登录成功，登录 IP：")
                || content.startsWith("检测到你的账号从新的 IP 地址登录："));
    }
}
