package cn.wyq.serverwebsocket.messaging;

import cn.wyq.serverwebsocket.service.LoginAuditService;
import cn.wyq.serverwebsocket.socket.WebSocket;
import cn.wyq.serverwebsocket.utils.MailUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/** 登录模块 RabbitMQ 消费者：落库审计、发送邮件和实时通知。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginRabbitConsumer {
    private final LoginAuditService loginAuditService;
    private final MailUtil mailUtil;
    private final RabbitMessageIdempotencyService idempotencyService;

    @RabbitListener(queues = LoginMessagingConstants.AUDIT_QUEUE)
    public void consumeAudit(LoginAuditMessage message,
                             @Header(name = AmqpHeaders.MESSAGE_ID, required = false) String messageId) {
        if (message == null) {
            return;
        }
        String idempotencyId = resolveMessageId(messageId, message.getEventId());
        if (!idempotencyService.tryAcquire("login-audit", idempotencyId)) {
            log.debug("跳过重复登录审计消息，messageId={}", idempotencyId);
            return;
        }
        try {
            loginAuditService.save(message);
            idempotencyService.markSuccess("login-audit", idempotencyId);
        } catch (RuntimeException | Error exception) {
            idempotencyService.release("login-audit", idempotencyId);
            throw exception;
        }
    }

    @RabbitListener(queues = LoginMessagingConstants.MAIL_QUEUE)
    public void consumeMail(MailMessage message,
                            @Header(name = AmqpHeaders.MESSAGE_ID, required = false) String messageId) {
        if (message == null || message.getToEmail() == null || message.getToEmail().isBlank()) {
            return;
        }
        String idempotencyId = resolveMessageId(messageId, message.getEventId());
        if (!idempotencyService.tryAcquire("login-mail", idempotencyId)) {
            log.debug("跳过重复登录邮件消息，messageId={}", idempotencyId);
            return;
        }
        try {
            mailUtil.sendMailNowOrThrow(message.getToEmail(), message.getSubject(), message.getContent());
            idempotencyService.markSuccess("login-mail", idempotencyId);
        } catch (RuntimeException | Error exception) {
            idempotencyService.release("login-mail", idempotencyId);
            throw exception;
        }
    }

    @RabbitListener(queues = LoginMessagingConstants.NOTIFICATION_QUEUE)
    public void consumeNotification(LoginNotificationMessage message,
                                    @Header(name = AmqpHeaders.MESSAGE_ID, required = false) String messageId) {
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
        String idempotencyId = resolveMessageId(messageId, message.getEventId());
        if (!idempotencyService.tryAcquire("login-notification", idempotencyId)) {
            log.debug("跳过重复登录通知消息，messageId={}", idempotencyId);
            return;
        }
        try {
            WebSocket.sendSystemBroadcast(message.getUsername(), message.getContent(),
                    message.getNotificationType());
            idempotencyService.markSuccess("login-notification", idempotencyId);
        } catch (Exception e) {
            idempotencyService.release("login-notification", idempotencyId);
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

    /** 优先使用生产者绑定到 AMQP 属性的本地消息 ID，旧消息回退到业务事件 ID。 */
    private String resolveMessageId(String messageId, String eventId) {
        return messageId == null || messageId.isBlank() ? eventId : messageId;
    }
}
