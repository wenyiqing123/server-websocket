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
 * 高可用 JSON 字符串缓存客户端。
 * <p>
 * 核心功能特性：
 * 1. 解决【缓存穿透】：查询数据库为空时，缓存一个特定的空值标记（__CACHE_NULL__），并设置较短的过期时间。
 * 2. 解决【缓存击穿】：热点 Key 失效时，采用 Redis 分布式互斥锁（Mutex），保证只有一个线程去查询数据库并重建缓存。
 * 3. 解决【缓存雪崩】：在基础 TTL 之上增加随机的抖动时间（Jitter），防止大量缓存同时过期。
 * 4. 解决【锁误删】：使用 Lua 脚本保证释放分布式锁时的原子性（只删除属于自己线程的锁）。
 * 5. 安全清理：使用 SCAN 替代 KEYS 命令进行前缀批量清理，防止 Redis 阻塞。
 */
@Component
@Slf4j
public class CacheClient {

    // 缓存键的全局命名空间，方便后期数据迁移或清理（如升级为 v3）
    private static final String KEY_NAMESPACE = "cache:v2:";
    // 用于解决【缓存穿透】的空值标记
    private static final String CACHE_NULL_VALUE = "__CACHE_NULL__";
    // 分布式互斥锁的命名空间前缀
    private static final String LOCK_NAMESPACE = KEY_NAMESPACE + "lock:";
    // 获取分布式互斥锁的最大尝试次数
    private static final int MAX_LOCK_ATTEMPTS = 40;
    // 获取分布式互斥锁失败后的线程休眠基准时间（毫秒）
    private static final long LOCK_WAIT_MILLIS = 75L;
    // 缓存空值（解决穿透）的过期时间（秒）
    private static final long NULL_CACHE_SECONDS = 120L;

