package cn.wyq.serverwebsocket.messaging;

/**
 * RabbitMQ 通用基础设施的交换机、队列和路由键常量。
 */
public final class RabbitMessagingConstants {

    private RabbitMessagingConstants() {
    }

    /** 消费失败后的统一错误交换机。 */
    public static final String ERROR_EXCHANGE = "message.error.exchange";
    /** 消费失败后的统一错误队列。 */
    public static final String ERROR_QUEUE = "message.error.queue";
    /** 错误消息路由键。 */
    public static final String ERROR_ROUTING_KEY = "message.error";

    /**
     * 本地消息表业务 ID 的统一消息头。
     *
     * <p>该值与 RabbitMQ 的 correlation id、messageId 保持完全一致，
     * 方便生产者确认、消息返回和错误队列从任意回调入口定位同一条业务消息。</p>
     */
    public static final String BUSINESS_MESSAGE_ID_HEADER = "x-business-message-id";
}
