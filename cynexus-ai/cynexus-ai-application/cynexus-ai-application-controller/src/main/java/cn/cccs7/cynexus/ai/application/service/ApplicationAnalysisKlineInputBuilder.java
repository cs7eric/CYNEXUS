package cn.cccs7.cynexus.ai.application.service;

import cn.cccs7.cynexus.ai.domain.entity.ApplicationAnalysisPageBO;
import cn.cccs7.cynexus.ai.infra.tool.market.MarketDataTools;
import cn.cccs7.cynexus.market.api.dto.StockChartDTO;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ApplicationAnalysisKlineInputBuilder {

    private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
    private static final int RECENT30_DAYS = 30;
    private static final int WEEKLY_KEEP = 12;
    private static final int BENCHMARK_DAILY_KEEP = 10;
    private static final int BENCHMARK_WEEKLY_KEEP = 5;
    private static final Map<String, String> BENCHMARKS = Map.of(
            "000001", "上证指数",
            "399001", "深证成指",
            "000688", "科创50",
            "000300", "沪深300"
    );

    private final MarketDataTools marketDataTools;

    public ApplicationAnalysisKlineInputBuilder(MarketDataTools marketDataTools) {
        this.marketDataTools = marketDataTools;
    }

    public Map<String, Object> buildRecent30Input(ApplicationAnalysisPageBO.Target target) {
        String targetType = defaultString(target.getTargetType(), "stock");
        String symbol = defaultString(target.getSymbol(), "");
        String name = defaultString(target.getName(), symbol);
        String adjust = defaultString(target.getAdjust(), "qfq");
        StockChartDTO.KlineResponseDTO daily = loadBars(targetType, symbol, "1d", adjust);
        List<Map<String, Object>> dailyItems = normalizeBars(daily);
        List<Map<String, Object>> dailyTail = tail(dailyItems, RECENT30_DAYS);
        List<Map<String, Object>> weeklyTail = tail(aggregateToWeek(dailyItems), WEEKLY_KEEP);

        Map<String, Object> bars = new LinkedHashMap<>();
        bars.put("daily", Map.of(
                "period", "1d",
                "adjust", adjust,
                "total_count", dailyItems.size(),
                "recent_window_days", RECENT30_DAYS,
                "items", dailyTail
        ));
        bars.put("weekly", Map.of(
                "period", "1w",
                "adjust", adjust,
                "total_count", weeklyTail.size(),
                "items", weeklyTail
        ));

        Map<String, Object> input = new LinkedHashMap<>();
        input.put("target", Map.of(
                "target_type", targetType,
                "symbol", symbol,
                "name", name
        ));
        input.put("bars", bars);
        input.put("benchmark_bars", benchmarkBars(adjust));
        input.put("market_breadth_series", List.of());
        input.put("analysis_windows", List.of(3, 5, 10, 20));
        input.put("enabled_features", Map.of(
                "support_resistance", true,
                "trend_detection", true,
                "volume_price_analysis", true,
                "turnover_analysis", true,
                "pattern_candidates", true,
                "multi_index_resonance", true,
                "market_sentiment_overlay", true,
                "short_term_trend_assessment", true,
                "current_situation_assessment", true
        ));
        input.put("recent_window", Map.of("kind", "recent_30_daily_k", "days", RECENT30_DAYS));
        input.put("_sources", Map.of(
                "daily", source(daily),
                "daily_total_count", dailyItems.size(),
                "daily_kept", dailyTail.size(),
                "weekly", "aggregated_from_daily",
                "weekly_total_count", weeklyTail.size(),
                "weekly_kept", weeklyTail.size(),
                "market_breadth_series", "none",
                "market_breadth_series_kept", 0
        ));
        return input;
    }

    private Map<String, Object> benchmarkBars(String adjust) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, String> benchmark : BENCHMARKS.entrySet()) {
            StockChartDTO.KlineResponseDTO daily = loadBars("index", benchmark.getKey(), "1d", adjust);
            List<Map<String, Object>> dailyItems = normalizeBars(daily);
            List<Map<String, Object>> weeklyItems = aggregateToWeek(dailyItems);
            result.put(benchmark.getKey(), Map.of(
                    "name", benchmark.getValue(),
                    "daily", tail(dailyItems, BENCHMARK_DAILY_KEEP),
                    "weekly", tail(weeklyItems, BENCHMARK_WEEKLY_KEEP)
            ));
        }
        return result;
    }

    private StockChartDTO.KlineResponseDTO loadBars(String targetType, String symbol, String period, String adjust) {
        StockChartDTO.KlineResponseDTO response =
                marketDataTools.getStockChartKlines(targetType, symbol, period, adjust);
        if (response == null || response.getItems() == null) {
            return StockChartDTO.KlineResponseDTO.builder()
                    .symbol(symbol)
                    .targetType(targetType)
                    .period(period)
                    .adjust(adjust)
                    .source("cynexus-market-empty")
                    .items(List.of())
                    .build();
        }
        return response;
    }

    private List<Map<String, Object>> normalizeBars(StockChartDTO.KlineResponseDTO response) {
        if (response == null || response.getItems() == null) {
            return List.of();
        }
        return response.getItems().stream()
                .filter(item -> item != null && item.getClose() != null)
                .sorted(Comparator.comparingLong(this::timestamp))
                .map(this::barItem)
                .toList();
    }

    private Map<String, Object> barItem(StockChartDTO.KlineBarDTO item) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("timestamp", timestamp(item));
        result.put("open", decimal(item.getOpen()));
        result.put("high", decimal(item.getHigh()));
        result.put("low", decimal(item.getLow()));
        result.put("close", decimal(item.getClose()));
        result.put("volume", decimal(item.getVolume()));
        result.put("turnover", decimal(item.getTurnover()));
        result.put("turnover_rate", decimal(item.getTurnoverRate()));
        result.put("volume_ratio", decimal(item.getVolumeRatio()));
        return result;
    }

    private List<Map<String, Object>> aggregateToWeek(List<Map<String, Object>> dailyItems) {
        Map<String, List<Map<String, Object>>> groups = new LinkedHashMap<>();
        for (Map<String, Object> item : dailyItems) {
            long ts = number(item.get("timestamp")).longValue();
            String key = java.time.Instant.ofEpochMilli(ts).atZone(SHANGHAI)
                    .get(java.time.temporal.IsoFields.WEEK_BASED_YEAR)
                    + "-"
                    + java.time.Instant.ofEpochMilli(ts).atZone(SHANGHAI)
                    .get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR);
            groups.computeIfAbsent(key, ignored -> new java.util.ArrayList<>()).add(item);
        }
        return groups.values().stream()
                .filter(group -> !group.isEmpty())
                .map(this::weeklyBar)
                .toList();
    }

    private Map<String, Object> weeklyBar(List<Map<String, Object>> group) {
        Map<String, Object> first = group.getFirst();
        Map<String, Object> last = group.getLast();
        double high = group.stream().map(item -> number(item.get("high")).doubleValue()).max(Double::compareTo).orElse(0.0);
        double low = group.stream().map(item -> number(item.get("low")).doubleValue()).min(Double::compareTo).orElse(0.0);
        double volume = group.stream().mapToDouble(item -> number(item.get("volume")).doubleValue()).sum();
        double turnover = group.stream().mapToDouble(item -> number(item.get("turnover")).doubleValue()).sum();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("timestamp", last.get("timestamp"));
        result.put("open", first.get("open"));
        result.put("high", high);
        result.put("low", low);
        result.put("close", last.get("close"));
        result.put("volume", volume);
        result.put("turnover", turnover);
        return result;
    }

    private long timestamp(StockChartDTO.KlineBarDTO item) {
        if (item.getTimestamp() != null) {
            return item.getTimestamp();
        }
        if (item.getTradeDate() != null) {
            return item.getTradeDate().atTime(15, 0).atZone(SHANGHAI).toInstant().toEpochMilli();
        }
        return 0L;
    }

    private List<Map<String, Object>> tail(List<Map<String, Object>> items, int limit) {
        if (items == null || items.isEmpty() || limit <= 0) {
            return List.of();
        }
        int from = Math.max(0, items.size() - limit);
        return items.subList(from, items.size());
    }

    private Number decimal(BigDecimal value) {
        return value == null ? 0 : value;
    }

    private Number number(Object value) {
        return value instanceof Number number ? number : 0;
    }

    private String source(StockChartDTO.KlineResponseDTO value) {
        return value == null || value.getSource() == null ? "cynexus-market-empty" : value.getSource();
    }

    private String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
