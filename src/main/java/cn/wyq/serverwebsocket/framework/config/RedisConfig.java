package cn.wyq.serverwebsocket.framework.config;

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
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * @author wyq
 * @description Redis配置类：Lettuce客户端 (已修复 LocalDateTime 序列化问题)
 * @date 2023-11-22
 */
@Configuration
@EnableCaching
public class RedisConfig extends CachingConfigurerSupport {

    /**
     * 自定义 ObjectMapper，用于解决 LocalDateTime 序列化和多态类型问题
     */
    private ObjectMapper createObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();

        // 1. 注册 JavaTimeModule，解决 LocalDateTime 报错的核心！
        objectMapper.registerModule(new JavaTimeModule());

        // 2. 禁用“将日期写为时间戳”，这样 Redis 里存的是 "2023-11-22 10:00:00" 而不是一串数字
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 3. 设置可见性
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);

        // 4. 激活类型信息（重要）：这样 Redis 才知道存的是哪个具体的类，反序列化不会报错
        // 使用 LaissezFaireSubTypeValidator 是较新版本 Jackson 的推荐写法
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        return objectMapper;
    }

    /**
     * 配置 RedisTemplate (用于手动 redisUtil 操作)
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);

        // 使用配置好的 ObjectMapper 创建序列化器
        ObjectMapper objectMapper = createObjectMapper();
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        // 设置 Key 的序列化
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());

        // 设置 Value 的序列化 (使用我们自定义的 jsonSerializer)
        redisTemplate.setValueSerializer(jsonSerializer);
        redisTemplate.setHashValueSerializer(jsonSerializer);

        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

    /**
     * 配置 RedisCacheConfiguration (用于 @Cacheable 注解)
     */
    @Bean
    public RedisCacheConfiguration cacheConfiguration() {
        // 使用同样的 ObjectMapper 配置
        ObjectMapper objectMapper = createObjectMapper();
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        return RedisCacheConfiguration.defaultCacheConfig()
                .computePrefixWith(cacheName -> cacheName + ":")
                .entryTtl(Duration.ofHours(2)) // 建议设置默认过期时间，比如 2 小时
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                // 关键：这里也要用支持 JavaTimeModule 的序列化器
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                .disableCachingNullValues();
    }
}