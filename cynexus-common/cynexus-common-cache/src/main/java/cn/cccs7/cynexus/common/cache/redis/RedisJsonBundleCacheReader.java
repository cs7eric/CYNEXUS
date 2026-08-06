package cn.cccs7.cynexus.common.cache.redis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.Optional;

/** Reads the latest atomically published JSON capture without owning feature semantics. */
public final class RedisJsonBundleCacheReader {
    private final RedisCacheOperations operations;
    private final ObjectMapper objectMapper;

    public RedisJsonBundleCacheReader(RedisCacheOperations operations, ObjectMapper objectMapper) {
        this.operations = operations;
        this.objectMapper = objectMapper;
    }

    public Optional<JsonNode> readLatest(String prefix, LocalDate requestedDate) {
        RedisCaptureBundleKeys keys = new RedisCaptureBundleKeys(prefix);
        try {
            String tradeDate = requestedDate == null
                    ? operations.get(keys.latestTradeDate())
                    : requestedDate.toString();
            if (tradeDate == null || tradeDate.isBlank()) {
                return Optional.empty();
            }
            String captureId = operations.get(keys.latest(tradeDate));
            if (captureId == null || captureId.isBlank()) {
                return Optional.empty();
            }
            String raw = operations.get(keys.capture(tradeDate, captureId));
            if (raw == null || raw.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readTree(raw));
        } catch (Exception ignored) {
            // Cache failures must preserve the repository's durable-store fallback.
            return Optional.empty();
        }
    }
}
