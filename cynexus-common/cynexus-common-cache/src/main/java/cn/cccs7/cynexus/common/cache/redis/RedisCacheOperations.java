package cn.cccs7.cynexus.common.cache.redis;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

/** Shared string-oriented Redis operations for service infrastructure. */
public final class RedisCacheOperations {
    private final StringRedisTemplate redis;

    public RedisCacheOperations(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public String get(String key) {
        String normalizedKey = requireKey(key);
        return redis.opsForValue().get(normalizedKey);
    }

    public void set(String key, String value, Duration ttl) {
        String normalizedKey = requireKey(key);
        Duration normalizedTtl = requireTtl(ttl);
        redis.opsForValue().set(normalizedKey, value, normalizedTtl);
    }

    public void set(String key, String value, long timeout, TimeUnit unit) {
        String normalizedKey = requireKey(key);
        if (timeout <= 0 || unit == null) {
            throw new IllegalArgumentException("Redis TTL must be positive");
        }
        redis.opsForValue().set(normalizedKey, value, timeout, unit);
    }

    public boolean setIfAbsent(String key, String value, Duration ttl) {
        String normalizedKey = requireKey(key);
        Duration normalizedTtl = requireTtl(ttl);
        return Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(normalizedKey, value, normalizedTtl));
    }

    public boolean exists(String key) {
        String normalizedKey = requireKey(key);
        return Boolean.TRUE.equals(redis.hasKey(normalizedKey));
    }

    public boolean delete(String key) {
        String normalizedKey = requireKey(key);
        return Boolean.TRUE.equals(redis.delete(normalizedKey));
    }

    public <T> T execute(RedisScript<T> script, List<String> keys, Object... args) {
        return redis.execute(script, keys, args);
    }

    private String requireKey(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Redis key must not be blank");
        }
        return key;
    }

    private Duration requireTtl(Duration ttl) {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("Redis TTL must be positive");
        }
        return ttl;
    }
}
