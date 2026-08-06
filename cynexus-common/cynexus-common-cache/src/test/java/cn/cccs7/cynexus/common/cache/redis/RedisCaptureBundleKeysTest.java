package cn.cccs7.cynexus.common.cache.redis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class RedisCaptureBundleKeysTest {
    @Test
    void buildsCanonicalCaptureKeys() {
        RedisCaptureBundleKeys keys = new RedisCaptureBundleKeys("cynexus:market-indicator:");

        assertEquals("cynexus:market-indicator:latest-trade-date", keys.latestTradeDate());
        assertEquals("cynexus:market-indicator:latest:2026-08-04", keys.latest("2026-08-04"));
        assertEquals("cynexus:market-indicator:capture:2026-08-04:2026-08-04T12:22:09+00:00", keys.capture("2026-08-04", "2026-08-04T12:22:09+00:00"));
        assertEquals("cynexus:market-indicator:pending:2026-08-04", keys.pending("2026-08-04"));
    }

    @Test
    void rejectsUnsafePrefix() {
        assertThrows(IllegalArgumentException.class, () -> new RedisCaptureBundleKeys(" "));
        assertThrows(IllegalArgumentException.class, () -> new RedisCaptureBundleKeys("cynexus market"));
    }
}
