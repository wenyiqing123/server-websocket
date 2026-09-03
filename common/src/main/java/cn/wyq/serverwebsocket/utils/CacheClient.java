package cn.wyq.serverwebsocket.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * JSON 字符串缓存客户端，统一处理缓存穿透、击穿和雪崩。
 */
/**
 * CacheClient：通用工具类，封装可复用的辅助方法。
 */
@Component
@Slf4j
public class CacheClient {

    private static final String KEY_NAMESPACE = "cache:v2:";
    private static final String CACHE_NULL_VALUE = "__CACHE_NULL__";
    private static final String LOCK_NAMESPACE = KEY_NAMESPACE + "lock:";
    private static final int MAX_LOCK_ATTEMPTS = 40;
    private static final long LOCK_WAIT_MILLIS = 75L;
    private static final long NULL_CACHE_SECONDS = 120L;
    private static final String UNLOCK_SCRIPT_TEXT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then "
                    + "return redis.call('del', KEYS[1]) else return 0 end";
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT =
            new DefaultRedisScript<>(UNLOCK_SCRIPT_TEXT, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public CacheClient(StringRedisTemplate redisTemplate) {
        this.redisTemplate = Objects.requireNonNull(redisTemplate, "redisTemplate");
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public <R, ID> R queryWithMutex(String keyPrefix,
                                    ID id,
                                    Class<R> type,
                                    Function<ID, R> dbFallback,
                                    long ttl,
                                    TimeUnit unit) {
        Objects.requireNonNull(type, "type");
        return queryWithMutex(keyPrefix, id, objectMapper.getTypeFactory().constructType(type), dbFallback, ttl, unit);
    }

    public <R, ID> R queryWithMutex(String keyPrefix,
                                    ID id,
                                    TypeReference<R> type,
                                    Function<ID, R> dbFallback,
                                    long ttl,
                                    TimeUnit unit) {
        Objects.requireNonNull(type, "type");
        return queryWithMutex(keyPrefix, id, objectMapper.getTypeFactory().constructType(type.getType()), dbFallback, ttl, unit);
    }

    private <R, ID> R queryWithMutex(String keyPrefix,
                                     ID id,
                                     JavaType javaType,
                                     Function<ID, R> dbFallback,
                                     long ttl,
                                     TimeUnit unit) {
        validateArguments(keyPrefix, id, dbFallback, ttl, unit);
        long baseSeconds = toPositiveSeconds(ttl, unit);
        String cacheKey = buildKey(keyPrefix, id);

        CacheRead<R> firstRead = read(cacheKey, javaType);
        if (firstRead.hit()) {
            return firstRead.value();
        }

        String lockKey = LOCK_NAMESPACE + cacheKey.substring(KEY_NAMESPACE.length());
        String token = null;
        try {
            for (int attempt = 0; attempt < MAX_LOCK_ATTEMPTS; attempt++) {
                CacheRead<R> retryRead = read(cacheKey, javaType);
                if (retryRead.hit()) {
                    return retryRead.value();
                }
                token = tryLock(lockKey, lockTtlSeconds(baseSeconds));
                if (token != null) {
                    break;
                }
                sleepBeforeRetry();
            }

            if (token == null) {
                log.warn("缓存互斥锁等待超时，降级查询数据库，key={}", cacheKey);
                return dbFallback.apply(id);
            }

            CacheRead<R> lockedRead = read(cacheKey, javaType);
            if (lockedRead.hit()) {
                return lockedRead.value();
            }

            R result = dbFallback.apply(id);
            write(cacheKey, result, baseSeconds);
            return result;
        } finally {
            if (token != null) {
                unlock(lockKey, token);
            }
        }
    }

    public String buildKey(String keyPrefix, Object id) {
        if (keyPrefix == null || keyPrefix.trim().isEmpty()) {
            throw new IllegalArgumentException("缓存前缀不能为空");
        }
        if (id == null || id.toString().trim().isEmpty()) {
            throw new IllegalArgumentException("缓存 ID 不能为空");
        }
        String prefix = keyPrefix.trim();
        if (!prefix.endsWith(":")) {
            prefix += ":";
        }
        return KEY_NAMESPACE + prefix + id;
    }

    public void evict(String keyPrefix, Object id) {
        try {
            redisTemplate.delete(buildKey(keyPrefix, id));
        } catch (RuntimeException e) {
            log.warn("清理缓存失败，keyPrefix={}, id={}", keyPrefix, id, e);
        }
    }

    public void evictKey(String cacheKey) {
        if (cacheKey == null || cacheKey.trim().isEmpty()) {
            return;
        }
        try {
            redisTemplate.delete(cacheKey);
        } catch (RuntimeException e) {
            log.warn("清理缓存失败，key={}", cacheKey, e);
        }
    }

    /** 删除指定缓存前缀下的所有键。使用 SCAN，避免 KEYS 阻塞 Redis。 */
    public void evictByPrefix(String keyPrefix) {
        if (keyPrefix == null || keyPrefix.trim().isEmpty()) {
            return;
        }
        String prefix = keyPrefix.trim();
        if (!prefix.endsWith(":")) {
            prefix += ":";
        }
        try {
            String pattern = KEY_NAMESPACE + prefix + "*";
            List<String> keys = new ArrayList<>();
            try (Cursor<String> cursor = redisTemplate.scan(
                    ScanOptions.scanOptions().match(pattern).count(256).build())) {
                while (cursor.hasNext()) {
                    keys.add(cursor.next());
                }
            }
            if (!keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (RuntimeException e) {
            log.warn("按前缀清理缓存失败，prefix={}", keyPrefix, e);
        }
    }

    private <R> CacheRead<R> read(String cacheKey, JavaType javaType) {
        String json;
        try {
            json = redisTemplate.opsForValue().get(cacheKey);
        } catch (RuntimeException e) {
            log.warn("读取缓存失败，降级数据库查询，key={}", cacheKey, e);
            return CacheRead.miss();
        }
        if (json == null) {
            return CacheRead.miss();
        }
        if (CACHE_NULL_VALUE.equals(json)) {
            return CacheRead.hit(null);
        }
        if (json.trim().isEmpty()) {
            evictKey(cacheKey);
            return CacheRead.miss();
        }
        try {
            return CacheRead.hit(objectMapper.readValue(json, javaType));
        } catch (Exception e) {
            log.warn("缓存内容无法反序列化，删除后回源，key={}", cacheKey, e);
            evictKey(cacheKey);
            return CacheRead.miss();
        }
    }

    private <R> void write(String cacheKey, R value, long baseSeconds) {
        try {
            if (value == null) {
                redisTemplate.opsForValue().set(cacheKey, CACHE_NULL_VALUE,
                        Math.min(NULL_CACHE_SECONDS, baseSeconds), TimeUnit.SECONDS);
                return;
            }
            String json = objectMapper.writeValueAsString(value);
            long jitter = Math.min(300L, Math.max(1L, baseSeconds / 10L));
            long finalSeconds = baseSeconds + ThreadLocalRandom.current().nextLong(jitter + 1L);
            redisTemplate.opsForValue().set(cacheKey, json, finalSeconds, TimeUnit.SECONDS);
        } catch (JsonProcessingException | RuntimeException e) {
            log.warn("写入缓存失败，不影响数据库查询结果，key={}", cacheKey, e);
        }
    }

    private String tryLock(String lockKey, long lockTtlSeconds) {
        String token = UUID.randomUUID().toString();
        try {
            Boolean acquired = redisTemplate.opsForValue().setIfAbsent(
                    lockKey, token, lockTtlSeconds, TimeUnit.SECONDS);
            return Boolean.TRUE.equals(acquired) ? token : null;
        } catch (RuntimeException e) {
            log.warn("获取缓存互斥锁失败，key={}", lockKey, e);
            return null;
        }
    }

    private void unlock(String lockKey, String token) {
        try {
            redisTemplate.execute(UNLOCK_SCRIPT, List.of(lockKey), token);
        } catch (RuntimeException e) {
            log.warn("释放缓存互斥锁失败，key={}", lockKey, e);
        }
    }

    private void sleepBeforeRetry() {
        try {
            Thread.sleep(LOCK_WAIT_MILLIS + ThreadLocalRandom.current().nextLong(25L));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("等待缓存互斥锁时线程被中断", e);
        }
    }

    private long lockTtlSeconds(long baseSeconds) {
        return Math.max(10L, Math.min(300L, baseSeconds));
    }

    private long toPositiveSeconds(long ttl, TimeUnit unit) {
        long seconds = unit.toSeconds(ttl);
        if (seconds <= 0L) {
            seconds = 1L;
        }
        return seconds;
    }

    private void validateArguments(String keyPrefix,
                                   Object id,
                                   Function<?, ?> dbFallback,
                                   long ttl,
                                   TimeUnit unit) {
        if (keyPrefix == null || keyPrefix.trim().isEmpty()) {
            throw new IllegalArgumentException("缓存前缀不能为空");
        }
        if (id == null || id.toString().trim().isEmpty()) {
            throw new IllegalArgumentException("缓存 ID 不能为空");
        }
        if (dbFallback == null) {
            throw new IllegalArgumentException("数据库回源函数不能为空");
        }
        if (ttl <= 0L) {
            throw new IllegalArgumentException("缓存过期时间必须大于 0");
        }
        if (unit == null) {
            throw new IllegalArgumentException("缓存时间单位不能为空");
        }
    }

    /**
     * CacheRead：通用工具类，封装可复用的辅助方法。
     */
    private record CacheRead<R>(boolean hit, R value) {
        private static <R> CacheRead<R> hit(R value) {
            return new CacheRead<>(true, value);
        }

        private static <R> CacheRead<R> miss() {
            return new CacheRead<>(false, null);
        }
    }
}
