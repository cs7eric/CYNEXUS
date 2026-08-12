package cn.cccs7.cynexus.common.cache.redis;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import org.springframework.data.redis.core.RedisTemplate;

/** Byte-preserving Redis operations for compressed immutable feature bundles. */
public final class RedisBinaryCacheOperations {
    private final RedisTemplate<String, byte[]> redis;

    public RedisBinaryCacheOperations(RedisTemplate<String, byte[]> redis) {
        this.redis = Objects.requireNonNull(redis, "redis");
    }

    public byte[] get(String key) {
        return redis.opsForValue().get(requireKey(key));
    }

    public void set(String key, byte[] value, Duration ttl) {
        if (value == null) throw new IllegalArgumentException("Redis binary value must not be null");
        if (ttl == null || ttl.isZero() || ttl.isNegative()) throw new IllegalArgumentException("Redis TTL must be positive");
        redis.opsForValue().set(requireKey(key), value, ttl);
    }

    public List<byte[]> multiGet(List<String> keys) {
        if (keys == null || keys.isEmpty()) return List.of();
        return redis.opsForValue().multiGet(keys.stream().map(this::requireKey).toList());
    }

    public boolean exists(String key) {
        return Boolean.TRUE.equals(redis.hasKey(requireKey(key)));
    }

    public boolean delete(String key) {
        return Boolean.TRUE.equals(redis.delete(requireKey(key)));
    }

    private String requireKey(String key) {
        if (key == null || key.isBlank()) throw new IllegalArgumentException("Redis key must not be blank");
        return key;
    }
}
