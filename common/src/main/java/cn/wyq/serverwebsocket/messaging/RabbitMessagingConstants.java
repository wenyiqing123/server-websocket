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
}
