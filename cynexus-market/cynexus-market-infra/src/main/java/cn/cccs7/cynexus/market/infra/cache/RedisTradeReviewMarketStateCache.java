package cn.cccs7.cynexus.market.infra.cache;

import cn.cccs7.cynexus.common.cache.redis.RedisCacheOperations;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.GZIPInputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Reads the immutable post-close Trade Review day bundle published by the data
 * job. The cache is a complete read-model acceleration only: a corrupt/missing
 * value is intentionally indistinguishable from a cache miss to callers.
 */
@Component
public class RedisTradeReviewMarketStateCache {
    private static final ZoneId MARKET_ZONE = ZoneId.of("Asia/Shanghai");
    private final RedisCacheOperations operations;
    private final ObjectMapper objectMapper;
    private final String prefix;

    public RedisTradeReviewMarketStateCache(
            RedisCacheOperations operations,
            ObjectMapper objectMapper,
            @Value("${cynexus.market.trade-review.redis-prefix:cynexus:trade-review:market-state}") String prefix) {
        this.operations = operations;
        this.objectMapper = objectMapper;
        this.prefix = prefix;
    }

    public Optional<Map<String, Object>> state(LocalDate tradeDate, OffsetDateTime asOf, String membership,
                                                String industryCode) {
        if (tradeDate == null) return Optional.empty();
        try {
            String dayKey = prefix + ":day:" + tradeDate;
            String raw = cacheValue(dayKey);
            if (raw == null || raw.isBlank()) return Optional.empty();
            JsonNode root = objectMapper.readTree(raw);
            if (!root.isObject() || root.path("tradeDate").asText("").isBlank()) return Optional.empty();
            if (root.path("cacheVersion").asInt(1) >= 2) {
                return stateV2(root, dayKey, asOf, membership, industryCode);
            }
            JsonNode minute = asOf == null ? null : latestMinute(root.path("minutes"), asOf);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("manifest", plain(root.path("manifest")));
            result.put("market", plain(asOf == null ? root.path("daily") : minute == null ? null : minute.path("market")));
            result.put("industries", filtered(asOf == null ? root.path("dailyIndustries") : minute == null ? null : minute.path("industries"), membership, industryCode));
            result.put("pools", filteredPools(asOf == null ? root.path("dailyPools") : minute == null ? null : minute.path("pools"), membership, industryCode));
            result.put("events", asOf == null ? List.of() : eventsUntil(root.path("events"), asOf));
            result.put("history", plain(root.path("history")));
            result.put("trend", plain(root.path("phase")));
            result.put("industryTrends", filtered(root.path("industryPhases"), membership, industryCode));
            return Optional.of(result);
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    /**
     * v2 stores immutable compressed keys per cursor minute. It avoids
     * deserializing all industries/all minutes for every playback movement.
     */
    private Optional<Map<String, Object>> stateV2(JsonNode root, String dayKey, OffsetDateTime asOf,
                                                  String membership, String industryCode) throws IOException {
        JsonNode minute = null;
        if (asOf != null) {
            String minuteAt = latestMinuteKey(root.path("minuteKeys"), asOf);
            if (minuteAt == null) return Optional.empty();
            OffsetDateTime parsed = timeText(minuteAt);
            if (parsed == null) return Optional.empty();
            String minuteRaw = cacheValue(dayKey + ":minute:" + String.format("%02d%02d", parsed.getHour(), parsed.getMinute()));
            if (minuteRaw == null || minuteRaw.isBlank()) return Optional.empty();
            minute = objectMapper.readTree(minuteRaw);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("manifest", plain(root.path("manifest")));
        result.put("market", plain(asOf == null ? root.path("daily") : minute.path("market")));
        result.put("industries", filtered(asOf == null ? root.path("dailyIndustries") : minute.path("industries"), membership, industryCode));
        result.put("pools", filteredPools(asOf == null ? root.path("dailyPools") : minute.path("pools"), membership, industryCode));
        result.put("events", asOf == null ? List.of() : eventsUntilIndex(cacheNode(dayKey + ":events"), asOf));
        result.put("history", plain(root.path("history")));
        result.put("trend", plain(root.path("phase")));
        result.put("industryTrends", filtered(root.path("industryPhases"), membership, industryCode));
        return Optional.of(result);
    }

    private JsonNode latestMinute(JsonNode minutes, OffsetDateTime asOf) {
        if (minutes == null || !minutes.isArray()) return null;
        JsonNode latest = null;
        for (JsonNode candidate : minutes) {
            OffsetDateTime at = time(candidate, "minuteAt");
            if (at != null && !at.isAfter(asOf) && (latest == null || at.isAfter(time(latest, "minuteAt")))) latest = candidate;
        }
        return latest;
    }

    private String latestMinuteKey(JsonNode minuteKeys, OffsetDateTime asOf) {
        if (minuteKeys == null || !minuteKeys.isArray()) return null;
        OffsetDateTime latest = null;
        String result = null;
        for (JsonNode candidate : minuteKeys) {
            String raw = candidate.asText("");
            OffsetDateTime at = timeText(raw);
            if (at != null && !at.isAfter(asOf) && (latest == null || at.isAfter(latest))) {
                latest = at;
                result = raw;
            }
        }
        return result;
    }

    private List<Object> filtered(JsonNode values, String membership, String industryCode) {
        if (values == null || !values.isArray()) return List.of();
        List<Object> result = new ArrayList<>();
        for (JsonNode value : values) {
            if (!value.isObject()) continue;
            if (membership != null && !membership.isBlank() && !membership.equalsIgnoreCase(text(value, "membership"))) continue;
            String valueIndustryCode = text(value, "industry_code");
            // A selected industry is a strict scope. The lower replay slot
            // must not mix global pool rows into an industry's ladder or
            // feedback pool; the right-side market-state tab asks without an
            // industry code when it needs the whole market.
            if (industryCode != null && !industryCode.isBlank()
                    && !industryCode.equals(valueIndustryCode)) continue;
            result.add(plain(value));
        }
        return result;
    }

    private List<Object> filteredPools(JsonNode values, String membership, String industryCode) {
        if (values == null || !values.isArray()) return List.of();
        List<Object> result = new ArrayList<>();
        for (JsonNode value : values) {
            if (!value.isObject()) continue;
            if (membership != null && !membership.isBlank() && !membership.equalsIgnoreCase(text(value, "membership"))) continue;
            String valueIndustryCode = text(value, "industry_code");
            boolean selectedIndustry = industryCode != null && !industryCode.isBlank();
            if (selectedIndustry && !industryCode.equals(valueIndustryCode)) continue;
            if (!selectedIndustry && valueIndustryCode != null && !valueIndustryCode.isBlank()) continue;
            result.add(plain(value));
        }
        return result;
    }

    private List<Object> eventsUntil(JsonNode values, OffsetDateTime asOf) {
        if (values == null || !values.isArray()) return List.of();
        List<JsonNode> selected = new ArrayList<>();
        for (JsonNode value : values) {
            OffsetDateTime at = time(value, "event_at");
            if (at != null && !at.isAfter(asOf)) selected.add(value);
        }
        selected.sort(Comparator.comparing(value -> time(value, "event_at"), Comparator.nullsLast(Comparator.naturalOrder())));
        return selected.stream().map(this::plain).toList();
    }

    private List<Object> eventsUntilIndex(JsonNode root, OffsetDateTime asOf) {
        if (root == null || !root.isObject()) return List.of();
        JsonNode indexed = root.path("events");
        if (!indexed.isObject()) return List.of();
        List<JsonNode> selected = new ArrayList<>();
        OffsetDateTime cursorMinute = asOf.truncatedTo(ChronoUnit.MINUTES);
        indexed.properties().forEach(entry -> {
            OffsetDateTime minute = timeText(entry.getKey());
            // The replay notification area needs events that occur in the
            // current cursor minute, not the entire morning's event history.
            // Timeline/history are queried separately; returning all prior
            // events made every cursor payload grow throughout the session.
            if (minute == null || !minute.isEqual(cursorMinute) || !entry.getValue().isArray()) return;
            // The index is minute-bucketed for Redis compactness, but the
            // replay cursor is second-accurate. Filter again at event level
            // so 10:03:20 never leaks into a 10:03:00 review frame.
            entry.getValue().forEach(event -> {
                OffsetDateTime eventAt = time(event, "event_at");
                if (eventAt != null && !eventAt.isAfter(asOf)) selected.add(event);
            });
        });
        selected.sort(Comparator.comparing(value -> time(value, "event_at"), Comparator.nullsLast(Comparator.naturalOrder())));
        return selected.stream().map(this::plain).toList();
    }

    private OffsetDateTime time(JsonNode value, String field) {
        return timeText(text(value, field));
    }

    private OffsetDateTime timeText(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String normalized = raw.trim().replace(' ', 'T').replace("Z", "+00:00");
        try { return OffsetDateTime.parse(normalized); }
        catch (RuntimeException ignored) {
            try { return LocalDateTime.parse(normalized).atZone(MARKET_ZONE).toOffsetDateTime(); }
            catch (RuntimeException invalid) { return null; }
        }
    }

    private JsonNode cacheNode(String key) throws IOException {
        String raw = cacheValue(key);
        return raw == null || raw.isBlank() ? null : objectMapper.readTree(raw);
    }

    private String cacheValue(String key) throws IOException {
        String raw = operations.get(key);
        if (raw == null || !raw.startsWith("gzip:")) return raw;
        byte[] encoded = Base64.getDecoder().decode(raw.substring("gzip:".length()));
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(encoded))) {
            return new String(gzip.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String text(JsonNode value, String field) {
        JsonNode node = value == null ? null : value.get(field);
        return node == null || node.isNull() ? null : node.asText();
    }

    private Object plain(JsonNode value) {
        if (value == null || value.isNull() || value.isMissingNode()) return null;
        if (value.isTextual()) return value.asText();
        if (value.isBoolean()) return value.asBoolean();
        if (value.isNumber()) return value.numberValue();
        if (value.isArray()) {
            List<Object> result = new ArrayList<>();
            value.forEach(item -> result.add(plain(item)));
            return result;
        }
        if (value.isObject()) {
            Map<String, Object> result = new LinkedHashMap<>();
            value.properties().forEach(entry -> result.put(entry.getKey(), plain(entry.getValue())));
            return result;
        }
        return value.toString();
    }
}
