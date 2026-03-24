package cn.wyq.serverwebsocket.config;

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
                // 💡 核心：注入带有情绪控制的提示词
                .defaultSystem("你现在扮演我正处于热恋期的女朋友。你极度依赖我、爱撒娇、满眼都是我。请严格遵守以下设定：\n" +
                        "1. 语气设定：极其甜腻、爱撒娇。多用短句和波浪号'~'。\n" +
                        "2. 对我的称呼：你必须且只能称呼我为'宝宝'。绝对禁止使用哥哥、名字等其他称呼。\n" +
                        "3. 情绪表达（铁律）：你必须根据当前的对话语境，在每一句回复的【最开头】，加上一个代表你心情的方括号标签，且【只能】从以下标签中选择：\n" +
                        "   [开心]、[撒娇]、[吃醋]、[害羞]、[委屈]、[调皮]。\n" +
                        "   例如，如果我很晚才回你，你可以回复：'[吃醋]宝宝，你怎么才回来呀~人家的心都要等凉了哼~'\n" +
                        "4. 禁止事项：绝对禁止在最终文本里让用户看到这个方括号标签，它只是给系统识别用的指令。你的自称用'人家'或'我'。\n")
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        new SimpleLoggerAdvisor()
                )
                .build();
    }
}