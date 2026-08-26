package cn.cccs7.cynexus.market.infra.client;

import cn.cccs7.cynexus.market.domain.entity.screener.StockScreenerQueryBO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Reads only the bounded recent OHLCV history needed for a requested Screener pattern. */
@Component
public class PostgresStockScreenerPriceVolumePatternClient {

    private static final String FACT_SOURCE = "clickhouse.daily_raw";
    private final JdbcTemplate jdbcTemplate;

    public PostgresStockScreenerPriceVolumePatternClient(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, StockScreenerQueryBO.PriceVolumePatternMetrics> findOnTradeDate(
            LocalDate tradeDate, StockScreenerQueryBO.PriceVolumePattern pattern) {
        if (tradeDate == null || pattern == null || !pattern.enabled()) return Map.of();
        int requiredPriorSessions = pattern.volumeContractionDays() == null ? 0 : pattern.volumeContractionDays();
        int rowLimit = Math.max(1, requiredPriorSessions + 1);
        if (pattern.minCloseAboveMa20Pct() != null || pattern.minMa5AboveMa20Pct() != null || pattern.maxAtrPct20() != null) {
            rowLimit = Math.max(rowLimit, 20);
        }
        if (pattern.breakoutHighDays() != null) rowLimit = Math.max(rowLimit, pattern.breakoutHighDays() + 1);
        if (pattern.minConsecutiveBullishDays() != null) rowLimit = Math.max(rowLimit, pattern.minConsecutiveBullishDays());
        if (pattern.minVolumeVsMa5Multiple() != null) rowLimit = Math.max(rowLimit, 6);
        if (pattern.maxVolumeVsMa5Multiple() != null) rowLimit = Math.max(rowLimit, 6);
        if (pattern.candlestickPattern() != null) rowLimit = Math.max(rowLimit, 2);
        Map<String, List<DailyFact>> historyByCode = jdbcTemplate.query(
                """
                with ranked as (
                    select f.code, f.trade_date, f.open_price, f.high_price, f.low_price, f.close_price, f.prev_close, f.volume,
                           row_number() over (partition by f.code order by f.trade_date desc) as session_rank
                       from cynexus_appl_market.a_stock_daily_screening_fact f
                      where f.source = ? and f.trade_date <= ?
                )
                    select code, trade_date, open_price, high_price, low_price, close_price, prev_close, volume, session_rank
                  from ranked
                 where session_rank <= ?
                 order by code, session_rank
                """,
                resultSet -> {
                    Map<String, List<DailyFact>> values = new LinkedHashMap<>();
                    while (resultSet.next()) {
                        values.computeIfAbsent(resultSet.getString("code"), ignored -> new ArrayList<>()).add(
                                new DailyFact(resultSet.getObject("trade_date", LocalDate.class),
                                        resultSet.getBigDecimal("open_price"), resultSet.getBigDecimal("high_price"),
                                        resultSet.getBigDecimal("low_price"), resultSet.getBigDecimal("close_price"),
                                        resultSet.getBigDecimal("prev_close"), resultSet.getBigDecimal("volume")));
                    }
                    return values;
                }, FACT_SOURCE, tradeDate, rowLimit);
        Map<String, StockScreenerQueryBO.PriceVolumePatternMetrics> metrics = new LinkedHashMap<>();
        historyByCode.forEach((code, history) -> {
            if (!history.isEmpty() && tradeDate.equals(history.getFirst().tradeDate())) {
                metrics.put(code, metrics(history, requiredPriorSessions));
            }
        });
        return metrics;
    }

    private StockScreenerQueryBO.PriceVolumePatternMetrics metrics(List<DailyFact> history, int requiredPriorSessions) {
        if (history == null || history.isEmpty()) return null;
        DailyFact current = history.getFirst();
        BigDecimal bullishBodyPct = percentage(current.closePrice().subtract(current.openPrice()), current.prevClose());
        int observedContraction = 0;
        BigDecimal breakoutMultiple = null;
        if (requiredPriorSessions > 0 && history.size() >= requiredPriorSessions + 1 && positive(current.volume())) {
            List<DailyFact> previous = history.subList(1, requiredPriorSessions + 1);
            observedContraction = contractionSessions(previous);
            BigDecimal priorAverage = averageVolume(previous);
            breakoutMultiple = ratio(current.volume(), priorAverage);
        }
        BigDecimal closeAboveMa20Pct = null;
        BigDecimal ma5AboveMa20Pct = null;
        if (history.size() >= 20 && positive(current.closePrice())) {
            BigDecimal ma20 = averageClose(history.subList(0, 20));
            BigDecimal ma5 = averageClose(history.subList(0, 5));
            if (ma20 != null) {
                closeAboveMa20Pct = percentage(current.closePrice().subtract(ma20), ma20);
                if (ma5 != null) ma5AboveMa20Pct = percentage(ma5.subtract(ma20), ma20);
            }
        }
        Integer breakoutHighDays = breakoutHighDays(history, current.closePrice());
        BigDecimal atrPct20 = history.size() >= 20 ? atrPct(history.subList(0, 20), current.closePrice()) : null;
        Integer consecutiveBullishDays = consecutiveBullishDays(history);
        BigDecimal volumeVsMa5Multiple = history.size() >= 6 && positive(current.volume())
                ? ratio(current.volume(), averageVolume(history.subList(1, 6))) : null;
        return new StockScreenerQueryBO.PriceVolumePatternMetrics(observedContraction, breakoutMultiple, bullishBodyPct,
                closeAboveMa20Pct, ma5AboveMa20Pct, breakoutHighDays, atrPct20, consecutiveBullishDays, volumeVsMa5Multiple,
                candlestickPatterns(history));
    }

    private int contractionSessions(List<DailyFact> previous) {
        if (previous.isEmpty() || !positive(previous.getFirst().volume())) return 0;
        int count = 1;
        for (int index = 1; index < previous.size(); index++) {
            BigDecimal newer = previous.get(index - 1).volume();
            BigDecimal older = previous.get(index).volume();
            if (!positive(older) || newer.compareTo(older) >= 0) break;
            count++;
        }
        return count;
    }

    private BigDecimal averageVolume(List<DailyFact> values) {
        BigDecimal sum = BigDecimal.ZERO;
        for (DailyFact value : values) {
            if (!positive(value.volume())) return null;
            sum = sum.add(value.volume());
        }
        return sum.divide(BigDecimal.valueOf(values.size()), 8, RoundingMode.HALF_UP);
    }

    private BigDecimal averageClose(List<DailyFact> values) {
        BigDecimal sum = BigDecimal.ZERO;
        for (DailyFact value : values) {
            if (!positive(value.closePrice())) return null;
            sum = sum.add(value.closePrice());
        }
        return sum.divide(BigDecimal.valueOf(values.size()), 8, RoundingMode.HALF_UP);
    }

    private Integer breakoutHighDays(List<DailyFact> history, BigDecimal closePrice) {
        if (!positive(closePrice)) return null;
        int count = 0;
        for (int index = 1; index < history.size(); index++) {
            BigDecimal high = history.get(index).highPrice();
            if (!positive(high) || closePrice.compareTo(high) < 0) break;
            count++;
        }
        return count;
    }

    private BigDecimal atrPct(List<DailyFact> values, BigDecimal closePrice) {
        if (!positive(closePrice)) return null;
        BigDecimal sum = BigDecimal.ZERO;
        for (DailyFact value : values) {
            if (!positive(value.highPrice()) || !positive(value.lowPrice()) || !positive(value.prevClose())) return null;
            BigDecimal trueRange = value.highPrice().subtract(value.lowPrice())
                    .max(value.highPrice().subtract(value.prevClose()).abs())
                    .max(value.lowPrice().subtract(value.prevClose()).abs());
            sum = sum.add(trueRange);
        }
        return percentage(sum.divide(BigDecimal.valueOf(values.size()), 8, RoundingMode.HALF_UP), closePrice);
    }

    private Integer consecutiveBullishDays(List<DailyFact> values) {
        int count = 0;
        for (DailyFact value : values) {
            if (!positive(value.openPrice()) || !positive(value.closePrice()) || value.closePrice().compareTo(value.openPrice()) <= 0) break;
            count++;
        }
        return count;
    }

    private List<String> candlestickPatterns(List<DailyFact> history) {
        if (history.isEmpty()) return List.of();
        DailyFact current = history.getFirst();
        if (!positive(current.openPrice()) || !positive(current.highPrice()) || !positive(current.lowPrice())
                || !positive(current.closePrice())) return List.of();
        BigDecimal range = current.highPrice().subtract(current.lowPrice());
        if (range.signum() <= 0) return List.of();
        BigDecimal body = current.closePrice().subtract(current.openPrice()).abs();
        BigDecimal lowerShadow = current.openPrice().min(current.closePrice()).subtract(current.lowPrice());
        BigDecimal upperShadow = current.highPrice().subtract(current.openPrice().max(current.closePrice()));
        List<String> patterns = new ArrayList<>();
        if (body.multiply(BigDecimal.TEN).compareTo(range) <= 0) patterns.add("doji");
        if (body.signum() > 0 && lowerShadow.compareTo(body.multiply(BigDecimal.valueOf(2))) >= 0
                && upperShadow.compareTo(body) <= 0) patterns.add("hammer");
        if (body.signum() > 0 && upperShadow.compareTo(body.multiply(BigDecimal.valueOf(2))) >= 0
                && lowerShadow.compareTo(body) <= 0) patterns.add("shooting_star");
        if (history.size() < 2) return List.copyOf(patterns);
        DailyFact previous = history.get(1);
        if (!positive(previous.openPrice()) || !positive(previous.highPrice()) || !positive(previous.lowPrice()) || !positive(previous.closePrice())) {
            return List.copyOf(patterns);
        }
        if (current.closePrice().compareTo(current.openPrice()) > 0 && previous.closePrice().compareTo(previous.openPrice()) < 0
                && current.openPrice().compareTo(previous.closePrice()) <= 0 && current.closePrice().compareTo(previous.openPrice()) >= 0) {
            patterns.add("bullish_engulfing");
        }
        if (current.closePrice().compareTo(current.openPrice()) < 0 && previous.closePrice().compareTo(previous.openPrice()) > 0
                && current.openPrice().compareTo(previous.closePrice()) >= 0 && current.closePrice().compareTo(previous.openPrice()) <= 0) {
            patterns.add("bearish_engulfing");
        }
        if (current.lowPrice().compareTo(previous.highPrice()) > 0) patterns.add("gap_up");
        if (current.highPrice().compareTo(previous.lowPrice()) < 0) patterns.add("gap_down");
        if (current.highPrice().compareTo(previous.highPrice()) <= 0 && current.lowPrice().compareTo(previous.lowPrice()) >= 0) patterns.add("inside_bar");
        return List.copyOf(patterns);
    }

    private BigDecimal ratio(BigDecimal numerator, BigDecimal denominator) {
        if (numerator == null || denominator == null || denominator.signum() <= 0) return null;
        return numerator.divide(denominator, 8, RoundingMode.HALF_UP);
    }

    private BigDecimal percentage(BigDecimal numerator, BigDecimal denominator) {
        BigDecimal value = ratio(numerator, denominator);
        return value == null ? null : value.multiply(BigDecimal.valueOf(100));
    }

    private boolean positive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }

    private record DailyFact(LocalDate tradeDate, BigDecimal openPrice, BigDecimal highPrice, BigDecimal lowPrice, BigDecimal closePrice,
                             BigDecimal prevClose, BigDecimal volume) {}
}
