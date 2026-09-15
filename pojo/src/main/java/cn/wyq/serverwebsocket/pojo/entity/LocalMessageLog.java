package cn.wyq.serverwebsocket.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 本地消息表记录。
 *
 * <p>业务事务先把待发送消息写入数据库，再由后台调度器投递到 RabbitMQ。
 * 该对象不代表聊天消息，而是可靠消息投递的 Outbox 记录。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocalMessageLog {
    private Long id;
    private String messageId;
    private String messageType;
    private String exchangeName;
    private String routingKey;
    private String payload;
    private String status;
    private Integer attemptCount;
    private LocalDateTime nextRetryAt;
    private LocalDateTime lastAttemptAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime returnedAt;
    private String lastError;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
