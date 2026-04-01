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
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.zhipuai.ZhiPuAiChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
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

    private final ChatClient girlfriendClient;
    private final ChatClient serviceClient; // 💡 改名为 serviceClient
    private final ChatMemory chatMemory;
    private final ChatMapper chatMapper;
    private final VectorStore vectorStore;

    private SysConfigService sysConfigService;

    public ChatServiceImpl(
            @Qualifier("girlfriendChatClient") ChatClient girlfriendClient,
            @Qualifier("serviceChatClient") ChatClient serviceClient, // 💡 注入 serviceChatClient
            ChatMemory chatMemory,
            ChatMapper chatMapper, VectorStore vectorStore, SysConfigService sysConfigService) {
        this.girlfriendClient = girlfriendClient;
        this.serviceClient = serviceClient;
        this.chatMemory = chatMemory;
        this.chatMapper = chatMapper;
        this.vectorStore = vectorStore;
        this.sysConfigService = sysConfigService;
    }

    /**
     * 生成隔离的专属记忆 ID
     */
    private String getIsolatedMemoryId(String agentType, String sessionId) {
        String safeAgentType = StringUtils.hasText(agentType) ? agentType : "girlfriend";
        // 现在的格式会是 "service:session_xxx" 或 "girlfriend:session_xxx"
        return safeAgentType + ":" + sessionId;
    }

    @Override
    public Flux<String> chatStream(String message, String sessionId, String userName, String agentType) {
        try {
            // 1. 动态选择 Client
            ChatClient activeClient = "service".equalsIgnoreCase(agentType) ? serviceClient : girlfriendClient;

            // 2. 获取隔离的对话记忆 ID
            String memoryId = getIsolatedMemoryId(agentType, sessionId);

            // 3. 构建初始请求 (挂载基础参数和记忆顾问)
            var clientRequestSpec = activeClient.prompt()
                    .user(message)
                    .advisors(a -> a.param(AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY, memoryId));

            // 4. 🚀 分支处理：按角色动态挂载专属配置
            if ("service".equalsIgnoreCase(agentType)) {
                // 🤖 【智能客服逻辑】: 使用纯常量配置，挂载 RAG
                clientRequestSpec = clientRequestSpec
                        .system(ModelConstant.SERVICE_SYSTEM_PROMPT) // 注入常量提示词 (你需要确保类里有这个常量)
                        .advisors(
                                // 挂载知识库
                                QuestionAnswerAdvisor.builder(vectorStore)
                                        .searchRequest(
                                                SearchRequest.builder()
                                                        .topK(3)
                                                        .similarityThreshold(0.7)
                                                        .build()
                                        )
                                        .build()
                        );

            } else if ("girlfriend".equalsIgnoreCase(agentType)) {
                // 👩‍🦰 【女友助手逻辑】: 从数据库拉取动态配置
                Map<String, String> configMap = sysConfigService.list().stream()
                        .collect(Collectors.toMap(SysConfig::getConfigKey, SysConfig::getConfigValue));

                // 1. 解析配置并赋予安全默认值
                String dynamicPrompt = configMap.getOrDefault("ai_system_prompt", ModelConstant.GirlFriend_SYSTEM_PROMPT);
                double dynamicTemp = Double.parseDouble(configMap.getOrDefault("ai_temperature", "0.7"));

                // 🚀 2. 新增：解析动态的 RAG 检索量 (兜底默认值为 3)
                int dynamicTopK = Integer.parseInt(configMap.getOrDefault("ai_top_k", "3"));

                // 3. 将动态提示词、温度和 RAG 检索器挂载到请求中
                clientRequestSpec = clientRequestSpec
                        .system(dynamicPrompt)
                        // 🚀 4. 新增：在这里注入动态的 topK 检索请求
                        // 注意：这里需要确保你的 Service 类里已经通过 @Autowired 注入了 VectorStore
                        .advisors(new QuestionAnswerAdvisor(vectorStore,
                                SearchRequest.builder()
                                        .topK(dynamicTopK)           // 使用从数据库读出的动态检索量
                                        .similarityThreshold(0.7)    // 相似度阈值可以写死，也可以提成配置
                                        .build()))
                        .options(ZhiPuAiChatOptions.builder()
                                .temperature(dynamicTemp)
                                .build());
            }

            // 5. 执行并返回
            return clientRequestSpec.stream().content()
                    .onErrorResume(WebClientResponseException.TooManyRequests.class, e -> {
                        log.warn("触发大模型 429 流控限制，用户：{}", userName);
                        return Flux.just("\n\n[系统提示]：机甲大脑正在高速运转中，当前排队请求过多（触发API限流），请停顿 5 到 10 秒后再试哦！😅");
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