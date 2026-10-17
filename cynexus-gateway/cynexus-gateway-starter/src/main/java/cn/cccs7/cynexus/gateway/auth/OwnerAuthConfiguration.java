package cn.cccs7.cynexus.gateway.auth;

import cn.cccs7.cynexus.common.cache.redis.RedisCacheOperations;
import cn.cccs7.cynexus.gateway.auth.domain.OwnerAuthService;
import cn.cccs7.cynexus.gateway.auth.domain.OwnerCredentials;
import cn.cccs7.cynexus.gateway.auth.infra.RedisOwnerSessionStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration(proxyBeanMethods = false)
public class OwnerAuthConfiguration {
    @Bean OwnerAuthService ownerAuthService(StringRedisTemplate redis,
            @Value("${CYNEXUS_OWNER_PASSWORD_HASH:}") String passwordHash) {
        var credentials = new OwnerCredentials(passwordHash);
        return new OwnerAuthService(credentials, new RedisOwnerSessionStore(new RedisCacheOperations(redis), credentials));
    }
}
