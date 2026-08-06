package cn.cccs7.cynexus.common.cache.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import cn.cccs7.cynexus.common.cache.redis.RedisJsonBundleCacheReader;
import cn.cccs7.cynexus.common.cache.redis.RedisCacheOperations;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

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
    public RedisJsonBundleCacheReader redisJsonBundleCacheReader(
            RedisCacheOperations operations,
            ObjectMapper objectMapper) {
        return new RedisJsonBundleCacheReader(operations, objectMapper);
    }
}
