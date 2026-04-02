package cn.wyq.serverwebsocket.service.impl;

import cn.wyq.serverwebsocket.constant.ModelConstant;
import cn.wyq.serverwebsocket.exception.ServiceException;
import cn.wyq.serverwebsocket.mapper.ChatMapper;
import cn.wyq.serverwebsocket.pojo.entity.Chat;
import cn.wyq.serverwebsocket.pojo.entity.SysConfig;
import cn.wyq.serverwebsocket.service.ChatService;
import cn.wyq.serverwebsocket.service.SysConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.zhipuai.ZhiPuAiChatOptions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ChatServiceImpl implements ChatService {

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;
    private final ChatMapper chatMapper;
    private final VectorStore vectorStore;
    private final SysConfigService sysConfigService;

    public ChatServiceImpl(
            ChatClient.Builder chatClientBuilder, // 推荐注入 Builder 来构建统一 Client
            ChatMemory chatMemory,
            ChatMapper chatMapper,
            VectorStore vectorStore,
            SysConfigService sysConfigService) {
        this.chatClient = chatClientBuilder.build(); // 构建单一模型实例
        this.chatMemory = chatMemory;
        this.chatMapper = chatMapper;
        this.vectorStore = vectorStore;
        this.sysConfigService = sysConfigService;
    }

    private String getIsolatedMemoryId(String agentType, String sessionId) {
        String safeAgentType = StringUtils.hasText(agentType) ? agentType : "girlfriend";
        return safeAgentType + ":" + sessionId;
    }

    @Override
    public Flux<String> chatStream(String message, String sessionId, String userName, String agentType, String docId) {
        try {
            String memoryId = getIsolatedMemoryId(agentType, sessionId);

            // 1. 基础构建：使用同一个大模型，并挂载历史记忆
            // 1. 基础构建：使用同一个大模型，并挂载历史记忆
            var clientRequestSpec = chatClient.prompt()
                    .user(message)
                    // 🌟 拆分第一步：先挂载记忆拦截器实例
                    .advisors(new MessageChatMemoryAdvisor(chatMemory))
                    // 🌟 拆分第二步：再挂载拦截器需要的动态参数（传入 memoryId）
                    .advisors(a -> a.param(AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY, memoryId));
            // 2. 分支处理：给同一个模型穿上不同的“角色衣服”
            if ("service".equalsIgnoreCase(agentType)) {
                // 🤖 【智能客服逻辑】: 支持全局检索或单文档精读
                SearchRequest searchRequest = SearchRequest.builder()
                        .topK(3)
                        .similarityThreshold(0.7)
                        .build();

                // 🌟 修复：放宽专属文档的检索条件，确保大模型能拿到文件内容
                if (StringUtils.hasText(docId)) {
                    searchRequest = SearchRequest.builder()
                            .topK(5) // 放宽到 5 条，给足大模型上下文
                            // 取消 similarityThreshold 限制，防止因匹配度稍微不够导致回答"不知道"
                            .filterExpression("docId == '" + docId + "'") // 严格限定只搜该文档
                            .build();
                }

                clientRequestSpec = clientRequestSpec
                        .system(ModelConstant.SERVICE_SYSTEM_PROMPT)
                        .advisors(new QuestionAnswerAdvisor(vectorStore, searchRequest));

            } else if ("girlfriend".equalsIgnoreCase(agentType)) {
                // 👩‍🦰 【女友助手逻辑】: 从数据库拉取动态配置，使用她自己的全局专属记忆
                Map<String, String> configMap = sysConfigService.list().stream()
                        .collect(Collectors.toMap(SysConfig::getConfigKey, SysConfig::getConfigValue));

                String dynamicPrompt = configMap.getOrDefault("ai_system_prompt", ModelConstant.GirlFriend_SYSTEM_PROMPT);
                double dynamicTemp = Double.parseDouble(configMap.getOrDefault("ai_temperature", "0.7"));
                int dynamicTopK = Integer.parseInt(configMap.getOrDefault("ai_top_k", "3"));

                clientRequestSpec = clientRequestSpec
                        .system(dynamicPrompt)
                        // 女友固定查全局记忆，不掺和 docId 的事
                        .advisors(new QuestionAnswerAdvisor(vectorStore,
                                SearchRequest.builder()
                                        .topK(dynamicTopK)
                                        .similarityThreshold(0.7)
                                        .build()))
                        .options(ZhiPuAiChatOptions.builder()
                                .temperature(dynamicTemp)
                                .build());
            }

            // 3. 执行流式返回
            return clientRequestSpec.stream().content()
                    .onErrorResume(WebClientResponseException.TooManyRequests.class, e -> {
                        log.warn("触发大模型 429 流控限制，用户：{}", userName);
                        return Flux.just("\n\n[系统提示]：机甲大脑正在高速运转中，当前排队请求过多，请停顿 5 到 10 秒后再试哦！😅");
                    })
                    .onErrorResume(Exception.class, e -> {
                        log.error("大模型流处理发生未知异常", e);
                        return Flux.just("\n\n[系统提示]：大模型神经元连接暂时中断，请稍后再试。");
                    });

        } catch (Exception e) {
            log.error("构建大模型请求时发生异常，用户：{}，消息：{}", userName, message, e);
            throw new ServiceException("机甲大脑装配失败", 400);
        }
    }

    @Override
    public List<Message> getHistory(String sessionId, String agentType) {
        String memoryId = getIsolatedMemoryId(agentType, sessionId);
        return chatMemory.get(memoryId, 100);
    }

    @Override
    public List<Chat> getSessionList(String userId, String agentType) {
        // 先查出该用户所有的会话
        List<Chat> allSessions = chatMapper.selectByUserId(userId);

        // 💡 核心魔法：在 Java 层利用 Stream 过滤！
        return allSessions.stream()
                .filter(chat -> {
                    // 客服就严格只查 service_ 开头的
                    return chat.getId().startsWith(agentType + "_");
                })
                .toList();
    }

    @Override
    public Chat createSession(String userId, String title, String agentType) {
        // 💡 核心魔法：把身份标识拼装到原生的 sessionId 里！
        String prefix = StringUtils.hasText(agentType) ? agentType : "girlfriend";
        String sessionId = prefix + "_session_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);

        chatMapper.insertSession(sessionId, userId, title);
        return new Chat(sessionId, userId, title, LocalDate.now());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSession(String sessionId) {
        log.info("执行删除会话操作: {}", sessionId);
        chatMapper.deleteById(sessionId);

        // 💡 同步清理所有相关智能体的记忆
        chatMemory.clear(getIsolatedMemoryId("girlfriend", sessionId));
        chatMemory.clear(getIsolatedMemoryId("service", sessionId));
    }
}