    // 释放分布式锁的 Lua 脚本：先校验 Value 是否匹配，匹配才执行 del，保证原子性防止误删
    private static final String UNLOCK_SCRIPT_TEXT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then "
                    + "return redis.call('del', KEYS[1]) else return 0 end";
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT =
            new DefaultRedisScript<>(UNLOCK_SCRIPT_TEXT, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 构造函数注入依赖，并初始化定制化的 ObjectMapper。
     */
    public CacheClient(StringRedisTemplate redisTemplate) {
        this.redisTemplate = Objects.requireNonNull(redisTemplate, "redisTemplate");
        this.objectMapper = new ObjectMapper()
                // 支持 Java 8 的 LocalDateTime 等时间类
                .registerModule(new JavaTimeModule())
                // 时间不序列化为时间戳，而是标准的 ISO-8601 字符串
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                // 反序列化时，遇到未知属性不报错，增强向下兼容性
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * 互斥锁查询缓存（针对普通 Class 类型）。
     *
     * @param keyPrefix  缓存业务前缀（如 "user:info"）
     * @param id         业务数据的主键 ID
     * @param type       返回值的 Class 类型
     * @param dbFallback 缓存未命中时，查询数据库的回源函数 (如 id -> userMapper.selectById(id))
     * @param ttl        缓存过期时间
     * @param unit       过期时间单位
     * @param <R>        返回值泛型
     * @param <ID>       ID 泛型
     * @return 业务数据对象
     */
    public <R, ID> R queryWithMutex(String keyPrefix,
                                    ID id,
                                    Class<R> type,
                                    Function<ID, R> dbFallback,
                                    long ttl,
                                    TimeUnit unit) {
        Objects.requireNonNull(type, "type");
        // 将 Class 转换为 Jackson 的 JavaType，交由底层私有方法处理
        return queryWithMutex(keyPrefix, id, objectMapper.getTypeFactory().constructType(type), dbFallback, ttl, unit);
    }

    /**
     * 互斥锁查询缓存（针对泛型集合类型，如 List<User>）。
     *
     * @param type TypeReference，用于保留泛型类型信息，防止泛型擦除
     *             (例如使用 new TypeReference<List<User>>(){})
     * @see #queryWithMutex(String, Object, Class, Function, long, TimeUnit)
     */
    public <R, ID> R queryWithMutex(String keyPrefix,
                                    ID id,
                                    TypeReference<R> type,
                                    Function<ID, R> dbFallback,
                                    long ttl,
                                    TimeUnit unit) {
        Objects.requireNonNull(type, "type");
        // 将 TypeReference 转换为 Jackson 的 JavaType
        return queryWithMutex(keyPrefix, id, objectMapper.getTypeFactory().constructType(type.getType()), dbFallback, ttl, unit);
    }

    /**
     * 互斥锁查询缓存的核心底层方法。
     * 使用互斥锁机制解决【缓存击穿】问题。
     */
    private <R, ID> R queryWithMutex(String keyPrefix,
                                     ID id,
                                     JavaType javaType,
                                     Function<ID, R> dbFallback,
                                     long ttl,
                                     TimeUnit unit) {
        validateArguments(keyPrefix, id, dbFallback, ttl, unit);
        long baseSeconds = toPositiveSeconds(ttl, unit);
        String cacheKey = buildKey(keyPrefix, id);

        // 1. 首次查询缓存
        CacheRead<R> firstRead = read(cacheKey, javaType);
        if (firstRead.hit()) {
            return firstRead.value(); // 命中直接返回（包括命中了真实的 null 值）
        }

        // 2. 缓存未命中，准备获取互斥锁去查数据库
        String lockKey = LOCK_NAMESPACE + cacheKey.substring(KEY_NAMESPACE.length());
        String token = null; // 用于标识当前线程持有的锁凭证

        try {
            // 循环尝试获取锁（自旋等待）
            for (int attempt = 0; attempt < MAX_LOCK_ATTEMPTS; attempt++) {
                // 每次抢锁前，再次检查缓存（Double Check）。因为可能在上一次休眠期间，别人已经把缓存建好了
                CacheRead<R> retryRead = read(cacheKey, javaType);
                if (retryRead.hit()) {
                    return retryRead.value();
                }

                // 尝试获取分布式锁
                token = tryLock(lockKey, lockTtlSeconds(baseSeconds));
                if (token != null) {
                    break; // 抢锁成功，跳出循环执行查库逻辑
                }
                // 抢锁失败，休眠一会儿后重试
                sleepBeforeRetry();
            }

            // 3. 超过最大重试次数仍然拿不到锁，作为兜底，直接降级查询数据库（避免死锁导致业务不可用）
            if (token == null) {
                log.warn("缓存互斥锁等待超时，降级查询数据库，key={}", cacheKey);
                return dbFallback.apply(id);
            }

            // 4. 拿到锁后，最后做一次 Double Check 检查缓存 (极其严谨)
            CacheRead<R> lockedRead = read(cacheKey, javaType);
            if (lockedRead.hit()) {
                return lockedRead.value();
            }

            // 5. 真正查询数据库
            R result = dbFallback.apply(id);

            // 6. 将数据库结果写入缓存（内部已处理穿透和雪崩逻辑）
            write(cacheKey, result, baseSeconds);

            return result;
        } finally {
            // 7. 无论业务执行成功与否，必须释放当前线程自己加的锁
            if (token != null) {
                unlock(lockKey, token);
            }
        }
    }

    /**
     * 构建标准化的缓存 Key。
     */
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

    /**
     * 删除指定前缀和 ID 的缓存。
     */
    public void evict(String keyPrefix, Object id) {
        try {
            redisTemplate.delete(buildKey(keyPrefix, id));
        } catch (RuntimeException e) {
            log.warn("清理缓存失败，keyPrefix={}, id={}", keyPrefix, id, e);
        }
    }

    /**
     * 直接根据绝对 Key 删除缓存。
     */
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

    /**
     * 删除指定缓存前缀下的所有键。
     * 【重点】：使用 SCAN 指令替代 KEYS 指令，防止在键值极多时阻塞 Redis 的单线程工作。
     */
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
            // 使用 Cursor 进行迭代扫描，每次 count(256)
            try (Cursor<String> cursor = redisTemplate.scan(
                    ScanOptions.scanOptions().match(pattern).count(256).build())) {
                while (cursor.hasNext()) {
                    keys.add(cursor.next());
                }
            }
            // 批量删除扫描出来的 key
            if (!keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (RuntimeException e) {
            log.warn("按前缀清理缓存失败，prefix={}", keyPrefix, e);
        }
    }

    /**
     * 从 Redis 读取缓存，并处理 JSON 反序列化及【空值标记】。
     */
    private <R> CacheRead<R> read(String cacheKey, JavaType javaType) {
        String json;
        try {
            json = redisTemplate.opsForValue().get(cacheKey);
        } catch (RuntimeException e) {
            log.warn("读取缓存失败，降级数据库查询，key={}", cacheKey, e);
            return CacheRead.miss();
        }

        // 缓存完全未命中
        if (json == null) {
            return CacheRead.miss();
        }

        // 命中空值标记（防止缓存穿透的特殊字符），返回包含 null 值的 Hit 对象
        if (CACHE_NULL_VALUE.equals(json)) {
            return CacheRead.hit(null);
        }

        // 处理脏数据：存了空字符串，直接删除清理
        if (json.trim().isEmpty()) {
            evictKey(cacheKey);
            return CacheRead.miss();
        }

        // 正常反序列化为对象
        try {
            return CacheRead.hit(objectMapper.readValue(json, javaType));
        } catch (Exception e) {
            // 反序列化异常说明数据结构变更或存在脏数据，直接删掉让后续走查库逻辑
            log.warn("缓存内容无法反序列化，删除后回源，key={}", cacheKey, e);
            evictKey(cacheKey);
            return CacheRead.miss();
        }
    }

    /**
     * 将数据序列化并写入 Redis。
     */
    private <R> void write(String cacheKey, R value, long baseSeconds) {
        try {
            // 1. 解决【缓存穿透】：如果数据库查出来是 null，也缓存进 Redis，存入特殊标记，并设置较短的过期时间
            if (value == null) {
                redisTemplate.opsForValue().set(cacheKey, CACHE_NULL_VALUE,
                        Math.min(NULL_CACHE_SECONDS, baseSeconds), TimeUnit.SECONDS);
                return;
            }

            // 2. 正常值序列化为 JSON
            String json = objectMapper.writeValueAsString(value);

            // 3. 解决【缓存雪崩】：计算随机抖动时间（Jitter）。抖动范围是基础时间的 1/10，最大不超过 300 秒。
            long jitter = Math.min(300L, Math.max(1L, baseSeconds / 10L));
            long finalSeconds = baseSeconds + ThreadLocalRandom.current().nextLong(jitter + 1L);

            // 写入 Redis
            redisTemplate.opsForValue().set(cacheKey, json, finalSeconds, TimeUnit.SECONDS);

        } catch (JsonProcessingException | RuntimeException e) {
            // 缓存写入失败是弱依赖，不应该影响数据库主流程，仅打印日志
            log.warn("写入缓存失败，不影响数据库查询结果，key={}", cacheKey, e);
        }
    }

    /**
     * 尝试获取分布式互斥锁（SETNX）。
     *
     * @return 返回生成的 Token(UUID) 用于后续解锁，如果获取失败返回 null
     */
    private String tryLock(String lockKey, long lockTtlSeconds) {
        String token = UUID.randomUUID().toString();
        try {
            Boolean acquired = redisTemplate.opsForValue().setIfAbsent(
                    lockKey, token, lockTtlSeconds, TimeUnit.SECONDS);
            return Boolean.TRUE.equals(acquired) ? token : null;
        } catch (RuntimeException e) {
            log.warn("获取缓存互斥锁失败，key={}", lockKey, e);
            return null; // 锁系统异常时，直接返回失败让其走重试或兜底逻辑
        }
    }

    /**
     * 释放分布式互斥锁。
     * 使用 Lua 脚本保证只删除自己线程加的锁，防止误删别人加的锁。
     */
    private void unlock(String lockKey, String token) {
        try {
            redisTemplate.execute(UNLOCK_SCRIPT, List.of(lockKey), token);
        } catch (RuntimeException e) {
            log.warn("释放缓存互斥锁失败，key={}", lockKey, e);
        }
    }

    /**
     * 获取锁失败时的线程休眠逻辑，加入了随机值避免多个线程产生“惊群效应”。
     */
    private void sleepBeforeRetry() {
        try {
            // 休眠时间 = 基础时间 + 0~25ms 的随机数
            Thread.sleep(LOCK_WAIT_MILLIS + ThreadLocalRandom.current().nextLong(25L));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("等待缓存互斥锁时线程被中断", e);
        }
    }

    /**
     * 计算互斥锁的安全过期时间（保证业务有足够时间执行，同时不会无限死锁）。
     */
    private long lockTtlSeconds(long baseSeconds) {
        return Math.max(10L, Math.min(300L, baseSeconds));
    }

    /**
     * 统一转换并校验过期时间，确保至少为 1 秒。
     */
    private long toPositiveSeconds(long ttl, TimeUnit unit) {
        long seconds = unit.toSeconds(ttl);
        if (seconds <= 0L) {
            seconds = 1L;
        }
        return seconds;
    }

    /**
     * 入参校验。
     */
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
     * 内部记录类（Java 14+ Record），用于优雅地封装缓存读取结果。
     * 区分了三种情况：
     * 1. 缓存未命中 (hit = false)
     * 2. 命中真实对象 (hit = true, value = 真实对象)
     * 3. 命中防穿透的 Null 值 (hit = true, value = null)
     */
    private record CacheRead<R>(boolean hit, R value) {
        // 静态工厂：缓存命中
        private static <R> CacheRead<R> hit(R value) {
            return new CacheRead<>(true, value);
        }

        // 静态工厂：缓存未命中
        private static <R> CacheRead<R> miss() {
            return new CacheRead<>(false, null);
        }
    }
}