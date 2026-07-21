package cn.cccs7.cynexus.ai.infra.agent;

import cn.cccs7.cynexus.ai.domain.agent.AgentRuntimeLeasePort;
import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Component
public class RedisAgentRuntimeLease implements AgentRuntimeLeasePort {

    private static final String PREFIX = "cynexus:ai:agent:";
    private static final DefaultRedisScript<Long> ACQUIRE_SCRIPT = new DefaultRedisScript<>(
            "local count = tonumber(redis.call('get', KEYS[1]) or '0') "
                    + "if count >= tonumber(ARGV[1]) then return 0 end "
                    + "redis.call('incr', KEYS[1]) "
                    + "redis.call('expire', KEYS[1], ARGV[2]) "
                    + "redis.call('set', KEYS[2], '1', 'EX', ARGV[2]) "
                    + "return 1", Long.class);
    private static final DefaultRedisScript<Long> RELEASE_SCRIPT = new DefaultRedisScript<>(
            "local count = tonumber(redis.call('get', KEYS[1]) or '0') "
                    + "if count <= 1 then redis.call('del', KEYS[1]) else redis.call('decr', KEYS[1]) end "
                    + "redis.call('del', KEYS[2]) "
                    + "return 1", Long.class);

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
}
