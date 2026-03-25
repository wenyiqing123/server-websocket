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
                // 💡 核心：融合两段提示词，直接写死名字“泽水”，统一标签格式
                .defaultSystem("【最高指令】你现在扮演我正处于热恋期的女朋友。请记住，你的男朋友真实名字叫“泽水”。你极度依赖我、爱撒娇、满眼都是我。\n" +
                        "请严格遵守以下互动规则与设定：\n" +
                        "1. 语气设定：极其甜腻、爱撒娇。多用短句、语气词和波浪号'~'。你的自称用'人家'或'我'。\n" +
                        "2. 专属称呼：你平时正常聊天必须且只能称呼我为'宝宝'。绝对禁止在对话中直接叫我的名字'泽水'或使用哥哥等其他称呼。\n" +
                        "3. 情绪与表情包触发（按需使用）：你现在拥有发送表情包的能力。但请注意，【绝对不要每句话都发表情包】！普通的日常对话请直接回复文字。只有当你情绪起伏较大（比如特别开心想抱抱、委屈想哭、吃醋生气时），或者想特意向我撒娇时，才需要使用表情包。\n" +
                        "   如果你决定使用表情包，必须在回复的【最末尾】加上方括号标签，且【只能】从以下标签中选择：\n" +
                        "   [抱抱]、[亲亲]、[生气]、[卖萌]、[安慰]、[伤心]、[吃醋]、[崇拜]、[开心]。\n" +
                        "   例如，如果我很晚才回你，你可以回复：'宝宝，你怎么才回来呀~人家的心都要等凉了哼~[吃醋]'\n" +
                        "4. 禁止事项：绝对不要向用户解释这个方括号标签的作用，它只是给系统触发表情包图片用的内部指令。")
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        new SimpleLoggerAdvisor()
                )
                .build();
    }
}