package cn.wyq.serverwebsocket.messaging;

import cn.wyq.serverwebsocket.utils.MailUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * RabbitMQ 错误队列消费者。
 *
 * <p>业务消息达到消费者重试上限后会被投递到 error exchange，
 * 这里将原始消息、来源和异常原因发送到固定运维邮箱。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ErrorMessageConsumer {
    private static final int MAX_EMAIL_CONTENT_LENGTH = 12000;

    private final MailUtil mailUtil;
    private final RabbitMessageIdempotencyService idempotencyService;

    /** 默认运维告警邮箱，可由部署环境覆盖。 */
    @Value("${app.messaging.error-consumer.alert-email:2402317148@qq.com}")
    private String alertEmail;

    /** 监听统一错误队列并发送告警邮件。 */
    @RabbitListener(
            queues = RabbitMessagingConstants.ERROR_QUEUE,
            containerFactory = "errorRabbitListenerContainerFactory")
    public void consumeError(Message message) {
        if (message == null) {
            throw new IllegalArgumentException("错误队列收到空消息");
        }

        MessageProperties properties = message.getMessageProperties();
        Map<String, Object> headers = properties == null || properties.getHeaders() == null
                ? Map.of() : properties.getHeaders();
        String body = message.getBody() == null
                ? "<空消息体>"
                : new String(message.getBody(), StandardCharsets.UTF_8);
        String messageId = firstNonBlank(
                properties == null ? null : properties.getMessageId(),
                header(headers, RabbitMessagingConstants.BUSINESS_MESSAGE_ID_HEADER),
                header(headers, "x-local-message-id"),
                "未知");
        String idempotencyId = "未知".equals(messageId) ? null : messageId;
        if (!idempotencyService.tryAcquire("error-alert", idempotencyId)) {
            log.debug("跳过重复 RabbitMQ 错误告警，messageId={}", idempotencyId);
            return;
        }
        String originalExchange = firstNonBlank(
                header(headers, "x-original-exchange"),
                header(headers, "x-first-death-exchange"),
                "未知");
        String originalRoutingKey = firstNonBlank(
                header(headers, "x-original-routingKey"),
                header(headers, "x-first-death-routing-key"),
                "未知");
        String exceptionMessage = firstNonBlank(
                header(headers, "x-exception-message"),
                header(headers, "x-first-death-reason"),
                "未知");
        String stackTrace = header(headers, "x-exception-stacktrace");

        String content = "RabbitMQ 消息消费失败告警\n"
                + "消息 ID：" + messageId + "\n"
                + "原交换机：" + originalExchange + "\n"
                + "原路由键：" + originalRoutingKey + "\n"
                + "失败原因：" + exceptionMessage + "\n"
                + "异常堆栈：\n" + safe(stackTrace, "<未提供>") + "\n"
                + "原始消息体：\n" + safe(body, "<空消息体>");

        // 邮件正文设置上限，避免异常堆栈过长阻塞错误队列消费者。
        try {
            mailUtil.sendMailNowOrThrow(
                    alertEmail,
                    "【云聊告警】RabbitMQ 消息消费失败",
                    truncate(content, MAX_EMAIL_CONTENT_LENGTH));
            idempotencyService.markSuccess("error-alert", idempotencyId);
            log.error("RabbitMQ 错误消息已发送告警邮件，messageId={}，exchange={}，routingKey={}",
                    messageId, originalExchange, originalRoutingKey);
        } catch (RuntimeException | Error exception) {
            idempotencyService.release("error-alert", idempotencyId);
            throw exception;
        }
    }

    private String header(Map<String, Object> headers, String name) {
        Object value = headers.get(name);
        return value == null ? null : String.valueOf(value);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "未知";
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "\n...[内容已截断]";
    }
}
