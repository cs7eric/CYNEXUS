package cn.cccs7.cynexus.gateway.auth.infra;

import cn.cccs7.cynexus.common.cache.redis.RedisCacheOperations;
import cn.cccs7.cynexus.gateway.auth.domain.OwnerAuthService;
import cn.cccs7.cynexus.gateway.auth.domain.OwnerCredentials;
import cn.cccs7.cynexus.gateway.auth.domain.OwnerSessionStore;
import java.time.Duration;
import java.util.List;
import org.springframework.data.redis.core.script.RedisScript;

/** Authentication is fail-closed on Redis errors. Lua keeps refresh/revocation atomic. */
public final class RedisOwnerSessionStore implements OwnerSessionStore {
    private final RedisCacheOperations redis;
    private final String prefix;
    private static final RedisScript<Long> RATE = RedisScript.of("""
            local n = redis.call('INCR', KEYS[1])
            if n == 1 then redis.call('EXPIRE', KEYS[1], 900) end
            return n
            """, Long.class);
    private static final RedisScript<Long> CREATE = RedisScript.of("""
            redis.call('SETNX', KEYS[1], ARGV[1])
            local epoch = redis.call('GET', KEYS[1])
            redis.call('SET', KEYS[2], epoch .. '|' .. ARGV[2], 'EX', ARGV[3])
            return 1
            """, Long.class);
    private static final RedisScript<String> TOUCH = RedisScript.of("""
            local value = redis.call('GET', KEYS[2])
            local epoch = redis.call('GET', KEYS[1])
            if not value or not epoch then return nil end
            local start = epoch .. '|'
            if string.sub(value, 1, string.len(start)) ~= start then
              redis.call('DEL', KEYS[2])
              return nil
            end
            local session = string.sub(value, string.len(start) + 1)
            local ttl = ARGV[2]
            if string.sub(session, -1) == '1' then ttl = ARGV[1] end
            redis.call('EXPIRE', KEYS[2], ttl)
            return session
            """, String.class);

    public RedisOwnerSessionStore(RedisCacheOperations redis, OwnerCredentials credentials) {
        this.redis = redis;
        prefix = "cynexus:owner-auth:" + credentials.fingerprint() + ":";
    }

    @Override public void checkLoginRate(String client) {
        Long global = redis.execute(RATE, List.of(prefix + "rate:global"));
        Long local = redis.execute(RATE, List.of(prefix + "rate:" + OwnerCredentials.digest(client)));
        if (global == null || local == null) throw new IllegalStateException("Login limiter unavailable");
        if (global > 100 || local > 10) throw new OwnerAuthService.RateLimited();
    }

    @Override public void create(String tokenHash, Session session, Duration ttl) {
        redis.execute(CREATE, List.of(prefix + "epoch", prefix + tokenHash), OwnerAuthService.randomToken(),
                session.csrfToken() + "|" + (session.remembered() ? "1" : "0"), Long.toString(ttl.toSeconds()));
    }

    @Override public Session touch(String tokenHash, Duration rememberedTtl, Duration temporaryTtl) {
        String value = redis.execute(TOUCH, List.of(prefix + "epoch", prefix + tokenHash),
                Long.toString(rememberedTtl.toSeconds()), Long.toString(temporaryTtl.toSeconds()));
        if (value == null) return null;
        String[] parts = value.split("\\|", -1);
        if (parts.length != 2 || !parts[0].matches("[A-Za-z0-9_-]{43}")
                || !(parts[1].equals("0") || parts[1].equals("1"))) return null;
        return new Session(parts[0], parts[1].equals("1"));
    }

    @Override public void revoke(String tokenHash) { redis.delete(prefix + tokenHash); }

    @Override public void revokeAll() {
        // Epoch must outlive every session; a missing epoch also invalidates sessions.
        redis.set(prefix + "epoch", OwnerAuthService.randomToken(), Duration.ofDays(36500));
    }
}
