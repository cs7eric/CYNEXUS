package cn.cccs7.cynexus.ai.application.service;

import cn.cccs7.cynexus.ai.api.gateway.MarketDataGateway;
import cn.cccs7.cynexus.ai.domain.entity.ApplicationAnalysisPageBO;
import cn.cccs7.cynexus.ai.domain.applicationanalysis.ApplicationAnalysisKlineDomainPolicy;
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
    private final MarketDataGateway marketDataGateway;
    private final ApplicationAnalysisKlineDomainPolicy domainPolicy = new ApplicationAnalysisKlineDomainPolicy();

    public ApplicationAnalysisKlineInputBuilder(MarketDataGateway marketDataGateway) {
        this.marketDataGateway = marketDataGateway;
    }

    public Map<String, Object> buildRecent30Input(ApplicationAnalysisPageBO.Target target) {
        String targetType = defaultString(target.getTargetType(), "stock");
        String symbol = defaultString(target.getSymbol(), "");
        String name = defaultString(target.getName(), symbol);
        String adjust = defaultString(target.getAdjust(), "qfq");
        StockChartDTO.KlineResponseDTO daily = loadBars(targetType, symbol, "1d", adjust);
        List<Map<String, Object>> dailyItems = normalizeBars(daily);
        List<Map<String, Object>> dailyTail = domainPolicy.tail(dailyItems, domainPolicy.recentDays());
        List<Map<String, Object>> weeklyTail = domainPolicy.tail(domainPolicy.aggregateToWeek(dailyItems), domainPolicy.weeklyKeep());

        Map<String, Object> bars = new LinkedHashMap<>();
        bars.put("daily", Map.of(
                "period", "1d",
                "adjust", adjust,
                "total_count", dailyItems.size(),
                "recent_window_days", domainPolicy.recentDays(),
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
        input.put("recent_window", Map.of("kind", "recent_30_daily_k", "days", domainPolicy.recentDays()));
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
        for (Map.Entry<String, String> benchmark : domainPolicy.benchmarks().entrySet()) {
            StockChartDTO.KlineResponseDTO daily = loadBars("index", benchmark.getKey(), "1d", adjust);
            List<Map<String, Object>> dailyItems = normalizeBars(daily);
            List<Map<String, Object>> weeklyItems = domainPolicy.aggregateToWeek(dailyItems);
            result.put(benchmark.getKey(), Map.of(
                    "name", benchmark.getValue(),
                    "daily", domainPolicy.tail(dailyItems, domainPolicy.benchmarkDailyKeep()),
                    "weekly", domainPolicy.tail(weeklyItems, domainPolicy.benchmarkWeeklyKeep())
            ));
        }
        return result;
    }

    private StockChartDTO.KlineResponseDTO loadBars(String targetType, String symbol, String period, String adjust) {
        StockChartDTO.KlineResponseDTO response =
                marketDataGateway.getStockChartKlines(targetType, symbol, period, adjust);
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

    private long timestamp(StockChartDTO.KlineBarDTO item) {
        if (item.getTimestamp() != null) {
            return item.getTimestamp();
        }
        if (item.getTradeDate() != null) {
            return item.getTradeDate().atTime(15, 0).atZone(SHANGHAI).toInstant().toEpochMilli();
        }
        return 0L;
    }

    private Number decimal(BigDecimal value) {
        return value == null ? 0 : value;
    }

    private String source(StockChartDTO.KlineResponseDTO value) {
        return value == null || value.getSource() == null ? "cynexus-market-empty" : value.getSource();
    }

    private String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
