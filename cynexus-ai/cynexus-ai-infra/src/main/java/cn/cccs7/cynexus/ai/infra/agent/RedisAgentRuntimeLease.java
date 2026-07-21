package cn.cccs7.cynexus.ai.infra.agent;

import cn.cccs7.cynexus.ai.domain.agent.AgentRuntimeLeasePort;
import cn.cccs7.cynexus.ai.domain.agent.AgentRateLimitPort;
import cn.cccs7.cynexus.ai.domain.agent.AgentCircuitBreakerPort;
import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Component
public class RedisAgentRuntimeLease implements AgentRuntimeLeasePort, AgentRateLimitPort, AgentCircuitBreakerPort {

    private static final String PREFIX = "cynexus:ai:agent:";
    private static final DefaultRedisScript<Long> ACQUIRE_SCRIPT = new DefaultRedisScript<>(
            "local count = tonumber(redis.call('get', KEYS[1]) or '0') "
                    + "if count >= tonumber(ARGV[1]) then return 0 end "
                    + "redis.call('incr', KEYS[1]) "
                    + "redis.call('expire', KEYS[1], ARGV[2]) "
                    + "redis.call('set', KEYS[2], '1', 'EX', ARGV[2]) "
                    + "return 1", Long.class);
    private static final DefaultRedisScript<Long> RELEASE_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('exists', KEYS[2]) == 0 then return 0 end "
                    + "local count = tonumber(redis.call('get', KEYS[1]) or '0') "
                    + "if count <= 1 then redis.call('del', KEYS[1]) else redis.call('decr', KEYS[1]) end "
                    + "redis.call('del', KEYS[2]) return 1", Long.class);
    private static final DefaultRedisScript<Long> RATE_LIMIT_SCRIPT = new DefaultRedisScript<>(
            "local count = redis.call('incr', KEYS[1]) "
                    + "if count == 1 then redis.call('expire', KEYS[1], ARGV[2]) end "
                    + "if count > tonumber(ARGV[1]) then return 0 end "
                    + "return 1", Long.class);
    private static final DefaultRedisScript<Long> CIRCUIT_ALLOW_SCRIPT = new DefaultRedisScript<>(
            "local openUntil = tonumber(redis.call('hget', KEYS[1], 'open_until') or '0') "
                    + "local now = tonumber(ARGV[1]) "
                    + "if openUntil > now then return 0 end "
                    + "if openUntil > 0 then "
                    + "  if redis.call('hsetnx', KEYS[1], 'probe', '1') == 1 then return 1 else return 0 end "
                    + "end return 1", Long.class);
    private static final DefaultRedisScript<Long> CIRCUIT_FAILURE_SCRIPT = new DefaultRedisScript<>(
            "local failures = redis.call('hincrby', KEYS[1], 'failures', 1) "
                    + "local openUntil = tonumber(redis.call('hget', KEYS[1], 'open_until') or '0') "
                    + "local now = tonumber(ARGV[1]) "
                    + "if openUntil > 0 or failures >= tonumber(ARGV[2]) then "
                    + "  redis.call('hset', KEYS[1], 'open_until', now + tonumber(ARGV[3]), 'probe', '0') "
                    + "end redis.call('expire', KEYS[1], ARGV[4]) return failures", Long.class);

    private final StringRedisTemplate redis;

    public RedisAgentRuntimeLease(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public boolean tryAcquire(String ownerKey, String runNo, int maxActiveRuns, int ttlSeconds) {
        Long result = redis.execute(ACQUIRE_SCRIPT,
                java.util.List.of(activeKey(ownerKey), leaseKey(runNo)),
                String.valueOf(Math.max(1, maxActiveRuns)), String.valueOf(Math.max(30, ttlSeconds)));
        if (!Long.valueOf(1L).equals(result)) return false;
        redis.opsForValue().set(ownerKey(runNo), ownerKey, Duration.ofSeconds(Math.max(30, ttlSeconds)));
        return true;
    }

    @Override
    public void release(String ownerKey, String runNo) {
        redis.execute(RELEASE_SCRIPT, java.util.List.of(activeKey(ownerKey), leaseKey(runNo)));
        redis.delete(ownerKey(runNo));
    }

    @Override
    public boolean requestCancellation(String ownerKey, String runNo, int ttlSeconds) {
        if (!Boolean.TRUE.equals(redis.hasKey(leaseKey(runNo)))) return false;
        String owner = redis.opsForValue().get(ownerKey(runNo));
        if (owner != null && !owner.equals(ownerKey)) return false;
        redis.opsForValue().set(cancelKey(runNo), "1", Duration.ofSeconds(Math.max(30, ttlSeconds)));
        return true;
    }

    @Override
    public boolean isCancellationRequested(String runNo) {
        return Boolean.TRUE.equals(redis.hasKey(cancelKey(runNo)));
    }

    @Override
    public boolean isLeaseActive(String runNo) {
        return Boolean.TRUE.equals(redis.hasKey(leaseKey(runNo)));
    }

    @Override
    public boolean allow(String ownerKey, int maxRequests, int windowSeconds) {
        Long result = redis.execute(RATE_LIMIT_SCRIPT, java.util.List.of(rateKey(ownerKey)),
                String.valueOf(Math.max(1, maxRequests)), String.valueOf(Math.max(1, windowSeconds)));
        return Long.valueOf(1L).equals(result);
    }

    @Override
    public boolean allow(String dependencyKey) {
        Long result = redis.execute(CIRCUIT_ALLOW_SCRIPT, java.util.List.of(circuitKey(dependencyKey)),
                String.valueOf(System.currentTimeMillis() / 1000));
        return Long.valueOf(1L).equals(result);
    }

    @Override
    public void recordSuccess(String dependencyKey) {
        redis.delete(circuitKey(dependencyKey));
    }

    @Override
    public void recordFailure(String dependencyKey, int failureThreshold, int openSeconds) {
        redis.execute(CIRCUIT_FAILURE_SCRIPT, java.util.List.of(circuitKey(dependencyKey)),
                String.valueOf(System.currentTimeMillis() / 1000), String.valueOf(Math.max(1, failureThreshold)),
                String.valueOf(Math.max(1, openSeconds)), String.valueOf(Math.max(30, openSeconds * 2)));
    }

    private String activeKey(String ownerKey) {
        return PREFIX + "active:" + ownerKey;
    }

    private String leaseKey(String runNo) {
        return PREFIX + "lease:" + runNo;
    }

    private String ownerKey(String runNo) {
        return PREFIX + "lease-owner:" + runNo;
    }

    private String cancelKey(String runNo) {
        return PREFIX + "cancel:" + runNo;
    }

    private String rateKey(String ownerKey) {
        return PREFIX + "rate:" + ownerKey;
    }

    private String circuitKey(String dependencyKey) {
        return PREFIX + "circuit:" + dependencyKey;
    }
}
