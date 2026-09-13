package cn.wyq.serverwebsocket.service.impl;

import cn.wyq.serverwebsocket.mapper.AiServiceQuestionMapper;
import cn.wyq.serverwebsocket.pojo.entity.AiServiceQuestion;
import cn.wyq.serverwebsocket.pojo.vo.AiServiceQuestionSuggestionVO;
import cn.wyq.serverwebsocket.service.ServiceQuestionSuggestionService;
import cn.wyq.serverwebsocket.service.SysConfigService;
import cn.wyq.serverwebsocket.utils.SystemConfigUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 智能客服问题题库实现。
 *
 * <p>输入联想只遍历启动时加载的内存题库，不在用户键入过程中调用大模型；
 * 数据库只负责问题持久化和启动预热。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceQuestionSuggestionServiceImpl implements ServiceQuestionSuggestionService {

    /** 防止异常大的题库占用过多应用内存。 */
    private static final int MAX_CACHE_SIZE = 20_000;
    private static final int MAX_LIMIT = 5;
    private static final int MIN_QUERY_LENGTH = 2;
    private static final int MAX_COMPARISON_LENGTH = 512;
    private static final double MIN_SIMILARITY = 0.38D;
    private static final long WARM_UP_RETRY_MILLIS = 30_000L;

    private final AiServiceQuestionMapper questionMapper;
    private final SysConfigService sysConfigService;
    private final ConcurrentMap<String, AiServiceQuestion> questionCache = new ConcurrentHashMap<>();
    /** 预热阶段构建的字符特征，避免每次键入都为每条候选重新分词。 */
    private final ConcurrentMap<String, QuestionFeatures> questionFeatureCache = new ConcurrentHashMap<>();
    private final Object warmUpMonitor = new Object();
    private volatile boolean warmed;
    private volatile long nextWarmUpAttemptAt;

    @Override
    public void warmUp() {
        long now = System.currentTimeMillis();
        if (warmed || now < nextWarmUpAttemptAt) {
            return;
        }
        synchronized (warmUpMonitor) {
            now = System.currentTimeMillis();
            if (warmed || now < nextWarmUpAttemptAt) {
                return;
            }
            boolean loadedFromDatabase = false;
            try {
                List<AiServiceQuestion> questions = questionMapper.selectForWarmup(MAX_CACHE_SIZE);
                if (questions != null) {
                    for (AiServiceQuestion question : questions) {
                        if (question == null || !StringUtils.hasText(question.getQuestionHash())
                                || !StringUtils.hasText(question.getQuestion())
                                || !StringUtils.hasText(question.getNormalizedQuestion())) {
                            continue;
                        }
                        questionCache.put(question.getQuestionHash(), question);
                        questionFeatureCache.put(question.getQuestionHash(),
                                new QuestionFeatures(limitForComparison(question.getNormalizedQuestion())));
                    }
                }
                loadedFromDatabase = true;
            } catch (Exception e) {
                // 新题库首次部署前可能尚未执行迁移，继续尝试读取旧客服记忆。
                log.warn("新智能客服题库读取失败，将尝试兼容旧版聊天记忆", e);
            }
            try {
                int legacyLimit = Math.max(0, MAX_CACHE_SIZE - questionCache.size());
                if (legacyLimit > 0) {
                    List<String> legacyQuestions = questionMapper.selectHistoricalServiceQuestions(legacyLimit);
                    if (legacyQuestions != null) {
                        for (String legacyQuestion : legacyQuestions) {
                            addLegacyQuestion(legacyQuestion);
                        }
                    }
                }
                loadedFromDatabase = true;
            } catch (Exception e) {
                log.warn("旧版客服聊天记忆读取失败", e);
            }
            if (!loadedFromDatabase) {
                warmed = false;
                nextWarmUpAttemptAt = System.currentTimeMillis() + WARM_UP_RETRY_MILLIS;
                throw new IllegalStateException("智能客服问题题库和旧版聊天记忆均不可用");
            }
            if (questionCache.size() > MAX_CACHE_SIZE) {
                while (questionCache.size() > MAX_CACHE_SIZE) {
                    removeLeastUsedQuestion();
                }
            }
            warmed = true;
            nextWarmUpAttemptAt = 0L;
            if (questionCache.isEmpty()) {
                log.info("智能客服问题题库预热完成，当前没有可用问题");
            } else {
                log.info("智能客服问题题库预热完成，加载 {} 条记录", questionCache.size());
            }
        }
    }

    @Override
    public void recordQuestion(Integer userId, String question) {
        if (!StringUtils.hasText(question)) {
            return;
        }
        String original = question.trim();
        String normalized = normalize(original);
        if (!StringUtils.hasText(normalized)) {
            return;
        }
        String hash = sha256(normalized);
        List<AiServiceQuestionSuggestionVO> matches = suggest(original, 1);
        try {
            questionMapper.upsert(hash, original, normalized, userId);
            updateCache(hash, original, normalized, userId);
        } catch (Exception e) {
            // 题库故障不能影响客服主链路或模型流式响应。
            log.warn("客服问题持久化失败，hash={}", hash, e);
        }
        try {
            AiServiceQuestionSuggestionVO match = matches.isEmpty() ? null : matches.get(0);
            questionMapper.insertQuestionLog(userId, original,
                    match == null ? null : match.getQuestion(),
                    match == null ? null : match.getSimilarity(), match != null);
        } catch (Exception e) {
            // 质量日志是旁路能力，迁移未执行或写入失败都不能中断客服回答。
            log.warn("客服问题命中日志写入失败", e);
        }
    }

    @Override
    public List<AiServiceQuestionSuggestionVO> suggest(String query, int limit) {
        if (!StringUtils.hasText(query)) {
            return List.of();
        }
        String normalizedQuery = normalize(query.trim());
        if (normalizedQuery.length() < MIN_QUERY_LENGTH) {
            return List.of();
        }
        if (!warmed) {
            try {
                warmUp();
            } catch (Exception e) {
                return List.of();
            }
        }

        Map<String, String> settings;
        try {
            settings = sysConfigService.getConfigMap();
        } catch (RuntimeException exception) {
            settings = Map.of();
        }
        if (!parseBoolean(settings.get("service_question_suggestion_enabled"), true)) {
            return List.of();
        }
        int configuredLimit = SystemConfigUtils.intValue(settings,
                "service_question_max_results", MAX_LIMIT, 1, 10);
        double configuredSimilarity = SystemConfigUtils.doubleValue(settings,
                "service_question_similarity_threshold", MIN_SIMILARITY, 0D, 1D);
        int boundedLimit = Math.max(1, Math.min(configuredLimit, limit));
        String comparisonQuery = limitForComparison(normalizedQuery);
        QuestionFeatures queryFeatures = new QuestionFeatures(comparisonQuery);
        List<AiServiceQuestionSuggestionVO> suggestions = new ArrayList<>();
        for (AiServiceQuestion candidate : questionCache.values()) {
            if (candidate == null || !StringUtils.hasText(candidate.getQuestion())
                    || !StringUtils.hasText(candidate.getNormalizedQuestion())) {
                continue;
            }
            QuestionFeatures candidateFeatures = questionFeatureCache.computeIfAbsent(
                    candidate.getQuestionHash(),
                    key -> new QuestionFeatures(limitForComparison(candidate.getNormalizedQuestion())));
            double similarity = similarity(comparisonQuery,
                    limitForComparison(candidate.getNormalizedQuestion()), queryFeatures, candidateFeatures);
            if (similarity >= configuredSimilarity) {
                suggestions.add(AiServiceQuestionSuggestionVO.builder()
                        .question(candidate.getQuestion())
                        .similarity(roundSimilarity(similarity))
                        .useCount(candidate.getUseCount() == null ? 0 : candidate.getUseCount())
                        .build());
            }
        }
        suggestions.sort(Comparator
                .comparingDouble((AiServiceQuestionSuggestionVO item) ->
                        item.getSimilarity() == null ? 0D : item.getSimilarity()).reversed()
                .thenComparing(Comparator.comparingInt((AiServiceQuestionSuggestionVO item) ->
                        item.getUseCount() == null ? 0 : item.getUseCount()).reversed())
                .thenComparing(AiServiceQuestionSuggestionVO::getQuestion));
        return suggestions.size() <= boundedLimit
                ? suggestions
                : new ArrayList<>(suggestions.subList(0, boundedLimit));
    }

    @Override
    public void refreshCache() {
        synchronized (warmUpMonitor) {
            questionCache.clear();
            questionFeatureCache.clear();
            warmed = false;
            nextWarmUpAttemptAt = 0L;
        }
        warmUp();
    }

    private boolean parseBoolean(String value, boolean fallback) {
        if (!StringUtils.hasText(value)) {
            return fallback;
        }
        return "true".equalsIgnoreCase(value) || "1".equals(value);
    }

    private void updateCache(String hash, String original, String normalized, Integer userId) {
        questionCache.compute(hash, (key, existing) -> {
            LocalDateTime now = LocalDateTime.now();
            if (existing == null) {
                return AiServiceQuestion.builder()
                        .questionHash(hash)
                        .question(original)
                        .normalizedQuestion(normalized)
                        .sourceType("service")
                        .useCount(1)
                        .lastUserId(userId)
                        .lastUsedAt(now)
                        .createdAt(now)
                        .updatedAt(now)
                        .build();
            }
            int currentUseCount = existing.getUseCount() == null ? 0 : existing.getUseCount();
            int nextUseCount = currentUseCount >= Integer.MAX_VALUE
                    ? Integer.MAX_VALUE : currentUseCount + 1;
            return AiServiceQuestion.builder()
                    .id(existing.getId())
                    .questionHash(existing.getQuestionHash())
                    .question(existing.getQuestion())
                    .normalizedQuestion(existing.getNormalizedQuestion())
                    .sourceType(existing.getSourceType())
                    .useCount(Math.max(1, nextUseCount))
                    .lastUserId(userId)
                    .lastUsedAt(now)
                    .createdAt(existing.getCreatedAt())
                    .updatedAt(now)
                    .build();
        });
        questionFeatureCache.put(hash, new QuestionFeatures(limitForComparison(normalized)));
        // 新记录在预热前写入缓存时仍需保留数据库作为权威来源。
        if (questionCache.size() > MAX_CACHE_SIZE) {
            removeLeastUsedQuestion();
        }
    }

    /** 将旧版客服聊天记忆转成内存题库记录，不回写旧表或新表。 */
    private void addLegacyQuestion(String question) {
        if (!StringUtils.hasText(question)) {
            return;
        }
        String original = question.trim();
        String normalized = normalize(original);
        if (!StringUtils.hasText(normalized)) {
            return;
        }
        String hash = sha256(normalized);
        AiServiceQuestion legacy = AiServiceQuestion.builder()
                .questionHash(hash)
                .question(original)
                .normalizedQuestion(normalized)
                .sourceType("legacy-memory")
                .useCount(1)
                .build();
        questionCache.putIfAbsent(hash, legacy);
        questionFeatureCache.putIfAbsent(hash,
                new QuestionFeatures(limitForComparison(normalized)));
    }

    private void removeLeastUsedQuestion() {
        Map.Entry<String, AiServiceQuestion> leastUsed = questionCache.entrySet().stream()
                .min(Comparator.comparingInt(entry -> entry.getValue().getUseCount() == null
                        ? 0 : entry.getValue().getUseCount()))
                .orElse(null);
        if (leastUsed != null) {
            questionCache.remove(leastUsed.getKey(), leastUsed.getValue());
            questionFeatureCache.remove(leastUsed.getKey());
        }
    }

    /** 统一全角字符、大小写，并移除空白和标点，减少同一问题的重复记录。 */
    private String normalize(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT);
        StringBuilder builder = new StringBuilder(normalized.length());
        normalized.codePoints().forEach(codePoint -> {
            if (Character.isLetterOrDigit(codePoint)) {
                builder.appendCodePoint(codePoint);
            }
        });
        if (builder.length() > 0) {
            return builder.toString();
        }
        return normalized.replaceAll("\\s+", "");
    }

    private String limitForComparison(String value) {
        return value.length() <= MAX_COMPARISON_LENGTH
                ? value : value.substring(0, MAX_COMPARISON_LENGTH);
    }

    /** 组合包含关系、字符 Dice 和二元组 Dice，兼顾短中文问题与长句。 */
    private double similarity(String query, String candidate,
                              QuestionFeatures queryFeatures, QuestionFeatures candidateFeatures) {
        if (query.equals(candidate)) {
            return 1D;
        }
        if (query.length() < MIN_QUERY_LENGTH || candidate.length() < MIN_QUERY_LENGTH) {
            return 0D;
        }
        double containment = 0D;
        if (candidate.contains(query)) {
            containment = 0.72D + 0.28D * query.length() / candidate.length();
        } else if (query.contains(candidate)) {
            containment = 0.68D + 0.32D * candidate.length() / query.length();
        }
        double characterDice = dice(queryFeatures.characters, candidateFeatures.characters);
        double bigramDice = dice(queryFeatures.bigrams, candidateFeatures.bigrams);
        double qgramScore = characterDice * 0.35D + bigramDice * 0.65D;
        double editScore = (query.length() <= 128 && candidate.length() <= 128)
                ? levenshteinSimilarity(query, candidate) : 0D;
        return Math.min(1D, Math.max(containment, Math.max(qgramScore, qgramScore * 0.65D + editScore * 0.35D)));
    }

    private static Set<String> toCharacterSet(String value) {
        Set<String> result = new HashSet<>();
        value.codePoints().forEach(codePoint -> result.add(new String(Character.toChars(codePoint))));
        return result;
    }

    private static Set<String> toBigramSet(String value) {
        Set<String> result = new HashSet<>();
        for (int i = 0; i < value.length() - 1; i++) {
            result.add(value.substring(i, i + 2));
        }
        return result;
    }

    private static double dice(Set<String> left, Set<String> right) {
        if (left.isEmpty() || right.isEmpty()) {
            return 0D;
        }
        Set<String> intersection = new HashSet<>(left);
        intersection.retainAll(right);
        return 2D * intersection.size() / (left.size() + right.size());
    }

    private double levenshteinSimilarity(String left, String right) {
        int[] previous = new int[right.length() + 1];
        int[] current = new int[right.length() + 1];
        for (int j = 0; j <= right.length(); j++) {
            previous[j] = j;
        }
        for (int i = 1; i <= left.length(); i++) {
            current[0] = i;
            for (int j = 1; j <= right.length(); j++) {
                int substitution = previous[j - 1] + (left.charAt(i - 1) == right.charAt(j - 1) ? 0 : 1);
                current[j] = Math.min(Math.min(current[j - 1] + 1, previous[j] + 1), substitution);
            }
            int[] swap = previous;
            previous = current;
            current = swap;
        }
        int maxLength = Math.max(left.length(), right.length());
        return maxLength == 0 ? 1D : 1D - (double) previous[right.length()] / maxLength;
    }

    private double roundSimilarity(double similarity) {
        return Math.round(similarity * 1000D) / 1000D;
    }

    /** 预计算的字符和二元组集合。集合只读，能够被多个请求安全复用。 */
    private static final class QuestionFeatures {
        private final Set<String> characters;
        private final Set<String> bigrams;

        private QuestionFeatures(String value) {
            this.characters = Set.copyOf(toCharacterSet(value));
            this.bigrams = Set.copyOf(toBigramSet(value));
        }
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("当前 JDK 不支持 SHA-256", e);
        }
    }
}
