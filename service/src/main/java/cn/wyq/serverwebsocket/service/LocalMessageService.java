package cn.wyq.serverwebsocket.service;

import cn.wyq.serverwebsocket.pojo.entity.LocalMessageLog;

import java.util.List;

/** 本地消息表服务，负责可靠消息的持久化和状态流转。 */
public interface LocalMessageService {

    /**
     * 在当前事务中保存待投递消息。
     *
     * @return 本地消息唯一编号，作为 RabbitMQ 的 correlation id
     */
    String enqueue(String messageType, String exchangeName, String routingKey, String payload);

    /** 按稳定业务 ID 幂等保存待投递消息。 */
    String enqueueWithMessageId(String messageId, String messageType, String exchangeName,
                                String routingKey, String payload);

    /** 查询一批可以投递的消息。 */
    List<LocalMessageLog> findDispatchable(int limit);

    /** 根据本地消息编号读取当前投递次数，用于计算生产者退避时间。 */
    LocalMessageLog findByMessageId(String messageId);

    /** 抢占消息，成功后才允许向 RabbitMQ 发送。 */
    boolean claim(Long id);

    /** 清理宕机实例留下的发送中记录。 */
    int resetStaleSending(int staleMinutes);

    /** 生产者确认成功。 */
    boolean markConfirmed(String messageId);

    /** 生产者 NACK 或发送异常。 */
    boolean markRetry(String messageId, String errorMessage, int delaySeconds);

    /** 消息返回。 */
    boolean markReturned(String messageId, String errorMessage, int delaySeconds);
}
