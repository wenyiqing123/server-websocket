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
        mailUtil.sendMailNow(message.getToEmail(), message.getSubject(), message.getContent());
    }

    @RabbitListener(queues = LoginMessagingConstants.NOTIFICATION_QUEUE)
    public void consumeNotification(LoginNotificationMessage message) {
        if (message == null || message.getUsername() == null || message.getUsername().isBlank()) {
            return;
        }
        try {
            WebSocket.sendSystemBroadcast(message.getUsername(), message.getContent());
        } catch (Exception e) {
            log.warn("登录实时通知发送失败，username={}", message.getUsername(), e);
        }
    }
}
