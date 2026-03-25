package cn.wyq.serverwebsocket.config;

import cn.wyq.serverwebsocket.constant.ModelConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class CommonConfiguration {

    // 💡 修复 1：不要自己去 new，直接用 ChatMemoryRepository 接口让 Spring 自动注入
    @Bean
    public ChatMemory chatMemory(JdbcChatMemoryRepository repository) {
        log.info("【系统加载】启动 Spring AI 官方正式版 JDBC 记忆引擎！");
        // 使用滑动窗口记忆，包装底层的数据库引擎
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(repository)
                .maxMessages(20)
                .build();
    }

    // CommonConfiguration.java

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder, ChatMemory chatMemory) {
        log.info("【系统加载】启动仿真女友（自带表情包）装载完毕！");
        return builder
                // 💡 核心：融合两段提示词，直接写死名字“泽水”，统一标签格式
                .defaultSystem(ModelConstant.GirlFriend_prompt)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        new SimpleLoggerAdvisor()
                )
                .build();
    }
}