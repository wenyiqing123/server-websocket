package cn.wyq.serverwebsocket.config;

import cn.wyq.serverwebsocket.constant.RedisKeyConstants;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

/**
 * @author wyq
 * @description Redis 核心配置类 (基于 Lettuce 客户端)
 * 包含：RedisTemplate 的定制化序列化配置、Spring Cache 缓存管理器的标准化配置
 * @date 2023-11-22
 */
@Configuration
@EnableCaching // 开启 Spring Cache 缓存注解支持 (使得 @Cacheable, @CachePut 等生效)
public class RedisConfig extends CachingConfigurerSupport {

    /**
     * 核心组件：自定义 ObjectMapper
     * 作用：统一管理对象与 JSON 字符串之间的相互转换规则，解决时间格式化和复杂对象反序列化的痛点。
     */
    private ObjectMapper createObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();

        /*
         * 1. 解决 JDK 8 新日期时间 API (如 LocalDateTime, LocalDate) 的序列化问题。
         * 如果不加这个模块，Jackson 在序列化 LocalDateTime 时会直接抛出异常。
         */
        objectMapper.registerModule(new JavaTimeModule());

        /*
         * 2. 时间格式化策略：禁用将日期序列化为时间戳 (Timestamps)。
         * 目的：让存在 Redis 里的时间变成肉眼可读的字符串 (如 "2023-11-22 10:00:00")，
         * 而不是毫无语义的长整型数字，极大提升后期运维和排查 Bug 的效率。
         */
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        /*
         * 3. 设置可见性：允许序列化对象中的所有属性 (不论是 private 还是 public)，
         * 且不需要实体类强制写 getter/setter 方法也能顺利完成 JSON 转换。
         */
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);

        /*
         * 4. 激活类型信息记录 (多态反序列化的灵魂所在)。
         * 作用：在生成的 JSON 中额外写入一个 "@class" 属性，记录该对象的完整包路径类名。
         * 为什么必须加？如果不加，从 Redis 取出 JSON 反序列化时，Spring 默认只会把它转成 LinkedHashMap，
         * 导致你在代码里强转回 User.class 或 Message.class 时触发 ClassCastException。
         * 注：LaissezFaireSubTypeValidator 是新版 Jackson 推荐的安全写法，规避了旧版的反序列化漏洞。
         */
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        return objectMapper;
    }

    /**
     * 配置 RedisTemplate (主要用于手动通过代码如 redisUtil.set() 操作 Redis)
     * * @param redisConnectionFactory 由 Spring Boot 自动注入的 Redis 连接工厂
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);

        // 初始化我们精心配置好的 JSON 序列化器
        ObjectMapper objectMapper = createObjectMapper();
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        /*
         * 设置 Key (键) 的序列化方式为 StringRedisSerializer。
         * 目的：确保存入 Redis 的 Key 是清爽的普通字符串。
         * 如果使用默认的 JDK 序列化器，Key 的前面会带上一长串十六进制乱码 (如 \xac\xed\x00\x05t\x00\x05)。
         */
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer()); // 针对 Hash 数据结构的 Key

        /*
         * 设置 Value (值) 的序列化方式为我们自定义的 JSON 序列化器。
         * 目的：让存入 Redis 的对象自动变成 JSON 格式，既省空间又方便 GUI 工具查看。
         */
        redisTemplate.setValueSerializer(jsonSerializer);
        redisTemplate.setHashValueSerializer(jsonSerializer); // 针对 Hash 数据结构的 Value

        // 使得以上的各项配置生效
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

    /**
     * 配置 RedisCacheConfiguration (制定 Spring Cache 存入 Redis 时的默认“家规”)
     */
    @Bean
    public RedisCacheConfiguration cacheConfiguration() {
        // 同样采用我们定制的 JSON 序列化器，保证手动模板和自动注解的数据格式高度统一
        ObjectMapper objectMapper = createObjectMapper();
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        return RedisCacheConfiguration.defaultCacheConfig()
                // 规范缓存 Key 的层级结构：在 CacheName 和真实的 Key 之间加上单冒号 (如 userCache:1001)
                // 这样在 RDM 等可视化工具中，缓存会自动以文件夹树状结构展示，非常规整。
                .computePrefixWith(cacheName -> cacheName + ":")

                // 设置缓存全局默认的过期时间 (TTL) 为 2 小时。
                // 极端重要：防止僵尸缓存堆积导致 Redis 内存溢出 (OOM)。
                .entryTtl(Duration.ofHours(2))

                // 指定 Cache 键和值的序列化器 (和上面 RedisTemplate 保持一致)
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer));

                // 禁止将 null 值存入缓存，节约 Redis 内存空间。
                // (注：若业务中遇到严重的“缓存穿透”攻击，可考虑移除此项以允许缓存空对象)
                //注释掉，采取缓存空值，防止缓存击穿
//                .disableCachingNullValues();
    }

    /**
     * 配置 CacheManager 缓存管理器 (负责管理和调度所有的 Cache)
     * 这里解决了启动时报错 "Cannot find cache named 'xxx'" 的核心问题。
     */
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // 获取上面写好的全局基础规则配置
        RedisCacheConfiguration config = cacheConfiguration();

        // 将项目中所有用到的 @Cacheable 的 cacheNames 在此集中登记 (白名单预热)
        Set<String> cacheNames = new HashSet<>();
        cacheNames.add(RedisKeyConstants.MANAGE_MESSAGES_CACHE);
        cacheNames.add(RedisKeyConstants.MANAGE_USERS_CACHE);
        cacheNames.add(RedisKeyConstants.HISTORY_CONVERSATIONS_CACHE);
        cacheNames.add(RedisKeyConstants.CONVERSATION_MESSAGES_CACHE);

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)          // 应用全局基础配置
                .initialCacheNames(cacheNames)  // 初始化并注册缓存名字，告诉 Spring 这些桶是合法存在的
                // 开启事务感知功能：
                // 如果在有 @Transactional 注解的方法里触发了缓存的 Put 或 Evict，
                // 只有当数据库事务真正 commit 成功后，Redis 才会进行对应的修改。
                // 完美防止数据库回滚了，但 Redis 缓存被错误清空或更新的数据不一致灾难！
                .transactionAware()
                .build();
    }
}