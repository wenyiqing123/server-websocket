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
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ChatServiceImpl implements ChatService {

    // 🌟 注入两个在配置类里捏好的不同机甲
    private final ChatClient girlfriendClient;
    private final ChatClient serviceClient;

    private final ChatMemory chatMemory;
    private final ChatMapper chatMapper;
    private final VectorStore vectorStore;
    private final SysConfigService sysConfigService;

    // 🌟 正确：加上 ${} 占位符，并建议提供一个默认值（以防配置文件没写）
    @Value("${spring.ai.openai.chat.options.model:qwen-plus}")
    private String model;

    public ChatServiceImpl(
            @Qualifier("girlfriendChatClient") ChatClient girlfriendClient,
            @Qualifier("serviceChatClient") ChatClient serviceClient,
            ChatMemory chatMemory,
            ChatMapper chatMapper,
            VectorStore vectorStore,
            SysConfigService sysConfigService) {
        this.girlfriendClient = girlfriendClient;
        this.serviceClient = serviceClient;
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
            ChatClient activeClient = "service".equalsIgnoreCase(agentType) ? serviceClient : girlfriendClient;

            var promptSpec = activeClient.prompt()
                    .user(message)
                    .advisors(a -> a.param(AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY, memoryId));

            if ("service".equalsIgnoreCase(agentType)) {
                log.info("【意图识别】非系统查询，挂载知识库 RAG 增强");
                SearchRequest searchRequest = SearchRequest.builder()
                        .topK(3)
                        .similarityThreshold(0.92)
                        .build();
                if (StringUtils.hasText(docId)) {
                    searchRequest = SearchRequest.builder()
                            .topK(5)
                            .filterExpression("docId == '" + docId + "'")
                            .build();
                }
                promptSpec.advisors(new QuestionAnswerAdvisor(vectorStore, searchRequest));
            } else if ("girlfriend".equalsIgnoreCase(agentType)) {
                // 👩‍🦰 女友模式：使用正确的 OpenAiChatOptions 设置温度
                Map<String, String> configMap = sysConfigService.list().stream()
                        .collect(Collectors.toMap(SysConfig::getConfigKey, SysConfig::getConfigValue));
                String dynamicPrompt = configMap.getOrDefault("ai_system_prompt", ModelConstant.GirlFriend_SYSTEM_PROMPT);
                double dynamicTemp = Double.parseDouble(configMap.getOrDefault("ai_temperature", "0.7"));

                promptSpec.system(dynamicPrompt)
                        .options(OpenAiChatOptions.builder()
                                .model(model) // 也可以从配置读取
                                .temperature(dynamicTemp)

                                .build());
            }

            return promptSpec.stream().content()
                    .onErrorResume(Exception.class, e -> {
                        log.error("流处理异常", e);
                        return Flux.just("\n\n[系统提示]：机甲大脑神经元连接中断，请稍后再试。");
                    });

        } catch (Exception e) {
            log.error("装配失败", e);
            throw new ServiceException("机甲大脑装配失败", 400);
        }
    }

    @Override
    public List<Message> getHistory(String sessionId, String agentType) {
        String memoryId = getIsolatedMemoryId(agentType, sessionId);

        // 1. 获取最新 100 条消息
        List<Message> messages = chatMemory.get(memoryId, 100);

        // 2. 包装成可变的 ArrayList，防止修改时报错
        List<Message> modifiableList = new ArrayList<>(messages);

        // 3. 反转列表顺序 (变成按时间升序)
        Collections.reverse(modifiableList);

        // 4. 返回反转后的列表
        return modifiableList;
    }

    @Override
    public List<Chat> getSessionList(String userId, String agentType) {
        List<Chat> allSessions = chatMapper.selectByUserId(userId);
        return allSessions.stream()
                .filter(chat -> chat.getId().startsWith(agentType + "_"))
                .toList();
    }

    @Override
    public Chat createSession(String userId, String title, String agentType) {
        String prefix = StringUtils.hasText(agentType) ? agentType : "girlfriend";
        String sessionId = prefix + "_session_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        chatMapper.insertSession(sessionId, userId, title);
        return new Chat(sessionId, userId, title, LocalDate.now());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSession(String sessionId) {
        chatMapper.deleteById(sessionId);
        chatMemory.clear(getIsolatedMemoryId("girlfriend", sessionId));
        chatMemory.clear(getIsolatedMemoryId("service", sessionId));
    }
}