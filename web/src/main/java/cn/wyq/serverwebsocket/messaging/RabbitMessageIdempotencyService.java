package cn.wyq.serverwebsocket.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * RabbitMQ 消费幂等门闩。
 *
 * <p>生产者重试或消费者异常恢复时，同一业务消息可能再次到达。这里使用
 * “处理中”短租约和“已完成”长租约：并发重复投递只允许一个消费者执行，
 * 业务失败会释放门闩，进程崩溃也会在短租约到期后自动允许补偿。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitMessageIdempotencyService {
    private static final String KEY_PREFIX = "rabbit:consumer:idempotency:";
    private static final String PROCESSING = "PROCESSING";
    private static final String DONE = "DONE";
    private static final Duration PROCESSING_TTL = Duration.ofMinutes(10);
    private static final Duration DONE_TTL = Duration.ofDays(7);

    private final StringRedisTemplate redisTemplate;

    /**
     * 尝试取得某个消费者对业务消息的执行权。
     *
     * @return {@code true} 表示本次可以执行；{@code false} 表示已完成或正在处理
     */
    public boolean tryAcquire(String consumerName, String messageId) {
        if (isBlank(consumerName) || isBlank(messageId)) {
            // 没有业务 ID 时不能伪造一个公共锁，否则会把不同消息误判为重复。
            return true;
        }
        String key = key(consumerName, messageId);
        try {
            Boolean acquired = redisTemplate.opsForValue()
                    .setIfAbsent(key, PROCESSING, PROCESSING_TTL);
            if (Boolean.TRUE.equals(acquired)) {
                return true;
            }

            String state = redisTemplate.opsForValue().get(key);
            return !PROCESSING.equals(state) && !DONE.equals(state);
        } catch (Exception exception) {
            // 幂等基础设施故障时采用 fail-open，避免 Redis 故障扩大为业务消息全部失败。
            log.warn("读取 RabbitMQ 消费幂等状态失败，放行本次消费，consumer={}, messageId={}",
                    consumerName, messageId, exception);
            return true;
        }
    }

    /** 标记业务处理成功，后续重复消息会直接确认消费。 */
    public void markSuccess(String consumerName, String messageId) {
        if (isBlank(consumerName) || isBlank(messageId)) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(key(consumerName, messageId), DONE, DONE_TTL);
        } catch (Exception exception) {
            log.warn("写入 RabbitMQ 消费幂等状态失败，后续重复消息可能再次执行，consumer={}, messageId={}",
                    consumerName, messageId, exception);
        }
    }

    /** 业务执行失败时释放处理中状态，让 RabbitMQ 的重试能够再次取得执行权。 */
    public void release(String consumerName, String messageId) {
        if (isBlank(consumerName) || isBlank(messageId)) {
            return;
        }
        try {
            redisTemplate.delete(key(consumerName, messageId));
        } catch (Exception exception) {
            log.warn("释放 RabbitMQ 消费幂等状态失败，消息将等待处理中租约过期，consumer={}, messageId={}",
                    consumerName, messageId, exception);
        }
    }

    private String key(String consumerName, String messageId) {
        return KEY_PREFIX + consumerName.trim() + ":" + messageId.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
