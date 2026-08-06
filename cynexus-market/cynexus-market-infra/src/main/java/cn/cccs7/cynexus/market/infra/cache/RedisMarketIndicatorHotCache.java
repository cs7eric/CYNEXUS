package cn.cccs7.cynexus.market.infra.cache;

import com.fasterxml.jackson.databind.JsonNode;
import cn.cccs7.cynexus.common.cache.redis.RedisJsonBundleCacheReader;
import cn.cccs7.cynexus.market.infra.basic.entity.MarketIndicatorBreadthPO;
import cn.cccs7.cynexus.market.infra.basic.entity.MarketIndicatorEventPO;
import cn.cccs7.cynexus.market.infra.basic.entity.MarketIndicatorIndustrySnapshotPO;
import cn.cccs7.cynexus.market.infra.basic.entity.MarketIndicatorSnapshotPO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Iterator;
import java.util.stream.StreamSupport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Reads the complete, atomically published Python capture bundle. */
@Component
public class RedisMarketIndicatorHotCache {
    private final RedisJsonBundleCacheReader bundleReader;
    private final String prefix;
    private final String priorityPrefix;

    public RedisMarketIndicatorHotCache(
            RedisJsonBundleCacheReader bundleReader,
            @Value("${cynexus.market.indicator.redis-prefix:cynexus:market-indicator}") String prefix,
            @Value("${cynexus.market.indicator.priority-redis-prefix:cynexus:market-indicator:priority}") String priorityPrefix) {
        this.bundleReader = bundleReader;
        this.prefix = prefix;
        this.priorityPrefix = priorityPrefix;
    }

