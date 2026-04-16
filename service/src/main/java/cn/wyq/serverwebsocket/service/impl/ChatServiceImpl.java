package cn.wyq.serverwebsocket.service.impl;

import cn.wyq.serverwebsocket.constant.ModelConstant;
import cn.wyq.serverwebsocket.exception.ServiceException;
import cn.wyq.serverwebsocket.mapper.ChatMapper;
import cn.wyq.serverwebsocket.pojo.dto.AiReplyDto;
import cn.wyq.serverwebsocket.pojo.entity.Chat;
import cn.wyq.serverwebsocket.pojo.entity.SysConfig;
import cn.wyq.serverwebsocket.service.ChatService;
import cn.wyq.serverwebsocket.service.SysConfigService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import reactor.core.publisher.Flux;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ChatServiceImpl implements ChatService {
    // 🌟 阿里云百炼平台的 OpenAI 兼容接口地址
    private static final String ALIYUN_API_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";
    // 🌟 注入两个在配置类里捏好的不同机甲
    private final ChatClient girlfriendClient;
    private final ChatClient serviceClient;
    private final ChatMemory chatMemory;
    private final ChatMapper chatMapper;
    private final VectorStore vectorStore;
    private final SysConfigService sysConfigService;
    // 🌟 填入你在阿里云百炼平台申请的 API Key
    @Value("${spring.ai.openai.api-key:sk-5fe3023e1e5643469ba7f17b32efc864}")
    private String aliyunApiKey;
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
                String finalSystemPrompt = dynamicPrompt + "\n\n" + ModelConstant.MODEL_SYSTEM_PROMPT;
                promptSpec.system(finalSystemPrompt)
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


    public String generateSuggestReply(AiReplyDto dto) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // 使用 Bearer Token 鉴权
        headers.setBearerAuth(aliyunApiKey);

        // 🧠 核心：提示词调教
        String systemPrompt = String.format(
                "你是一个极其幽默、高情商的聊天嘴替。当前用户是【%s】，正在与【%s】聊天。\n" +
                        "请根据提供的最新聊天记录上下文，生成一句合适、自然、能勾起对方聊天兴趣的回复。\n" +
                        "严格要求：\n" +
                        "1. 绝不要输出任何解释、寒暄、分析过程，直接给出你能想到的最好的一句回复。\n" +
                        "2. 语气必须像真人在微信里聊天，可以适当使用 emoji。\n" +
                        "3. 字数严格控制在 40 字以内。",
                dto.getMyUsername(), dto.getTargetName()
        );

        JSONObject requestBody = new JSONObject();
        // 🌟 指定阿里云的模型：强烈推荐使用 qwen-turbo
        requestBody.put("model", "qwen-turbo");
        requestBody.put("temperature", 0.8); // 调高一点，让回复更有创意和随机性

        JSONArray messages = new JSONArray();

        // System 角色设定
        JSONObject systemMsg = new JSONObject();
        systemMsg.put("role", "system");
        systemMsg.put("content", systemPrompt);
        messages.add(systemMsg);

        // User 输入上下文
        JSONObject userMsg = new JSONObject();
        userMsg.put("role", "user");
        userMsg.put("content", "聊天上下文如下：\n" + dto.getContext());
        messages.add(userMsg);

        requestBody.put("messages", messages);

        HttpEntity<String> entity = new HttpEntity<>(requestBody.toJSONString(), headers);

        try {
            // 发起请求
            String response = restTemplate.postForObject(ALIYUN_API_URL, entity, String.class);
            JSONObject jsonResponse = JSON.parseObject(response);

            // 解析 OpenAI 格式的返回值
            return jsonResponse.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                    .trim();
        } catch (Exception e) {
            log.error("调用阿里云百炼 API 失败: ", e);
            return null;
        }
    }
}