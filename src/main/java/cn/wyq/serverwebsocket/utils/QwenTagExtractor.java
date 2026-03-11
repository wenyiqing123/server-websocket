package cn.wyq.serverwebsocket.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class QwenTagExtractor {

    // 注入 RestTemplate 发起 HTTP 请求
    @Autowired
    private RestTemplate restTemplate;

    // Ollama 默认的本地生成接口
    private static final String OLLAMA_API_URL = "http://127.0.0.1:11434/api/generate";

    /**
     * 将用户输入的原话提取为 1 个核心标签
     */
    public String extractTag(String originalPrompt) {
        try {
            // 1. 精心设计的“紧箍咒”提示词，强迫模型只输出标签
            String systemPrompt = "你是一个极其严格的意图提取器。请提取出用户提问中最核心的技术名词或业务意图作为标签。\n" +
                    "严格遵守以下3条规则：\n" +
                    "1. 只能输出一个词组。\n" +
                    "2. 长度绝对不能超过8个字符。\n" +
                    "3. 绝对不要输出任何标点符号、解释说明或分析过程。\n" +
                    "用户的提问是：[" + originalPrompt + "]";

            // 2. 组装 Ollama 请求载荷
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "qwen2.5:3b");
            requestBody.put("prompt", systemPrompt);
            requestBody.put("stream", false); // 关闭流式输出，我们需要一次性拿到完整结果

            // 设置极低的温度值，保证每次提取的结果极其稳定，不做发散性创造
            Map<String, Object> options = new HashMap<>();
            options.put("temperature", 0.1);
            requestBody.put("options", options);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

            // 3. 发送请求给本地 Ollama
            long startTime = System.currentTimeMillis();
            ResponseEntity<Map> response = restTemplate.postForEntity(OLLAMA_API_URL, requestEntity, Map.class);
            long costTime = System.currentTimeMillis() - startTime;

            // 4. 解析结果
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String tag = (String) response.getBody().get("response");
                // 再次清洗，去掉可能残留的空格、换行或标点
                tag = tag.replaceAll("[\\p{Punct}\\s]", "").trim();

                log.info("🎯 Ollama 提取标签成功: 原话[{}] -> 标签[{}], 耗时: {}ms", originalPrompt, tag, costTime);

                if (tag.length() > 0 && tag.length() <= 10) {
                    return tag;
                }
            }
        } catch (Exception e) {
            log.error("❌ 调用本地 Ollama 提取标签失败，原话: {}", originalPrompt, e);
        }

        // 5. 极客兜底策略：如果大模型罢工了，返回一个默认标签，保证程序不崩溃
        return "通用问题";
    }
}