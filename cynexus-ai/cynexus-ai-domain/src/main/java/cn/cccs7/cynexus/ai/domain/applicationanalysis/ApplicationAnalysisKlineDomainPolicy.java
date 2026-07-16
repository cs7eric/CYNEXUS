package cn.cccs7.cynexus.ai.domain.applicationanalysis;

import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Window and weekly aggregation rules for application-analysis K-line input. */
public final class ApplicationAnalysisKlineDomainPolicy {
    private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
    private static final Map<String, String> BENCHMARKS = Map.of("000001", "上证指数", "399001", "深证成指", "000688", "科创50", "000300", "沪深300");
    public int recentDays() { return 30; }
    public int weeklyKeep() { return 12; }
    public int benchmarkDailyKeep() { return 10; }
    public int benchmarkWeeklyKeep() { return 5; }
    public Map<String, String> benchmarks() { return BENCHMARKS; }
    public List<Map<String, Object>> tail(List<Map<String, Object>> items, int limit) {
        if (items == null || items.isEmpty() || limit <= 0) return List.of();
        return items.subList(Math.max(0, items.size() - limit), items.size());
    }
    public List<Map<String, Object>> aggregateToWeek(List<Map<String, Object>> items) {
        Map<String, List<Map<String, Object>>> groups = new LinkedHashMap<>();
        for (Map<String, Object> item : items) {
            var date = Instant.ofEpochMilli(number(item.get("timestamp")).longValue()).atZone(SHANGHAI);
            String key = date.get(IsoFields.WEEK_BASED_YEAR) + "-" + date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
            groups.computeIfAbsent(key, ignored -> new ArrayList<>()).add(item);
        }
        return groups.values().stream().map(this::weeklyBar).toList();
    }
    private Map<String, Object> weeklyBar(List<Map<String, Object>> group) {
        Map<String, Object> first = group.getFirst(); Map<String, Object> last = group.getLast();
        double high = group.stream().map(item -> number(item.get("high")).doubleValue()).max(Double::compareTo).orElse(0.0);
        double low = group.stream().map(item -> number(item.get("low")).doubleValue()).min(Double::compareTo).orElse(0.0);
        double volume = group.stream().mapToDouble(item -> number(item.get("volume")).doubleValue()).sum();
        double turnover = group.stream().mapToDouble(item -> number(item.get("turnover")).doubleValue()).sum();
        Map<String, Object> result = new LinkedHashMap<>(); result.put("timestamp", last.get("timestamp")); result.put("open", first.get("open"));
        result.put("high", high); result.put("low", low); result.put("close", last.get("close")); result.put("volume", volume); result.put("turnover", turnover);
        return result;
    }
    private Number number(Object value) { return value instanceof Number number ? number : 0; }
}
