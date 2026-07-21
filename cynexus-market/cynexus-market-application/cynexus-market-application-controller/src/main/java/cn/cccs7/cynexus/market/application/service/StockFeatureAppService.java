package cn.cccs7.cynexus.market.application.service;

import cn.cccs7.cynexus.market.domain.entity.StockDailyFeatureBO;
import cn.cccs7.cynexus.market.domain.entity.StockIntradayFeatureBO;
import cn.cccs7.cynexus.market.domain.entity.StockChartBO;
import cn.cccs7.cynexus.market.domain.repository.StockChartQueryRepository;
import cn.cccs7.cynexus.market.domain.valueobject.Stock;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class StockFeatureAppService {

    private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
    private final StockChartQueryRepository stockChartQueryRepository;

    public StockFeatureAppService(StockChartQueryRepository stockChartQueryRepository) {
        this.stockChartQueryRepository = stockChartQueryRepository;
    }

    public StockDailyFeatureBO getDailyFeature(String symbol, String tradeDate) {
        String code = normalizeSymbol(symbol);
        List<StockChartBO.Bar> bars = stockChartQueryRepository.listDailyBars("stock", code, 520, "qfq");
        LocalDate requested = parseDate(tradeDate);
        StockChartBO.Bar current = bars.stream()
                .filter(item -> requested == null || requested.equals(item.getTradeDate()))
                .reduce((first, second) -> second)
                .orElse(null);
        if (current == null || current.getTradeDate() == null || current.getClose() == null) return null;
        StockChartBO.Bar previous = bars.stream()
                .filter(item -> item.getTradeDate() != null && item.getTradeDate().isBefore(current.getTradeDate()))
                .reduce((first, second) -> second)
                .orElse(null);
        BigDecimal changePercent = previous == null || previous.getClose() == null || previous.getClose().signum() == 0
                ? null
                : current.getClose().subtract(previous.getClose()).multiply(BigDecimal.valueOf(100))
                .divide(previous.getClose(), 4, java.math.RoundingMode.HALF_UP);
        StockChartBO.StockMeta meta = stockChartQueryRepository.stockMeta(code);
        return StockDailyFeatureBO.builder()
                .stock(new Stock(code, valueOr(meta == null ? null : meta.getName(), code)))
                .tradeDate(current.getTradeDate().toString())
                .closePrice(current.getClose())
                .changePercent(changePercent)
                .turnoverRate(current.getTurnoverRate())
                .volumeRatio(volumeRatio(bars, current))
                .mainNetInflow(null)
                .trendLabel(label(changePercent))
                .build();
    }

    public StockIntradayFeatureBO getIntradayFeature(String symbol, String tradeDate) {
        String code = normalizeSymbol(symbol);
        LocalDate requested = parseDate(tradeDate);
        LocalDate effectiveDate = stockChartQueryRepository.findLatestMinuteDate("stock", code,
                requested == null ? LocalDate.now(SHANGHAI) : requested).orElse(null);
        if (effectiveDate == null) return null;
        List<StockChartBO.Bar> bars = stockChartQueryRepository.listMinuteBars("stock", code, effectiveDate, 1);
        if (bars.isEmpty()) return null;
        StockChartBO.Bar first = bars.get(0);
        StockChartBO.Bar latest = bars.get(bars.size() - 1);
        BigDecimal high = bars.stream().map(StockChartBO.Bar::getHigh).filter(java.util.Objects::nonNull)
                .max(BigDecimal::compareTo).orElse(first.getClose());
        BigDecimal low = bars.stream().map(StockChartBO.Bar::getLow).filter(java.util.Objects::nonNull)
                .min(BigDecimal::compareTo).orElse(first.getClose());
        BigDecimal amplitude = first.getClose() == null || first.getClose().signum() == 0 || high == null || low == null
                ? null : high.subtract(low).multiply(BigDecimal.valueOf(100))
                .divide(first.getClose(), 4, java.math.RoundingMode.HALF_UP);
        BigDecimal volumeAcceleration = volumeAcceleration(bars);
        StockChartBO.StockMeta meta = stockChartQueryRepository.stockMeta(code);
        return StockIntradayFeatureBO.builder()
                .stock(new Stock(code, valueOr(meta == null ? null : meta.getName(), code)))
                .tradeDate(effectiveDate.toString())
                .latestPrice(latest.getClose())
                .amplitude(amplitude)
                .volumeAcceleration(volumeAcceleration)
                .mainNetInflow(null)
                .intradayLabel(volumeAcceleration != null && volumeAcceleration.compareTo(BigDecimal.ONE) > 0 ? "accelerating" : "stable")
                .build();
    }

    private BigDecimal volumeRatio(List<StockChartBO.Bar> bars, StockChartBO.Bar current) {
        List<BigDecimal> history = bars.stream()
                .filter(item -> item.getTradeDate() != null && current.getTradeDate() != null
                        && item.getTradeDate().isBefore(current.getTradeDate()))
                .map(StockChartBO.Bar::getVolume).filter(java.util.Objects::nonNull).toList();
        if (history.isEmpty() || current.getVolume() == null) return null;
        int start = Math.max(0, history.size() - 20);
        BigDecimal average = history.subList(start, history.size()).stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(history.size() - start), 4, java.math.RoundingMode.HALF_UP);
        return average.signum() == 0 ? null : current.getVolume().divide(average, 4, java.math.RoundingMode.HALF_UP);
    }

    private BigDecimal volumeAcceleration(List<StockChartBO.Bar> bars) {
        if (bars.size() < 4) return null;
        int split = bars.size() / 2;
        BigDecimal first = bars.subList(0, split).stream().map(StockChartBO.Bar::getVolume)
                .filter(java.util.Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal second = bars.subList(split, bars.size()).stream().map(StockChartBO.Bar::getVolume)
                .filter(java.util.Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        return first.signum() == 0 ? null : second.divide(first, 4, java.math.RoundingMode.HALF_UP);
    }

    private LocalDate parseDate(String value) {
        return value == null || value.isBlank() ? null : LocalDate.parse(value);
    }

    private String normalizeSymbol(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Stock symbol is required");
        return value.trim().toUpperCase();
    }

    private String valueOr(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String label(BigDecimal changePercent) {
        if (changePercent == null) return "unavailable";
        return changePercent.signum() > 0 ? "strong" : changePercent.signum() < 0 ? "weak" : "flat";
    }
}
