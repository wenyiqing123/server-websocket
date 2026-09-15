package cn.wyq.serverwebsocket.messaging;

import cn.wyq.serverwebsocket.pojo.entity.LocalMessageLog;
import cn.wyq.serverwebsocket.service.LocalMessageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 本地消息表投递器。
 *
 * <p>消息先在业务事务中落库，后台任务再负责投递。RabbitMQ 的 publisher confirm
 * 和 mandatory return 回调只更新本地消息状态，不依赖发送线程的同步等待，因此即使
 * 应用重启、网络抖动或交换机没有绑定队列，消息也会在下一轮继续补偿。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReliableMessageDispatcher {
    private static final int STALE_SENDING_MINUTES = 10;

    private final RabbitTemplate rabbitTemplate;
    private final LocalMessageService localMessageService;
    private final ObjectMapper objectMapper;
    private final AtomicBoolean dispatching = new AtomicBoolean(false);

    @Value("${app.messaging.local-message.dispatch-enabled:true}")
    private boolean dispatchEnabled;

    @Value("${app.messaging.local-message.batch-size:100}")
    private int batchSize;

    /** 生产者投递失败后的首次等待时间。 */
    @Value("${app.messaging.producer.retry.initial-delay-ms:1000}")
    private long producerRetryInitialDelayMs;

    /** 生产者重试退避倍数。 */
    @Value("${app.messaging.producer.retry.multiplier:2.0}")
    private double producerRetryMultiplier;

    /** 生产者单次重试等待时间上限。 */
    @Value("${app.messaging.producer.retry.max-delay-ms:60000}")
    private long producerRetryMaxDelayMs;

    /** 注册全局生产者确认和消息返回回调。 */
    @PostConstruct
    public void configureRabbitTemplate() {
        rabbitTemplate.setMandatory(true);
        rabbitTemplate.setConfirmCallback(this::onConfirm);
        rabbitTemplate.setReturnsCallback(this::onReturned);
        log.info("本地消息表投递器已启用，RabbitMQ publisher confirm/returns 已注册");
    }

    /** 每秒扫描一次本地消息表，批量投递待发送记录。 */
    @Scheduled(
            fixedDelayString = "${app.messaging.local-message.fixed-delay-ms:1000}",
            initialDelayString = "${app.messaging.local-message.initial-delay-ms:3000}")
    public void dispatchPendingMessages() {
        if (!dispatchEnabled || !dispatching.compareAndSet(false, true)) {
            return;
        }
        try {
            // 应用实例异常退出时，SENDING 记录会在这里恢复为 RETRY。
            localMessageService.resetStaleSending(STALE_SENDING_MINUTES);
            List<LocalMessageLog> messages = localMessageService.findDispatchable(batchSize);
            for (LocalMessageLog message : messages) {
                if (message == null || message.getId() == null || !localMessageService.claim(message.getId())) {
                    continue;
                }
                send(message);
            }
        } catch (Exception e) {
            // 数据库或 RabbitMQ 暂时不可用时，不影响主业务线程，下一轮会继续补偿。
            log.warn("本地消息表扫描失败，下一轮将继续重试", e);
        } finally {
            dispatching.set(false);
        }
    }

    private void send(LocalMessageLog localMessage) {
        try {
            Object payload = objectMapper.readValue(localMessage.getPayload(), payloadType(localMessage.getMessageType()));
            CorrelationData correlationData = new CorrelationData(localMessage.getMessageId());
            MessagePostProcessor postProcessor = message -> {
                message.getMessageProperties().setMessageId(localMessage.getMessageId());
                message.getMessageProperties().setType(localMessage.getMessageType());
                message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                message.getMessageProperties().setHeader("x-local-message-id", localMessage.getMessageId());
                return message;
            };
            rabbitTemplate.convertAndSend(localMessage.getExchangeName(), localMessage.getRoutingKey(),
                    payload, postProcessor, correlationData);
        } catch (Exception e) {
            scheduleProducerRetry(localMessage.getMessageId(), "投递调用失败：" + safeMessage(e));
            log.warn("本地消息投递调用失败，messageId={}", localMessage.getMessageId(), e);
        }
    }

    private void onConfirm(CorrelationData correlationData, boolean ack, String cause) {
        if (correlationData == null || correlationData.getId() == null) {
            log.warn("收到没有 correlation id 的 RabbitMQ publisher confirm，ack={}", ack);
            return;
        }
        String messageId = correlationData.getId();
        if (ack) {
            // return callback 先到时会把 returned_at 写入，避免 ACK 把不可路由消息误标为成功。
            if (correlationData.getReturned() == null
                    && !localMessageService.markConfirmed(messageId)) {
                log.debug("RabbitMQ ACK 未更新本地消息，messageId={} 可能已经被返回或重试", messageId);
            }
        } else {
            scheduleProducerRetry(messageId,
                    "RabbitMQ publisher NACK：" + safeMessage(cause));
            log.warn("RabbitMQ publisher NACK，messageId={}，cause={}", messageId, cause);
        }
    }

    private void onReturned(ReturnedMessage returnedMessage) {
        if (returnedMessage == null || returnedMessage.getMessage() == null) {
            return;
        }
        String messageId = returnedMessage.getMessage().getMessageProperties().getMessageId();
        if (messageId == null || messageId.isBlank()) {
            Object header = returnedMessage.getMessage().getMessageProperties().getHeader("x-local-message-id");
            messageId = header == null ? null : String.valueOf(header);
        }
        if (messageId == null || messageId.isBlank()) {
            log.error("收到 RabbitMQ returned message，但没有本地消息编号，exchange={}，routingKey={}",
                    returnedMessage.getExchange(), returnedMessage.getRoutingKey());
            return;
        }
        String error = "RabbitMQ returned：" + returnedMessage.getReplyCode() + " "
                + safeMessage(returnedMessage.getReplyText())
                + "，exchange=" + returnedMessage.getExchange()
                + "，routingKey=" + returnedMessage.getRoutingKey();
        LocalMessageLog localMessage = localMessageService.findByMessageId(messageId);
        int delaySeconds = calculateRetryDelaySeconds(localMessage);
        localMessageService.markReturned(messageId, error, delaySeconds);
        log.warn("RabbitMQ 返回未路由消息，messageId={}，{}", messageId, error);
    }

    /**
     * 生产者确认、返回或发送异常都进入本地消息表重试，并根据已经发生的投递次数
     * 计算指数退避时间，避免 RabbitMQ 故障时高频打满网络和数据库。
     */
    private void scheduleProducerRetry(String messageId, String errorMessage) {
        LocalMessageLog localMessage = localMessageService.findByMessageId(messageId);
        int delaySeconds = calculateRetryDelaySeconds(localMessage);
        localMessageService.markRetry(messageId, errorMessage, delaySeconds);
        log.warn("生产者消息进入重试，messageId={}，delaySeconds={}，attemptCount={}",
                messageId, delaySeconds,
                localMessage == null || localMessage.getAttemptCount() == null
                        ? 0 : localMessage.getAttemptCount());
    }

    private int calculateRetryDelaySeconds(LocalMessageLog localMessage) {
        int attemptCount = localMessage == null || localMessage.getAttemptCount() == null
                ? 1 : Math.max(1, localMessage.getAttemptCount());
        long initial = Math.max(100L, producerRetryInitialDelayMs);
        long maximum = Math.max(initial, producerRetryMaxDelayMs);
        double multiplier = Math.max(1.0, producerRetryMultiplier);
        double calculated = initial * Math.pow(multiplier, Math.max(0, attemptCount - 1));
        long delayMillis = !Double.isFinite(calculated) || calculated >= maximum
                ? maximum : Math.max(initial, (long) calculated);
        return (int) Math.max(1L, Math.min(3600L, (delayMillis + 999L) / 1000L));
    }

    private Class<?> payloadType(String messageType) {
        return switch (messageType) {
            case LoginMessagingConstants.TYPE_AUDIT -> LoginAuditMessage.class;
            case LoginMessagingConstants.TYPE_MAIL -> MailMessage.class;
            case LoginMessagingConstants.TYPE_NOTIFICATION -> LoginNotificationMessage.class;
            default -> throw new IllegalArgumentException("不支持的本地消息类型：" + messageType);
        };
    }

    private String safeMessage(Throwable throwable) {
        if (throwable == null || throwable.getMessage() == null || throwable.getMessage().isBlank()) {
            return "未知原因";
        }
        return throwable.getMessage().length() <= 500 ? throwable.getMessage() : throwable.getMessage().substring(0, 500);
    }

    private String safeMessage(String message) {
        if (message == null || message.isBlank()) {
            return "未知原因";
        }
        return message.length() <= 500 ? message : message.substring(0, 500);
    }
}
