package cn.wyq.serverwebsocket.service;

import cn.wyq.serverwebsocket.mapper.MessageIntentionMapper;
import cn.wyq.serverwebsocket.pojo.entity.MessageIntention;
import cn.wyq.serverwebsocket.utils.QwenTagExtractor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class RealTimeRecommendService {

    // ==================== Redis Key 统一定义 (大幅精简) ====================
    // Set: 我们只需要一个数据结构！记录某个标签下对应的所有真实提问
    // 例如: CB:TAG_PROMPTS:springboot 里存放 ["什么是springboot", "springboot怎么配置"]
    private static final String KEY_TAG_PROMPTS = "CB:TAG_PROMPTS:";
    // ==========================================================

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private QwenTagExtractor qwenTagExtractor;
    @Autowired
    private MessageIntentionMapper messageIntentionMapper;

    /**
     * 异步流水线：提取标签 -> 落盘记录 -> 将问题归类到 Redis 的标签池
     */
    @Async("taskExecutor")
    public void processRecommendationPipelineAsync(int userId, String originalPrompt) {
        try {
            log.info("🚀 开始执行异步推荐归类, 收到用户原话: {}", originalPrompt);

            // 1. 提取意图标签
            String currentTag = qwenTagExtractor.extractTag(originalPrompt);
            if (currentTag == null || currentTag.trim().isEmpty() || "空提问".equals(currentTag) || "无相关名词".equals(currentTag) || "无适用名词".equals(currentTag)) {
                return;
            }

            // 2. MySQL 落盘防重机制 (保留你原有的优秀逻辑)
            List<MessageIntention> messageIntentions = messageIntentionMapper.selectByTagNameAndOriginalPrompt(currentTag, originalPrompt);
            if (messageIntentions.isEmpty()||messageIntentions==null||messageIntentions.size()==0) {
                MessageIntention intention = new MessageIntention();
                intention.setUserId(userId);
                intention.setTagName(currentTag);
                intention.setOriginalPrompt(originalPrompt);
                intention.setCreateTime(LocalDateTime.now());
                messageIntentionMapper.insert(intention);
            }

            // 3. 🚨 核心重构：彻底抛弃上下文共现矩阵！
            // 直接把这句原话，塞进属于这个标签的 Redis Set 集合中（Set自带去重功能）
            redisTemplate.opsForSet().add(KEY_TAG_PROMPTS + currentTag, originalPrompt);
            log.info("✅ 问答已归类，标签 [{}] 的同类题库已扩充！", currentTag);

        } catch (Exception e) {
            log.error("❌ 异步推荐归类异常", e);
        }
    }

    /**
     * O(1) 极速获取同类问题推荐
     */
    /**
     * O(1) 极速获取同类问题推荐
     */
    public List<String> getRecommendations(String currentTag) {
        List<String> resultPrompts = new ArrayList<>();

        if (currentTag != null && !currentTag.trim().isEmpty()) {
            // 🚨 终极修复：改用 distinctRandomMembers！
            // 这个方法对应 Redis 的正数 count，保证绝对不重复。返回类型也会变成 Set。
            Set<Object> randomPrompts = redisTemplate.opsForSet().distinctRandomMembers(KEY_TAG_PROMPTS + currentTag, 3);

            if (randomPrompts != null) {
                for (Object prompt : randomPrompts) {
                    resultPrompts.add(prompt.toString());
                }
            }
        }

        // 兜底策略
        if (resultPrompts.isEmpty()) {
            resultPrompts.add("你能帮我写一段 Java 代码吗？");
            resultPrompts.add("讲一下 Spring Boot 的核心原理");
        }
        return resultPrompts;
    }

    /**
     * 缓存冷启动预热：将 MySQL 历史数据按标签塞入 Redis
     */
    public void rebuildCacheIfEmpty() {
        try {
            // 简单判断一下是否需要预热 (找个常用的 tag 判断即可，或者判断库里是否有数据)
            // 为了安全，建议每次重启都清空前缀为 CB:TAG_PROMPTS:* 的 key 再预热

            List<MessageIntention> historyIntentions = messageIntentionMapper.selectAllOrderByTimeAsc();
            if (historyIntentions == null || historyIntentions.isEmpty()) {
                return;
            }

            for (MessageIntention intention : historyIntentions) {
                // 直接将历史数据丢进对应的 Set 池子里
                redisTemplate.opsForSet().add(KEY_TAG_PROMPTS + intention.getTagName(), intention.getOriginalPrompt());
            }
            log.info("✅ 内容推荐题库预热完毕！成功加载 {} 条历史数据。", historyIntentions.size());

        } catch (Exception e) {
            log.error("❌ 题库预热失败", e);
        }
    }
}