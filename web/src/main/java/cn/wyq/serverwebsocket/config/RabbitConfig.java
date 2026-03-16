package cn.wyq.serverwebsocket.config;


import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Configuration
public class RabbitConfig {

    // 1. 定义一个队列，名字叫 "ai_task_queue"
    // true 表示持久化（重启 MQ 队列还在）
    @Bean
    public Queue aiTaskQueue() {
        return new Queue("ai_task_queue", true);
    }

    // 2. 使用 JSON 序列化消息（强烈推荐）
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}