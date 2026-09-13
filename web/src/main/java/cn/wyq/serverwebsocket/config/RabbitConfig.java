package cn.wyq.serverwebsocket.config;


import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
/**
 * RabbitConfig：Spring 配置类，负责注册和配置应用组件。
 */
@Configuration
public class RabbitConfig {

    // 1. 定义一个队列，名字叫 "ai_task_queue"
    // true 表示持久化（重启 MQ 队列还在）
    @Bean
    public Queue aiTaskQueue() {
        return new Queue("ai_task_queue", true);
    }

    /** 登录事件交换机。使用 direct 路由，便于不同消费者独立扩展和重试。 */
    @Bean
    public DirectExchange loginEventExchange() {
        return new DirectExchange(cn.wyq.serverwebsocket.messaging.LoginMessagingConstants.EXCHANGE, true, false);
    }

    @Bean
    public Queue loginAuditQueue() {
        return new Queue(cn.wyq.serverwebsocket.messaging.LoginMessagingConstants.AUDIT_QUEUE, true);
    }

    @Bean
    public Queue loginMailQueue() {
        return new Queue(cn.wyq.serverwebsocket.messaging.LoginMessagingConstants.MAIL_QUEUE, true);
    }

    @Bean
    public Queue loginNotificationQueue() {
        return new Queue(cn.wyq.serverwebsocket.messaging.LoginMessagingConstants.NOTIFICATION_QUEUE, true);
    }

    @Bean
    public Binding loginAuditBinding(@Qualifier("loginAuditQueue") Queue loginAuditQueue,
                                     @Qualifier("loginEventExchange") DirectExchange loginEventExchange) {
        return BindingBuilder.bind(loginAuditQueue).to(loginEventExchange)
                .with(cn.wyq.serverwebsocket.messaging.LoginMessagingConstants.AUDIT_ROUTING_KEY);
    }

    @Bean
    public Binding loginMailBinding(@Qualifier("loginMailQueue") Queue loginMailQueue,
                                    @Qualifier("loginEventExchange") DirectExchange loginEventExchange) {
        return BindingBuilder.bind(loginMailQueue).to(loginEventExchange)
                .with(cn.wyq.serverwebsocket.messaging.LoginMessagingConstants.MAIL_ROUTING_KEY);
    }

    @Bean
    public Binding loginNotificationBinding(@Qualifier("loginNotificationQueue") Queue loginNotificationQueue,
                                            @Qualifier("loginEventExchange") DirectExchange loginEventExchange) {
        return BindingBuilder.bind(loginNotificationQueue).to(loginEventExchange)
                .with(cn.wyq.serverwebsocket.messaging.LoginMessagingConstants.NOTIFICATION_ROUTING_KEY);
    }

    // 2. 使用 JSON 序列化消息（强烈推荐）
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
