package cn.cccs7.cynexus.common.cache.redis;

import java.util.Objects;

/**
 * Canonical keys for an immutable, versioned Redis capture bundle.
 *
 * <p>The same key contract is implemented by the Python producer and the
 * Java readers. Feature code must receive a prefix from configuration and
 * must not concatenate Redis keys itself.</p>
 */
public final class RedisCaptureBundleKeys {
    private final String prefix;

    public RedisCaptureBundleKeys(String prefix) {
        String normalized = Objects.requireNonNull(prefix, "prefix").trim();
        while (normalized.endsWith(":")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.isBlank() || normalized.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException("Redis cache prefix must be non-blank and contain no whitespace");
        }
        this.prefix = normalized;
    }

    public String latestTradeDate() {
        return prefix + ":latest-trade-date";
    }

    public String latest(String tradeDate) {
        return prefix + ":latest:" + requiredPart(tradeDate, "tradeDate");
    }

    public String capture(String tradeDate, String captureId) {
        return prefix + ":capture:" + requiredPart(tradeDate, "tradeDate") + ":" + requiredPart(captureId, "captureId");
    }

    public String pending(String tradeDate) {
        return prefix + ":pending:" + requiredPart(tradeDate, "tradeDate");
    }

    public String prefix() {
        return prefix;
    }

    private String requiredPart(String value, String name) {
        String normalized = Objects.requireNonNull(value, name).trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return normalized;
    }
}
