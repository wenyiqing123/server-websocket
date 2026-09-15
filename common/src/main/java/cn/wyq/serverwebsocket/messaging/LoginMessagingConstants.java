package cn.wyq.serverwebsocket.messaging;

/** RabbitMQ 登录相关交换机、队列和路由键。 */
public final class LoginMessagingConstants {
    private LoginMessagingConstants() {
    }

    public static final String EXCHANGE = "login.events";
    public static final String AUDIT_QUEUE = "login.audit.queue";
    public static final String MAIL_QUEUE = "login.mail.queue";
    public static final String NOTIFICATION_QUEUE = "login.notification.queue";
    public static final String AUDIT_ROUTING_KEY = "login.audit";
    public static final String MAIL_ROUTING_KEY = "login.mail";
    public static final String NOTIFICATION_ROUTING_KEY = "login.notification";

    /** 本地消息表中的消息类型。 */
    public static final String TYPE_AUDIT = "LOGIN_AUDIT";
    public static final String TYPE_MAIL = "LOGIN_MAIL";
    public static final String TYPE_NOTIFICATION = "LOGIN_NOTIFICATION";
}
