package cn.cccs7.cynexus.market.domain.service.stock.impl;

import cn.cccs7.cynexus.common.core.constant.CommonConstants;
import cn.cccs7.cynexus.market.domain.entity.StockChartBO;
import cn.cccs7.cynexus.market.domain.entity.StockDailyFeatureBO;
import cn.cccs7.cynexus.market.domain.entity.StockIntradayFeatureBO;
import cn.cccs7.cynexus.market.domain.repository.StockChartQueryRepository;
import cn.cccs7.cynexus.market.domain.service.stock.StockChartQueryDomainService;
import cn.cccs7.cynexus.market.domain.stock.StockAuctionDomainPolicy;
import cn.cccs7.cynexus.market.domain.stock.StockChartDomainService;
import cn.cccs7.cynexus.market.domain.valueobject.Stock;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class StockChartQueryDomainServiceImpl implements StockChartQueryDomainService {

    private static final ZoneId SHANGHAI = ZoneId.of(CommonConstants.SHANGHAI_ZONE_ID);
    private static final String EMPTY_MINUTE_SOURCE = "clickhouse.ch_dwd_kline_1m-empty";
    private static final String EMPTY_AUCTION_SOURCE = "clickhouse.ch_ods_stock_auction_point-empty";

    private final StockChartQueryRepository repository;
    private final StockChartDomainService rules;
    private final StockAuctionDomainPolicy auctionPolicy;

    public StockChartQueryDomainServiceImpl(StockChartQueryRepository repository,
                                            StockChartDomainService rules) {
        this.repository = repository;
        this.rules = rules;
        this.auctionPolicy = new StockAuctionDomainPolicy();
    }

    @Override
    public List<StockChartBO.SearchItem> search(String keyword) {
        return repository.search(keyword, 20);
    }

    @Override
    public KlineResult klines(String targetType, String symbol, String period, String adjust) {
        String normalizedTargetType = targetType(targetType);
        String normalizedPeriod = rules.normalizePeriod(period);
        String normalizedAdjust = adjust(adjust);
        List<StockChartBO.Bar> rows;
        String source;
        if ("1d".equals(normalizedPeriod)) {
            rows = repository.listDailyBars(normalizedTargetType, symbol, rules.defaultKlineCount(), normalizedAdjust);
            boolean clickHouseQfq = "stock".equalsIgnoreCase(normalizedTargetType)
                    && "qfq".equalsIgnoreCase(normalizedAdjust);
            source = clickHouseQfq
                    ? source("clickhouse.daily_qfq", rows)
                    : source("java-tdx-local", rows);
        } else {
            int periodMinutes = rules.periodMinutes(normalizedPeriod);
            LocalDate effectiveDate = repository.findLatestMinuteDate(
                            normalizedTargetType, symbol, LocalDate.now(SHANGHAI))
                    .orElse(null);
            rows = effectiveDate == null
                    ? List.of()
                    : repository.listMinuteBars(normalizedTargetType, symbol, effectiveDate, periodMinutes);
            source = source("clickhouse.ch_dwd_kline_1m", rows);
        }
        return new KlineResult(symbol, normalizedTargetType, normalizedPeriod, normalizedAdjust, source, rows);
    }

    @Override
    public StockChartBO.StockMeta stockMeta(String symbol) {
        return repository.stockMeta(symbol);
    }

    @Override
    public StockChartBO.StockSectors stockSectors(String symbol) {
        return repository.stockSectors(symbol);
    }

    @Override
    public StockChartBO.FundFlowSnapshot fundFlow(String symbol) {
        return repository.fundFlow(symbol);
    }

    @Override
    public StockDailyFeatureBO dailyFeature(String symbol, String tradeDate) {
        String code = normalizeSymbol(symbol);
        List<StockChartBO.Bar> bars = repository.listDailyBars("stock", code, 520, "qfq");
        LocalDate requested = parseDate(tradeDate);
        StockChartBO.Bar current = bars.stream()
                .filter(item -> requested == null || requested.equals(item.getTradeDate()))
                .reduce((first, second) -> second).orElse(null);
        if (current == null || current.getTradeDate() == null || current.getClose() == null) return null;
        StockChartBO.Bar previous = bars.stream()
                .filter(item -> item.getTradeDate() != null && item.getTradeDate().isBefore(current.getTradeDate()))
                .reduce((first, second) -> second).orElse(null);
        BigDecimal changePercent = previous == null || previous.getClose() == null || previous.getClose().signum() == 0
                ? null
                : current.getClose().subtract(previous.getClose()).multiply(BigDecimal.valueOf(100))
                .divide(previous.getClose(), 4, RoundingMode.HALF_UP);
        StockChartBO.StockMeta meta = repository.stockMeta(code);
        return StockDailyFeatureBO.builder().stock(new Stock(code, valueOr(meta == null ? null : meta.getName(), code)))
                .tradeDate(current.getTradeDate().toString()).closePrice(current.getClose())
                .changePercent(changePercent).turnoverRate(current.getTurnoverRate())
                .volumeRatio(volumeRatio(bars, current)).mainNetInflow(null).trendLabel(label(changePercent)).build();
    }

    @Override
    public StockIntradayFeatureBO intradayFeature(String symbol, String tradeDate) {
        String code = normalizeSymbol(symbol);
        LocalDate requested = parseDate(tradeDate);
        LocalDate effectiveDate = repository.findLatestMinuteDate("stock", code,
                requested == null ? LocalDate.now(SHANGHAI) : requested).orElse(null);
        if (effectiveDate == null) return null;
        List<StockChartBO.Bar> bars = repository.listMinuteBars("stock", code, effectiveDate, 1);
        if (bars.isEmpty()) return null;
        StockChartBO.Bar first = bars.get(0);
        StockChartBO.Bar latest = bars.get(bars.size() - 1);
        BigDecimal high = bars.stream().map(StockChartBO.Bar::getHigh).filter(Objects::nonNull)
                .max(BigDecimal::compareTo).orElse(first.getClose());
        BigDecimal low = bars.stream().map(StockChartBO.Bar::getLow).filter(Objects::nonNull)
                .min(BigDecimal::compareTo).orElse(first.getClose());
        BigDecimal amplitude = first.getClose() == null || first.getClose().signum() == 0 || high == null || low == null
                ? null : high.subtract(low).multiply(BigDecimal.valueOf(100)).divide(first.getClose(), 4, RoundingMode.HALF_UP);
        BigDecimal volumeAcceleration = volumeAcceleration(bars);
        StockChartBO.StockMeta meta = repository.stockMeta(code);
        return StockIntradayFeatureBO.builder().stock(new Stock(code, valueOr(meta == null ? null : meta.getName(), code)))
                .tradeDate(effectiveDate.toString()).latestPrice(latest.getClose()).amplitude(amplitude)
                .volumeAcceleration(volumeAcceleration).mainNetInflow(null)
                .intradayLabel(volumeAcceleration != null && volumeAcceleration.compareTo(BigDecimal.ONE) > 0 ? "accelerating" : "stable")
                .build();
    }

    private BigDecimal volumeRatio(List<StockChartBO.Bar> bars, StockChartBO.Bar current) {
        List<BigDecimal> history = bars.stream().filter(item -> item.getTradeDate() != null && current.getTradeDate() != null
                        && item.getTradeDate().isBefore(current.getTradeDate())).map(StockChartBO.Bar::getVolume)
                .filter(Objects::nonNull).toList();
        if (history.isEmpty() || current.getVolume() == null) return null;
        int start = Math.max(0, history.size() - 20);
        BigDecimal average = history.subList(start, history.size()).stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(history.size() - start), 4, RoundingMode.HALF_UP);
        return average.signum() == 0 ? null : current.getVolume().divide(average, 4, RoundingMode.HALF_UP);
    }

    private BigDecimal volumeAcceleration(List<StockChartBO.Bar> bars) {
        if (bars.size() < 4) return null;
        int split = bars.size() / 2;
        BigDecimal first = bars.subList(0, split).stream().map(StockChartBO.Bar::getVolume)
                .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal second = bars.subList(split, bars.size()).stream().map(StockChartBO.Bar::getVolume)
                .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        return first.signum() == 0 ? null : second.divide(first, 4, RoundingMode.HALF_UP);
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

    @Override
    public StockChartBO.SectorStocks sectorStocks(String sectorId, Integer categoryRaw,
                                                   String topicId, String name, int limit) {
        return repository.sectorStocks(sectorId, categoryRaw, topicId, name, limit);
    }

    @Override
    public IntradayResult intraday(String targetType, String symbol, String name, String adjust,
                                   String tradeDate, List<String> periods) {
        String normalizedTargetType = targetType(targetType);
        String normalizedName = rules.defaultValue(name, symbol);
        String normalizedAdjust = adjust(adjust);
        LocalDate requested = rules.parseDateOrToday(tradeDate);
        LocalDate effectiveDate = repository.findLatestMinuteDate(
                        normalizedTargetType, symbol, requested)
                .orElse(null);
        if (effectiveDate == null) {
            return new IntradayResult(symbol, normalizedTargetType, normalizedName, normalizedAdjust,
                    "none", normalizedAdjust, EMPTY_MINUTE_SOURCE, null, requested,
                    List.of(), emptyMinuteBars(), "minute kline / timeshare data not found");
        }

        Map<String, List<StockChartBO.Bar>> minuteBars = new HashMap<>();
        for (String period : periods == null ? List.<String>of() : periods) {
            String normalizedPeriod = rules.normalizePeriod(period);
            minuteBars.put(normalizedPeriod, repository.listMinuteBars(
                    normalizedTargetType, symbol, effectiveDate, rules.periodMinutes(normalizedPeriod)));
        }
        List<StockChartBO.IntradayPoint> timeshare = repository.listTimeshare(
                normalizedTargetType, symbol, effectiveDate);
        boolean empty = timeshare.isEmpty() && minuteBars.values().stream().allMatch(List::isEmpty);
        return new IntradayResult(symbol, normalizedTargetType, normalizedName, normalizedAdjust,
                "none", normalizedAdjust, source("clickhouse.ch_dwd_kline_1m", empty),
                effectiveDate, requested, timeshare, minuteBars,
                empty ? "minute kline / timeshare data not found" : null);
    }

    @Override
    public AuctionResult auction(String symbol, String tradeDate) {
        LocalDate requested = rules.parseDateOrToday(tradeDate);
        LocalDate effectiveDate = repository.findLatestAuctionDate("stock", symbol, requested).orElse(null);
        return auctionForDate(symbol, requested, effectiveDate);
    }

    @Override
    public List<AuctionResult> auctionHistory(String symbol, int limit) {
        int resolvedLimit = rules.auctionHistoryLimit(limit);
        return repository.listAuctionDates("stock", symbol, resolvedLimit).stream()
                .map(date -> auctionForDate(symbol, date, date))
                .toList();
    }

    @Override
    public StockChartBO.F10Valuation valuation(String symbol, String reqId) {
        return repository.valuation(symbol, reqId);
    }

    @Override
    public StockChartBO.F10FinanceReport financeReport(String symbol, String reportType) {
        return repository.financeReport(symbol, reportType);
    }

    @Override
    public StockChartBO.F10BusinessComposition businessComposition(String symbol, String reportDate) {
        return repository.businessComposition(symbol, reportDate);
    }

    @Override
    public StockChartBO.F10ProfitForecast profitForecast(String symbol) {
        return repository.profitForecast(symbol);
    }

    @Override
    public StockChartBO.F10Announcements announcements(String symbol) {
        return repository.announcements(symbol);
    }

    @Override
    public StockChartBO.F10News news(String symbol) {
        return repository.news(symbol);
    }

    @Override
    public StockChartBO.F10Roadshows roadshows(String symbol) {
        return repository.roadshows(symbol);
    }

    @Override
    public StockChartBO.F10CompanyNews companyNews(String symbol, String section) {
        return repository.companyNews(symbol, section);
    }

    private AuctionResult auctionForDate(String symbol, LocalDate requested, LocalDate effectiveDate) {
        if (effectiveDate == null) {
            StockAuctionDomainPolicy.AuctionPhaseFacts emptyOpening = auctionPolicy.phase(List.of(), "09:25:00", "openingPoints");
            StockAuctionDomainPolicy.AuctionPhaseFacts emptyClosing = auctionPolicy.phase(List.of(), "15:00:00", "closingPoints");
            return new AuctionResult(symbol, requested, EMPTY_AUCTION_SOURCE, "auction detail data not found",
                    emptyOpening, emptyClosing, Map.of("has_auction_0925", false), List.of(), List.of(), List.of());
        }
        List<StockChartBO.AuctionPoint> rows = repository.listAuctionPoints("stock", symbol, effectiveDate);
        StockAuctionDomainPolicy.AuctionGroups groups = auctionPolicy.group(rows);
        List<StockChartBO.AuctionPoint> opening = groups.opening();
        List<StockChartBO.AuctionPoint> closing = groups.closing();
        return new AuctionResult(symbol, effectiveDate,
                source("clickhouse.ch_ods_stock_auction_point", rows),
                rows.isEmpty() ? "auction detail data not found" : null,
                auctionPolicy.phase(opening, "09:25:00", "openingPoints"),
                auctionPolicy.phase(closing, "15:00:00", "closingPoints"),
                auctionPolicy.auction0925(groups.auction0925()),
                opening.stream().map(auctionPolicy::point).toList(),
                closing.stream().map(auctionPolicy::point).toList(),
                rows.stream().map(auctionPolicy::point).toList());
    }

    private Map<String, List<StockChartBO.Bar>> emptyMinuteBars() {
        return Map.of("1m", List.of(), "5m", List.of(), "15m", List.of(),
                "30m", List.of(), "60m", List.of());
    }

    private String targetType(String value) {
        return rules.defaultValue(value, "stock").toLowerCase();
    }

    private String adjust(String value) {
        return rules.defaultValue(value, "qfq").toLowerCase();
    }

    private String source(String base, List<?> rows) {
        return source(base, rows == null || rows.isEmpty());
    }

    private String source(String base, boolean empty) {
        return empty ? base + "-empty" : base;
    }
}
