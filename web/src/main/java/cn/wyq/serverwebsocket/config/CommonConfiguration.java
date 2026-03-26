package cn.wyq.serverwebsocket.config;

import cn.wyq.serverwebsocket.constant.ModelConstant;
import cn.wyq.serverwebsocket.tool.MessageAnalysisTool;
import cn.wyq.serverwebsocket.tool.MessageTool;
import cn.wyq.serverwebsocket.tool.SystemStatsTool;
import cn.wyq.serverwebsocket.tool.UserTool;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class CommonConfiguration {
    private final UserTool userTool;
    private final MessageTool messageTool;
    private final MessageAnalysisTool messageAnalysisTool;
    private final SystemStatsTool systemStatsTool;

    // 💡 记忆引擎保持不变，作为底层基础设施供所有智能体使用
    @Bean
    public ChatMemory chatMemory(JdbcChatMemoryRepository repository) {
        log.info("【系统加载】启动 Spring AI 官方正式版 JDBC 记忆引擎！");
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(repository)
                .maxMessages(20)
                .build();
    }

    /**
     * 🤖 1号智能体：仿真女友（沉浸式角色扮演）
     * 特点：纯聊天，拥有记忆，但不附带任何数据库查询工具，绝对安全
     */
    @Bean("girlfriendChatClient")
    public ChatClient girlfriendChatClient(ChatClient.Builder builder, ChatMemory chatMemory) {
        log.info("【系统加载】启动仿真女友（自带表情包）装载完毕！");
        return builder
                .defaultSystem(ModelConstant.GirlFriend_SYSTEM_PROMPT)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        new SimpleLoggerAdvisor()
                )
                .build();
    }

    /**
     * 🕵️ 2号智能体：全能数据管家（严谨的系统管理员）
     * 特点：拥有严谨的人设，且装配了强大的数据库 Tool 权限
     */
    @Bean("serviceChatClient")
    public ChatClient serviceChatClient(ChatClient.Builder builder, ChatMemory chatMemory) {
        log.info("【系统加载】启动全能数据管家，装载数据库查询工具！");
        return builder
                .defaultSystem(ModelConstant.SERVICE_SYSTEM_PROMPT)
                // 💡 核心魔法：只给管家装配你在 MessageTool 里写的查询工具！
                // 注意：这里的 "queryMessage" 必须和你 @Tool 注解所在的方法名（或指定的 name）完全一致
                .defaultTools(userTool, messageTool,messageAnalysisTool,systemStatsTool)
                .defaultAdvisors(
                        // 管家同样可以拥有记忆，这样你可以让他“对比上一次的查询结果”
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        new SimpleLoggerAdvisor()
                )
                .build();
    }
}