package cn.wyq.serverwebsocket.messaging;

/**
 * 资金到期检查消息的 RabbitMQ 拓扑常量。
 *
 * <p>延迟队列使用消息自身的 expiration 属性，过期后由死信路由到统一检查队列。
 * 这样业务可以在不改变 RabbitMQ 队列声明的情况下使用不同的时间块。</p>
 */
public final class ExpiryMessagingConstants {

    private ExpiryMessagingConstants() {
    }

    /** 延迟消息交换机，同时承载延迟队列和检查队列路由。 */
    public static final String EXCHANGE = "money.expiry.exchange";
    /** 统一的到期检查队列。 */
    public static final String CHECK_QUEUE = "money.expiry.check.queue";
    /** 延迟消息最终进入检查队列的路由键。 */
    public static final String CHECK_ROUTING_KEY = "money.expiry.check";

    /** 充值订单延迟队列及路由键。 */
    public static final String RECHARGE_DELAY_QUEUE = "recharge.order.expiry.delay.queue";
    public static final String RECHARGE_DELAY_ROUTING_KEY = "money.expiry.delay.recharge";

    /** 红包、转账共用的资金延迟队列及路由键。 */
    public static final String MONEY_DELAY_QUEUE = "money.expiry.delay.queue";
    public static final String MONEY_DELAY_ROUTING_KEY = "money.expiry.delay.money";

    /** 本地消息表中的统一消息类型。 */
    public static final String MESSAGE_TYPE = "MONEY_EXPIRY_CHECK";

    /** 到期业务类型：充值订单。 */
    public static final String BUSINESS_RECHARGE_ORDER = "RECHARGE_ORDER";
    /** 到期业务类型：红包。 */
    public static final String BUSINESS_RED_PACKET = "RED_PACKET";
    /** 到期业务类型：转账。 */
    public static final String BUSINESS_TRANSFER = "TRANSFER";

    /** 根据业务类型选择对应延迟队列路由键。 */
    public static String delayRoutingKey(String businessType) {
        return switch (businessType) {
            case BUSINESS_RECHARGE_ORDER -> RECHARGE_DELAY_ROUTING_KEY;
            case BUSINESS_RED_PACKET, BUSINESS_TRANSFER -> MONEY_DELAY_ROUTING_KEY;
            default -> throw new IllegalArgumentException("不支持的到期业务类型：" + businessType);
        };
    }
}
