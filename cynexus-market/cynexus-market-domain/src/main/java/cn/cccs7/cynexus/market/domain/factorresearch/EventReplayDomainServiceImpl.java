package cn.cccs7.cynexus.market.domain.factorresearch;

import cn.cccs7.cynexus.market.domain.entity.MarketIndicatorReplayBO;
import cn.cccs7.cynexus.market.domain.entity.StockChartBO;
import cn.cccs7.cynexus.market.domain.service.indicator.MarketIndicatorQueryDomainService;
import cn.cccs7.cynexus.market.domain.service.stock.StockChartQueryDomainService;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

public final class EventReplayDomainServiceImpl implements EventReplayDomainService {
    private static final ZoneId MARKET_ZONE = ZoneId.of("Asia/Shanghai");
    private final StockChartQueryDomainService stockChartService;
    private final MarketIndicatorQueryDomainService indicatorService;

    public EventReplayDomainServiceImpl(StockChartQueryDomainService stockChartService,
                                        MarketIndicatorQueryDomainService indicatorService) {
        this.stockChartService = stockChartService;
        this.indicatorService = indicatorService;
    }

    @Override
    public EventReplayBO.Result replay(EventReplayBO.Query query) {
        if (query == null || query.code() == null || query.code().isBlank() || query.tradeDate() == null) {
            throw new IllegalArgumentException("code and tradeDate are required");
        }
        int limit = Math.max(10, Math.min(500, query.minuteLimit() <= 0 ? 240 : query.minuteLimit()));
        OffsetDateTime anchorAt = query.anchorAt() == null
                ? query.tradeDate().atTime(LocalTime.of(15, 0)).atZone(MARKET_ZONE).toOffsetDateTime()
                : query.anchorAt().atZoneSameInstant(MARKET_ZONE).toOffsetDateTime();
        if (!anchorAt.toLocalDate().equals(query.tradeDate())) {
            throw new IllegalArgumentException("anchorAt must be on tradeDate in Asia/Shanghai");
        }
        LocalTime from = anchorAt.toLocalTime().minusMinutes(Math.min(120, limit));
        if (from.isBefore(LocalTime.of(9, 30))) from = LocalTime.of(9, 30);
        MarketIndicatorReplayBO marketReplay = indicatorService.replayTimeline(query.tradeDate(), from,
                anchorAt.toLocalTime(), null, null, limit);
        StockChartQueryDomainService.IntradayResult stock = stockChartService.intraday("stock", query.code().trim(), null,
                "none", query.tradeDate().toString(), List.of("1m"), true);
        List<StockChartBO.Bar> rawStockBars = stock.minuteBars().getOrDefault("1m", List.of());
        long fromEpochMillis = query.tradeDate().atTime(from).atZone(MARKET_ZONE).toInstant().toEpochMilli();
        long anchorEpochMillis = anchorAt.toInstant().toEpochMilli();
        List<StockChartBO.Bar> stockBars = rawStockBars.stream()
                .filter(bar -> withinAnchorWindow(bar, query.tradeDate(), fromEpochMillis, anchorEpochMillis))
                .limit(limit)
                .toList();
        List<String> warnings = new ArrayList<>();
        if (stock.error() != null) warnings.add(stock.error());
        if (marketReplay.getMessage() != null) warnings.add(marketReplay.getMessage());
        if (!rawStockBars.isEmpty() && stockBars.isEmpty()) {
            warnings.add("stock 1m bars are outside the requested anchor window or have no timestamp");
        }
        boolean marketAvailable = "ready".equalsIgnoreCase(marketReplay.getDataStatus());
        boolean industryAvailable = marketReplay.getIndustryPoints() != null && !marketReplay.getIndustryPoints().isEmpty();
        boolean stockAvailable = !stockBars.isEmpty();
        String status = stockAvailable && marketAvailable && industryAvailable ? "AVAILABLE" : "PARTIAL";
        EventReplayBO.Coverage coverage = new EventReplayBO.Coverage(rawStockBars.size(), stockBars.size(),
                marketAvailable, industryAvailable, stockAvailable);
        String source = (stock.source() == null ? "clickhouse.ch_dwd_kline_1m" : stock.source())
                + "+" + (marketReplay.getSource() == null ? "postgres.trade_review" : marketReplay.getSource());
        return new EventReplayBO.Result(query.code().trim(), query.tradeDate(), anchorAt, status, source,
                anchorAt, coverage, warnings, stockBars, marketReplay);
    }

    private boolean withinAnchorWindow(StockChartBO.Bar bar, LocalDate tradeDate,
                                       long fromEpochMillis, long anchorEpochMillis) {
        if (bar == null || !tradeDate.equals(bar.getTradeDate()) || bar.getTimestamp() == null) return false;
        return bar.getTimestamp() >= fromEpochMillis && bar.getTimestamp() <= anchorEpochMillis;
    }
}
