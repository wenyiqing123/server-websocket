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
    // ==================== Redis Key 统一定义 ====================
    // Hash: 记录每个标签出现的总次数 N(A)
    private static final String KEY_TAG_TOTAL = "CF:TAG_TOTAL";

    // Hash: 记录标签对应的“真实原话”，用于最后返回给前端展示
    private static final String KEY_TAG_REP = "CF:TAG_REP";

    // List: 记录某个用户最近问过的 5 个标签，做上下文共现分析
    private static final String KEY_USER_RECENT = "CF:USER_RECENT:";

    // Hash: 记录两个标签同时出现的共现次数 N(A ∩ B)
    private static final String KEY_CO_OCCUR = "CF:CO_OCCUR:";

    // ZSet: 推荐排行榜，利用 Redis 的有序集合根据相似度自动排序
    private static final String KEY_REC_BOARD = "CF:REC_BOARD:";
    // ==========================================================

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private QwenTagExtractor qwenTagExtractor;             // 你刚写好的本地 Ollama 提取器
    @Autowired
    private MessageIntentionMapper messageIntentionMapper; // 意图表的数据访问层

    // Redis Key 统一定义省略... (和之前保持一致)

    /**
     * 异步流水线：提取标签 -> 落盘记录 -> 更新 Redis 协同矩阵
     * 这个方法会在后台静默运行，完全不影响用户聊天的流畅度
     */
    @Async("taskExecutor")
    public void processRecommendationPipelineAsync(int userId, String originalPrompt) {
        try {
            log.info("🚀 开始执行异步推荐打标流水线, 收到用户原话: {}", originalPrompt);

            // 1. 极速调取本地 Qwen2.5:3b 提取意图标签
            String currentTag = qwenTagExtractor.extractTag(originalPrompt);
            // 如果提取失败或为空，直接跳过计算，防止污染数据
            if (currentTag == null || currentTag.trim().isEmpty() || "空提问".equals(currentTag)|| "无相关名词".equals(currentTag)|| "无适用名词".equals(currentTag)) {
                log.warn("标签提取无效或为兜底通用词，跳过本次矩阵更新。");
                return;
            }
            List<MessageIntention> messageIntentions= messageIntentionMapper.selectByTagNameAndOriginalPrompt(currentTag,originalPrompt);
            if (messageIntentions.size()==0){
                // 2. 将原始问题和提取出的标签落盘到 MySQL (持久化底座)
                MessageIntention intention = new MessageIntention();
                intention.setUserId(userId);
                intention.setTagName(currentTag);
                intention.setOriginalPrompt(originalPrompt);
                intention.setCreateTime(LocalDateTime.now());
                messageIntentionMapper.insert(intention);

                // 3. 触发核心：Redis 实时增量更新！(复用咱们之前写好的这段算法逻辑)
                updateRealTimeMatrix(userId, originalPrompt, currentTag);
                log.info("✅ 异步推荐流水线执行完毕，标签 [{}] 已融入全局协同网络！", currentTag);
            }else {
                log.info("关键词意图已经存在且存在重复问题");
            }




        } catch (Exception e) {
            log.error("❌ 异步推荐流水线执行异常", e);
        }
    }

    /**
     * 核心算法：在 Redis 中进行 O(1) 复杂度的增量相似度计算
     * (这里就是你之前写好的那个方法，为了结构清晰，我把它单独抽出来了，不需要加 @Async，因为调用它的外层已经是异步了)
     */
    private void updateRealTimeMatrix(int userId, String originalPrompt, String currentTag) {
        try {
            // 1. 更新当前标签的总热度 N(A)
            redisTemplate.opsForHash().increment(KEY_TAG_TOTAL, currentTag, 1);
            // 保存这句原话，用于前端展示兜底
            redisTemplate.opsForHash().put(KEY_TAG_REP, currentTag, originalPrompt);

            // 2. 获取该用户最近问过的历史标签（上下文）
            String userHistoryKey = KEY_USER_RECENT + userId;
            List<Object> recentTags = redisTemplate.opsForList().range(userHistoryKey, 0, -1);

            // 3. 核心计算：更新共现次数与相似度
            if (recentTags != null && !recentTags.isEmpty()) {
                for (Object obj : recentTags) {
                    String histTag = obj.toString();
                    if (histTag.equals(currentTag)) continue;

                    // 为了防止 A-B 和 B-A 存两份，我们按字母顺序排列拼接 Key (无向图思维)
                    String tag1 = currentTag.compareTo(histTag) < 0 ? currentTag : histTag;
                    String tag2 = currentTag.compareTo(histTag) < 0 ? histTag : currentTag;

                    String coKey = KEY_CO_OCCUR + tag1;

                    // 增加共现次数 N(A ∩ B)
                    long coCount = redisTemplate.opsForHash().increment(coKey, tag2, 1);

                    // 获取各自的总热度
                    long nA = Long.parseLong(redisTemplate.opsForHash().get(KEY_TAG_TOTAL, tag1).toString());
                    long nB = Long.parseLong(redisTemplate.opsForHash().get(KEY_TAG_TOTAL, tag2).toString());

                    // 重新套入余弦相似度公式计算
                    double similarity = coCount / Math.sqrt(nA * nB);

                    // 瞬间更新双向的 ZSet 排行榜
                    redisTemplate.opsForZSet().add(KEY_REC_BOARD + tag1, tag2, similarity);
                    redisTemplate.opsForZSet().add(KEY_REC_BOARD + tag2, tag1, similarity);
                }
            }

            // 4. 将当前标签加入用户历史记录，并修剪只保留最近的 5 个
            redisTemplate.opsForList().leftPush(userHistoryKey, currentTag);
            redisTemplate.opsForList().trim(userHistoryKey, 0, 4);

            // 🌟 就是这句极其关键的日志，刚才丢了！
            log.info("🔥 实时协同过滤更新完毕，触发标签: {}", currentTag);

        } catch (Exception e) {
            log.error("❌ 实时推荐矩阵更新失败: ", e);
        }
    }

    /**
     * O(1) 极速获取推荐原话
     */
    public List<String> getRecommendations(String currentTag) {
        List<String> resultPrompts = new ArrayList<>();
        // 从 ZSet 中取出相似度最高的 Top 3 标签
        Set<Object> topTags = redisTemplate.opsForZSet().reverseRange(KEY_REC_BOARD + currentTag, 0, 2);

        if (topTags != null && !topTags.isEmpty()) {
            for (Object tag : topTags) {
                // 将标签转换为“真实原话”
                Object prompt = redisTemplate.opsForHash().get(KEY_TAG_REP, tag.toString());
                if (prompt != null) {
                    resultPrompts.add(prompt.toString());
                }
            }
        }

        // 兜底策略
        if (resultPrompts.isEmpty()) {
            resultPrompts.add("你能帮我写一段 Java 代码吗？");
            resultPrompts.add("什么是 Spring Boot？");
        }
        return resultPrompts;
    }
    /**
     * 缓存冷启动预热：将 MySQL 历史意图回放到 Redis 矩阵中
     */
    public void rebuildCacheIfEmpty() {
        try {
            // 1. 判断 Redis 里是否已经有数据了，如果有，说明无需预热
            Boolean hasKey = redisTemplate.hasKey(KEY_TAG_TOTAL);
            if (Boolean.TRUE.equals(hasKey)) {
                log.info("Redis中已存在推荐协同矩阵，跳过冷启动预热。");
                return;
            }

            log.info("检测到Redis推荐矩阵为空，开始从 MySQL 执行冷启动数据预热");

            // 2. 从数据库按时间线捞出所有历史记录
            List<MessageIntention> historyIntentions = messageIntentionMapper.selectAllOrderByTimeAsc();

            if (historyIntentions == null || historyIntentions.isEmpty()) {
                log.info("📉 MySQL 中暂无历史意图数据，无需预热。");
                return;
            }

            // 3. 事件回放：循环将历史数据喂给底层的矩阵重算方法
            for (MessageIntention intention : historyIntentions) {
                // 注意：这里不需要加 @Async，因为是在服务器启动阶段单线程顺序拉起
                // 直接调用咱们之前写好的核心算法逻辑
                updateRealTimeMatrix(
                        intention.getUserId(),
                        intention.getOriginalPrompt(),
                        intention.getTagName()
                );
            }

            log.info("✅ 推荐矩阵冷启动预热完毕！成功加载并重算 {} 条历史数据。", historyIntentions.size());

        } catch (Exception e) {
            log.error("❌ 推荐矩阵冷启动预热失败", e);
        }
    }
}