    public Optional<List<MarketIndicatorSnapshotPO>> latestSnapshots(LocalDate tradeDate, String status, int limit) {
        return bundle(tradeDate).map(root -> rows(root).map(node -> snapshot(node, root)).filter(row -> status == null || status.isBlank() || status.equals(row.getLimitStatus()))
                .sorted(Comparator.comparing(MarketIndicatorSnapshotPO::getCapturedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(MarketIndicatorSnapshotPO::getCode, Comparator.nullsLast(String::compareTo)))
                .limit(Math.max(1, limit)).toList());
    }

    public Optional<List<MarketIndicatorEventPO>> recentEvents(LocalDate tradeDate, String eventType, int limit) {
        return bundle(tradeDate).map(root -> events(root).map(node -> event(node, root))
                .filter(row -> eventType == null || eventType.isBlank() || eventType.equals(row.getEventType()))
                .sorted(Comparator.comparing(MarketIndicatorEventPO::getEventAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(Math.max(1, limit)).toList());
    }

    public Optional<MarketIndicatorBreadthPO> breadth(LocalDate tradeDate) {
        return bundle(tradeDate).map(root -> {
            MarketIndicatorBreadthPO result = new MarketIndicatorBreadthPO();
            List<MarketIndicatorSnapshotPO> rows = rows(root).map(node -> snapshot(node, root)).toList();
            result.setTotalCount(rows.size());
            result.setAdvancingCount((int) rows.stream().filter(row -> positive(row.getChangePct())).count());
            result.setDecliningCount((int) rows.stream().filter(row -> negative(row.getChangePct())).count());
            result.setFlatCount((int) rows.stream().filter(row -> row.getChangePct() == null || row.getChangePct().signum() == 0).count());
            result.setLimitUpCount((int) rows.stream().filter(row -> "limit_up_sealed".equals(row.getLimitStatus())).count());
            result.setLimitUpTouchCount((int) rows.stream().filter(row -> "limit_up_touch".equals(row.getLimitStatus())).count());
            result.setLimitDownCount((int) rows.stream().filter(row -> "limit_down_sealed".equals(row.getLimitStatus())).count());
            result.setLimitDownTouchCount((int) rows.stream().filter(row -> "limit_down_touch".equals(row.getLimitStatus())).count());
            result.setRapidRiseCount((int) events(root).filter(node -> "rapid_rise".equals(text(node, "event_type"))).count());
            result.setRapidFallCount((int) events(root).filter(node -> "rapid_fall".equals(text(node, "event_type"))).count());
            result.setCapturedAt(offset(root, "captured_at"));
            return result;
        });
    }

    public Optional<List<MarketIndicatorIndustrySnapshotPO>> latestIndustries(LocalDate tradeDate, int limit) {
        return bundle(tradeDate).map(root -> industries(root).map(this::industry)
                .sorted(Comparator.comparing(MarketIndicatorIndustrySnapshotPO::getIndustryName, Comparator.nullsLast(String::compareTo)))
                .limit(Math.max(1, limit)).toList());
    }

    public Optional<List<Map<String, Object>>> transmissionEpisodes(LocalDate tradeDate, int limit) {
        return transmission(tradeDate, "episodes", limit);
    }

    public Optional<List<Map<String, Object>>> transmissionAdvice(LocalDate tradeDate, int limit) {
        return transmission(tradeDate, "advice", limit);
    }

    public Optional<List<Map<String, Object>>> transmissionOutcomes(LocalDate tradeDate, int limit) {
        return transmission(tradeDate, "outcomes", limit);
    }

    private Optional<JsonNode> bundle(LocalDate requestedDate) {
        return bundleReader.readLatest(prefix, requestedDate);
    }

    private Optional<List<Map<String, Object>>> transmission(LocalDate tradeDate, String field, int limit) {
        return transmissionBundle(tradeDate).map(root -> {
            JsonNode values = root.path("transmission").path(field);
            List<Map<String, Object>> result = new ArrayList<>();
            JsonNode runtime = root.path("transmission").path("runtime");
            if (values.isArray()) {
                Iterator<JsonNode> iterator = values.elements();
                while (iterator.hasNext() && result.size() < Math.max(1, limit)) {
                    JsonNode value = iterator.next();
                    if (!value.isObject()) continue;
                    Map<String, Object> row = new LinkedHashMap<>();
                    value.fields().forEachRemaining(entry -> row.put(entry.getKey(), plainValue(entry.getValue())));
                    row.put("scope", text(runtime, "scope"));
                    row.put("universeMode", text(runtime, "universeMode"));
                    row.put("fullConfirmationRequired", runtime.path("fullConfirmationRequired").asBoolean(false));
                    row.put("capturedAt", text(root, "captured_at"));
                    result.add(row);
                }
            }
            return result;
        });
    }

    private Optional<JsonNode> transmissionBundle(LocalDate requestedDate) {
        return bundleReader.readLatest(priorityPrefix, requestedDate).or(() -> bundle(requestedDate));
    }

    private MarketIndicatorSnapshotPO snapshot(JsonNode node, JsonNode root) {
        MarketIndicatorSnapshotPO row = new MarketIndicatorSnapshotPO();
        row.setCode(text(node, "code")); row.setName(text(node, "name")); row.setIndustry(text(node, "industry")); row.setMarket(text(node, "market"));
        row.setLastPrice(decimal(node, "last_price")); row.setPrevClose(decimal(node, "prev_close")); row.setOpenPrice(decimal(node, "open_price"));
        row.setHighPrice(decimal(node, "high_price")); row.setLowPrice(decimal(node, "low_price")); row.setChangePct(decimal(node, "change_pct"));
        row.setAmount(decimal(node, "amount")); row.setVolume(decimal(node, "volume")); row.setTurnoverRate(decimal(node, "turnover_rate"));
        row.setLimitUpPrice(decimal(node, "limit_up_price")); row.setLimitDownPrice(decimal(node, "limit_down_price")); row.setLimitStatus(text(node, "limit_status"));
        row.setSource(text(root, "source")); row.setCapturedAt(offset(node, "captured_at"));
        return row;
    }

    private MarketIndicatorEventPO event(JsonNode node, JsonNode root) {
        MarketIndicatorEventPO row = new MarketIndicatorEventPO();
        JsonNode snapshot = node.path("snapshot");
        row.setTradeDate(localDate(root, "trade_date")); row.setCode(text(node, "code")); row.setName(text(node, "name")); row.setMarket(text(node, "market"));
        row.setEventType(text(node, "event_type")); row.setEventAt(offset(node, "event_at")); row.setSignalValue(decimal(node, "signal_value"));
        row.setLastPrice(decimal(snapshot, "last_price")); row.setChangePct(decimal(snapshot, "change_pct")); row.setLimitStatus(text(snapshot, "limit_status"));
        row.setSource(text(root, "source"));
        return row;
    }

    private MarketIndicatorIndustrySnapshotPO industry(JsonNode node) {
        MarketIndicatorIndustrySnapshotPO row = new MarketIndicatorIndustrySnapshotPO();
        row.setTradeDate(localDate(node, "trade_date")); row.setCapturedAt(offset(node, "captured_at")); row.setIndustryCode(text(node, "industry_code")); row.setIndustryName(text(node, "industry_name"));
        row.setStockCount(integer(node, "stock_count")); row.setAdvancingCount(integer(node, "advancing_count")); row.setDecliningCount(integer(node, "declining_count")); row.setFlatCount(integer(node, "flat_count"));
        row.setLimitUpCount(integer(node, "limit_up_count")); row.setLimitUpTouchCount(integer(node, "limit_up_touch_count")); row.setLimitDownCount(integer(node, "limit_down_count")); row.setLimitDownTouchCount(integer(node, "limit_down_touch_count"));
        row.setRapidRiseCount(integer(node, "rapid_rise_count")); row.setRapidFallCount(integer(node, "rapid_fall_count")); row.setFirstLimitUpAt(offset(node, "first_limit_up_at"));
        row.setFirstLimitUpCode(text(node, "first_limit_up_code")); row.setFirstLimitUpName(text(node, "first_limit_up_name")); row.setFirstLimitUpChangePct(decimal(node, "first_limit_up_change_pct")); row.setFirstEventAt(offset(node, "first_event_at"));
        row.setFirstEventCode(text(node, "first_event_code")); row.setFirstEventName(text(node, "first_event_name")); row.setFirstEventType(text(node, "first_event_type")); row.setFirstEventSignalValue(decimal(node, "first_event_signal_value")); row.setSource(text(node, "source"));
        return row;
    }

    private java.util.stream.Stream<JsonNode> rows(JsonNode root) { return stream(root.path("rows")); }
    private java.util.stream.Stream<JsonNode> events(JsonNode root) { return stream(root.path("events")); }
    private java.util.stream.Stream<JsonNode> industries(JsonNode root) { return stream(root.path("industry_rows")); }
    private java.util.stream.Stream<JsonNode> stream(JsonNode node) { return node.isArray() ? StreamSupport.stream(node.spliterator(), false) : java.util.stream.Stream.empty(); }
    private boolean positive(BigDecimal value) { return value != null && value.signum() > 0; }
    private boolean negative(BigDecimal value) { return value != null && value.signum() < 0; }
    private String text(JsonNode node, String field) { JsonNode value = node == null ? null : node.get(field); return value == null || value.isNull() ? null : value.asText(); }
    private Integer integer(JsonNode node, String field) { String value = text(node, field); return value == null ? null : Integer.valueOf(value); }
    private BigDecimal decimal(JsonNode node, String field) { String value = text(node, field); return value == null || value.isBlank() ? null : new BigDecimal(value); }
    private OffsetDateTime offset(JsonNode node, String field) {
        String value = text(node, field);
        if (value == null || value.isBlank()) return null;
        String normalized = value.replace("Z", "+00:00");
        if (normalized.length() > 10 && normalized.charAt(10) == ' ') normalized = normalized.substring(0, 10) + "T" + normalized.substring(11);
        return OffsetDateTime.parse(normalized);
    }
    private LocalDate localDate(JsonNode node, String field) { String value = text(node, field); return value == null || value.isBlank() ? null : LocalDate.parse(value.substring(0, 10)); }

    private Object plainValue(JsonNode value) {
        if (value == null || value.isNull()) return null;
        if (value.isTextual()) return value.asText();
        if (value.isBoolean()) return value.asBoolean();
        if (value.isNumber()) return value.numberValue();
        if (value.isArray()) {
            List<Object> values = new ArrayList<>();
            value.forEach(item -> values.add(plainValue(item)));
            return values;
        }
        if (value.isObject()) {
            Map<String, Object> values = new LinkedHashMap<>();
            value.fields().forEachRemaining(entry -> values.put(entry.getKey(), plainValue(entry.getValue())));
            return values;
        }
        return value.toString();
    }
}
