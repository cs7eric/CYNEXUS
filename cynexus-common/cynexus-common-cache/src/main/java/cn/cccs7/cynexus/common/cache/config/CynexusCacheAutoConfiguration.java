package cn.cccs7.cynexus.common.cache.config;

import cn.cccs7.cynexus.common.cache.redis.RedisJsonBundleCacheReader;
import cn.cccs7.cynexus.common.cache.redis.RedisCacheOperations;
import cn.cccs7.cynexus.common.cache.redis.RedisBinaryCacheOperations;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import tools.jackson.databind.ObjectMapper;

/** Shared cache infrastructure. Feature-specific cache readers belong in service infra packages. */
@Configuration(proxyBeanMethods = false)
public class CynexusCacheAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public RedisCacheOperations redisCacheOperations(StringRedisTemplate redis) {
        return new RedisCacheOperations(redis);
    }

    @Bean
    @ConditionalOnMissingBean
    public RedisBinaryCacheOperations redisBinaryCacheOperations(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, byte[]> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(RedisSerializer.string());
        template.setHashKeySerializer(RedisSerializer.string());
        template.setValueSerializer(RedisSerializer.byteArray());
        template.setHashValueSerializer(RedisSerializer.byteArray());
        template.afterPropertiesSet();
        return new RedisBinaryCacheOperations(template);
    }

    @Bean
    @ConditionalOnMissingBean
    public RedisJsonBundleCacheReader redisJsonBundleCacheReader(
            RedisCacheOperations operations,
            ObjectMapper objectMapper) {
        return new RedisJsonBundleCacheReader(operations, objectMapper);
    }
}
