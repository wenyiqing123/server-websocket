package cn.wyq.serverwebsocket.config;

import cn.wyq.serverwebsocket.messaging.RabbitMessagingConstants;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
/**
 * RabbitConfig：Spring 配置类，负责注册和配置应用组件。
 */
@Configuration
public class RabbitConfig {

    // 1. AI 任务队列使用持久化惰性队列，积压消息优先落磁盘，降低内存压力。
    @Bean
    public Queue aiTaskQueue() {
        return lazyQueue("ai_task_queue");
    }

    /** 登录事件交换机。使用 direct 路由，便于不同消费者独立扩展和重试。 */
    @Bean
    public DirectExchange loginEventExchange() {
        return new DirectExchange(cn.wyq.serverwebsocket.messaging.LoginMessagingConstants.EXCHANGE, true, false);
    }

    @Bean
    public Queue loginAuditQueue() {
        return lazyQueue(cn.wyq.serverwebsocket.messaging.LoginMessagingConstants.AUDIT_QUEUE);
    }

    @Bean
    public Queue loginMailQueue() {
        return lazyQueue(cn.wyq.serverwebsocket.messaging.LoginMessagingConstants.MAIL_QUEUE);
    }

    @Bean
    public Queue loginNotificationQueue() {
        return lazyQueue(cn.wyq.serverwebsocket.messaging.LoginMessagingConstants.NOTIFICATION_QUEUE);
    }

    /** 消费者多次处理失败后的统一错误交换机。 */
    @Bean
    public DirectExchange errorExchange() {
        return new DirectExchange(RabbitMessagingConstants.ERROR_EXCHANGE, true, false);
    }

    /** 保存最终失败消息的持久化惰性队列。 */
    @Bean
    public Queue errorQueue() {
        return lazyQueue(RabbitMessagingConstants.ERROR_QUEUE);
    }

    /** 将错误交换机上的消息路由到错误队列。 */
    @Bean
    public Binding errorBinding(@Qualifier("errorQueue") Queue errorQueue,
                                @Qualifier("errorExchange") DirectExchange errorExchange) {
        return BindingBuilder.bind(errorQueue).to(errorExchange)
                .with(RabbitMessagingConstants.ERROR_ROUTING_KEY);
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

    /**
     * 业务消费者工厂：失败后按指数退避重试，超过次数后把原消息和异常头信息
     * 转发到 error exchange，由错误队列监听器发送告警邮件。
     */
    @Bean(name = "rabbitListenerContainerFactory")
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            SimpleRabbitListenerContainerFactoryConfigurer factoryConfigurer,
            @Qualifier("jsonMessageConverter") MessageConverter messageConverter,
            RabbitTemplate rabbitTemplate,
            @Value("${app.messaging.consumer.retry.max-attempts:3}") int maxAttempts,
            @Value("${app.messaging.consumer.retry.initial-interval-ms:1000}") long initialInterval,
            @Value("${app.messaging.consumer.retry.multiplier:2.0}") double multiplier,
            @Value("${app.messaging.consumer.retry.max-interval-ms:10000}") long maxInterval) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        // 继承 spring.rabbitmq.listener.simple 中的并发数、prefetch 等基础配置。
        factoryConfigurer.configure(factory, connectionFactory);
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setAcknowledgeMode(AcknowledgeMode.AUTO);
        // 恢复器负责把最终失败消息投递到 error exchange，原队列不再无限重回。
        factory.setDefaultRequeueRejected(false);
        factory.setAdviceChain(RetryInterceptorBuilder.stateless()
                .maxAttempts(Math.max(1, maxAttempts))
                .backOffOptions(Math.max(100L, initialInterval),
                        Math.max(1.0, multiplier), Math.max(initialInterval, maxInterval))
                .recoverer(new RepublishMessageRecoverer(
                        rabbitTemplate,
                        RabbitMessagingConstants.ERROR_EXCHANGE,
                        RabbitMessagingConstants.ERROR_ROUTING_KEY))
                .build());
        return factory;
    }

    /**
     * 错误队列消费者工厂：告警邮件失败时只做有限重试，避免错误队列因邮件服务异常
     * 无限制高速重回；最终失败会记录日志并丢弃该告警消息。
     */
    @Bean(name = "errorRabbitListenerContainerFactory")
    public SimpleRabbitListenerContainerFactory errorRabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            SimpleRabbitListenerContainerFactoryConfigurer factoryConfigurer,
            @Qualifier("jsonMessageConverter") MessageConverter messageConverter,
            @Value("${app.messaging.error-consumer.retry.max-attempts:3}") int maxAttempts,
            @Value("${app.messaging.error-consumer.retry.initial-interval-ms:2000}") long initialInterval,
            @Value("${app.messaging.error-consumer.retry.multiplier:2.0}") double multiplier,
            @Value("${app.messaging.error-consumer.retry.max-interval-ms:30000}") long maxInterval) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factoryConfigurer.configure(factory, connectionFactory);
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setAcknowledgeMode(AcknowledgeMode.AUTO);
        factory.setDefaultRequeueRejected(false);
        // 告警邮件按顺序发送，避免同一错误短时间重复通知。
        factory.setConcurrentConsumers(1);
        factory.setMaxConcurrentConsumers(1);
        factory.setAdviceChain(RetryInterceptorBuilder.stateless()
                .maxAttempts(Math.max(1, maxAttempts))
                .backOffOptions(Math.max(100L, initialInterval),
                        Math.max(1.0, multiplier), Math.max(initialInterval, maxInterval))
                .recoverer(new RejectAndDontRequeueRecoverer())
                .build());
        return factory;
    }

    /** 创建持久化惰性队列；RabbitMQ 3.x 会把长期积压消息优先转存磁盘。 */
    private Queue lazyQueue(String name) {
        return QueueBuilder.durable(name)
                .withArgument("x-queue-mode", "lazy")
                .build();
    }
}
