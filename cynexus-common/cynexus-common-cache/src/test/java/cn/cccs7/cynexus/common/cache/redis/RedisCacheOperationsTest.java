package cn.cccs7.cynexus.common.cache.redis;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class RedisCacheOperationsTest {
    @Test
    void rejectsBlankKeysAndNonPositiveTtlsBeforeTouchingRedis() {
        RedisCacheOperations operations = new RedisCacheOperations(null);

        assertThrows(IllegalArgumentException.class, () -> operations.get(" "));
        assertThrows(IllegalArgumentException.class, () -> operations.set("key", "value", Duration.ZERO));
        assertThrows(IllegalArgumentException.class, () -> operations.set("key", "value", 0, TimeUnit.SECONDS));
        assertThrows(IllegalArgumentException.class, () -> operations.setIfAbsent("key", "value", Duration.ofSeconds(-1)));
    }
}
