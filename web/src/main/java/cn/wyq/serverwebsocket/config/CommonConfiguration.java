package cn.wyq.serverwebsocket.config;

import cn.wyq.serverwebsocket.constant.ModelConstant;
import cn.wyq.serverwebsocket.tool.ChatMessageTool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.jdbc.JdbcChatMemoryRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * CommonConfiguration：Spring 配置类，负责注册和配置应用组件。
 */
@Configuration
@Slf4j
@RequiredArgsConstructor
public class CommonConfiguration {

    private final ChatMessageTool chatMessageTool;

    /**
     * 💡 M8 记忆引擎：改名为 customChatMemory 避开官方默认 Bean
     */
    @Bean("customChatMemory")
    public ChatMemory customChatMemory(JdbcTemplate jdbcTemplate) {
        log.info("【系统加载】启动 Spring AI M8 官方正式版 JDBC 记忆引擎 (自定义窗口版)！");

        JdbcChatMemoryRepository repository = JdbcChatMemoryRepository.builder()
                .jdbcTemplate(jdbcTemplate)
                .build();

        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(repository)
                .maxMessages(20) // 防止 Token 爆炸
                .build();
    }

    /**
     * 🤖 1号智能体：仿真女友
     * 🌟 使用 @Qualifier 明确指定注入我们的 customChatMemory
     */
    @Bean("girlfriendChatClient")
    public ChatClient girlfriendChatClient(ChatClient.Builder builder,
                                           @Qualifier("customChatMemory") ChatMemory chatMemory) {
        log.info("【系统加载】启动仿真女友（Qwen 3.8 Max 驱动版）装载完毕！");
        return builder
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        new SimpleLoggerAdvisor()
                )
                .build();
    }

    /**
     * 🕵️ 2号智能体：官方客服。
     * 仅暴露受当前用户身份约束的代发消息工具；不能由模型提示词代替数据权限校验。
     */
    @Bean("serviceChatClient")
    public ChatClient serviceChatClient(ChatClient.Builder builder,
                                        @Qualifier("customChatMemory") ChatMemory chatMemory) {
        log.info("【系统加载】启动官方客服（Qwen 3.8 Max 驱动版），装载受限代发消息工具！");
        return builder
                .defaultSystem(ModelConstant.SERVICE_SYSTEM_PROMPT)
                .defaultTools(chatMessageTool)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        new SimpleLoggerAdvisor()
                )
                .build();
    }
}
