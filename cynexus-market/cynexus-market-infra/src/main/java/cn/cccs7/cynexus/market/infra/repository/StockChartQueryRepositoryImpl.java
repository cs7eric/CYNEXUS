package cn.cccs7.cynexus.market.infra.repository;

import cn.cccs7.cynexus.market.domain.entity.IndustryApplicationBO;
import cn.cccs7.cynexus.market.domain.entity.StockChartBO;
import cn.cccs7.cynexus.market.domain.repository.StockChartQueryRepository;
import cn.cccs7.cynexus.market.infra.basic.entity.StockBasicPO;
import cn.cccs7.cynexus.market.infra.basic.entity.StockFundFlowSnapshotPO;
import cn.cccs7.cynexus.market.infra.basic.service.StockBasicBasicService;
import cn.cccs7.cynexus.market.infra.basic.service.StockFundFlowSnapshotBasicService;
import cn.cccs7.cynexus.market.infra.client.ClickHouseStockChartClient;
import cn.cccs7.cynexus.market.infra.client.TdxLocalDayKlineClient;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Repository;

@Repository
public class StockChartQueryRepositoryImpl implements StockChartQueryRepository {

    private final StockBasicBasicService stockBasicBasicService;
    private final StockFundFlowSnapshotBasicService stockFundFlowSnapshotBasicService;
    private final TdxLocalDayKlineClient tdxLocalDayKlineClient;
    private final ClickHouseStockChartClient clickHouseStockChartClient;

    public StockChartQueryRepositoryImpl(StockBasicBasicService stockBasicBasicService,
                                         StockFundFlowSnapshotBasicService stockFundFlowSnapshotBasicService,
                                         TdxLocalDayKlineClient tdxLocalDayKlineClient,
                                         ClickHouseStockChartClient clickHouseStockChartClient) {
        this.stockBasicBasicService = stockBasicBasicService;
        this.stockFundFlowSnapshotBasicService = stockFundFlowSnapshotBasicService;
        this.tdxLocalDayKlineClient = tdxLocalDayKlineClient;
        this.clickHouseStockChartClient = clickHouseStockChartClient;
    }

    @Override
    public List<StockChartBO.SearchItem> search(String keyword, int limit) {
        String q = keyword == null ? "" : keyword.trim();
        if (q.isBlank()) {
            return List.of();
        }
        int max = Math.max(limit, 1);
        List<StockChartBO.SearchItem> items = new ArrayList<>(stockBasicBasicService.searchForStockChart(q, max).stream()
                .map(row -> StockChartBO.SearchItem.builder()
                        .targetType("stock")
                        .symbol(row.getCode())
                        .name(row.getName())
                        .build())
                .toList());
        Set<String> seen = new LinkedHashSet<>();
        for (StockChartBO.SearchItem item : items) {
            seen.add(item.getSymbol());
        }
        if (items.size() < max) {
            for (String code : tdxLocalDayKlineClient.searchLocalDaySymbols(q, max)) {
                if (seen.add(code)) {
                    items.add(StockChartBO.SearchItem.builder()
                            .targetType("stock")
                            .symbol(code)
                            .name(code)
                            .build());
                }
                if (items.size() >= max) {
                    break;
                }
            }
        }
        return items;
    }

    @Override
    public List<StockChartBO.Bar> listDailyBars(String targetType, String symbol, int limit) {
        String resolvedSymbol = normalizeTdxSymbol(targetType, symbol);
        return tdxLocalDayKlineClient.readDailyBars(resolvedSymbol, limit).stream()
                .map(this::dailyBar)
                .toList();
    }

    @Override
    public Optional<LocalDate> findLatestMinuteDate(String targetType, String symbol, LocalDate requestedDate) {
        MarketSymbol marketSymbol = normalizeMarketSymbol(targetType, symbol);
        return clickHouseStockChartClient.findLatestMinuteDate(marketSymbol.market(), marketSymbol.symbol(), requestedDate);
    }

    @Override
    public List<StockChartBO.Bar> listMinuteBars(String targetType, String symbol, LocalDate tradeDate, int periodMinutes) {
        MarketSymbol marketSymbol = normalizeMarketSymbol(targetType, symbol);
        return clickHouseStockChartClient.listMinuteBars(marketSymbol.market(), marketSymbol.symbol(), tradeDate, periodMinutes).stream()
                .map(row -> StockChartBO.Bar.builder()
                        .timestamp(row.getTimestamp())
                        .tradeDate(row.getTradeDate())
                        .open(row.getOpen())
                        .high(row.getHigh())
                        .low(row.getLow())
                        .close(row.getClose())
                        .volume(row.getVolume())
                        .turnover(row.getTurnover())
                        .build())
                .toList();
    }

    @Override
    public List<StockChartBO.IntradayPoint> listTimeshare(String targetType, String symbol, LocalDate tradeDate) {
        MarketSymbol marketSymbol = normalizeMarketSymbol(targetType, symbol);
        return clickHouseStockChartClient.listTimeshare(marketSymbol.market(), marketSymbol.symbol(), tradeDate).stream()
                .map(row -> StockChartBO.IntradayPoint.builder()
                        .timestamp(row.getTimestamp())
                        .tradeDate(row.getTradeDate())
                        .timeLabel(row.getTimeLabel())
                        .price(row.getPrice())
                        .open(row.getOpen())
                        .avgPrice(row.getAvgPrice())
                        .volume(row.getVolume())
                        .turnover(row.getTurnover())
                        .build())
                .toList();
    }

    @Override
    public StockChartBO.StockMeta stockMeta(String symbol) {
        String code = normalizeCode(symbol);
        StockBasicPO row = stockBasicBasicService.findByCodeForStockChart(code);
        if (row == null) {
            return StockChartBO.StockMeta.builder()
                    .symbol(code)
                    .name(code)
                    .build();
        }
        return StockChartBO.StockMeta.builder()
                .symbol(row.getCode())
                .name(row.getName())
                .totalMarketCap(row.getTotalMarketCap())
                .circMarketCap(row.getFloatMarketCap())
                .industry(row.getIndustry())
                .capStyle(row.getCapStyle())
                .sectorIndexSymbol(row.getSectorIndexSymbol())
                .sectorIndexName(row.getSectorIndexName())
                .build();
    }

    @Override
    public StockChartBO.FundFlowSnapshot fundFlow(String symbol) {
        String code = normalizeCode(symbol);
        StockFundFlowSnapshotPO latest = stockFundFlowSnapshotBasicService.findLatestByCode(code);
        if (latest == null) {
            return fundFlowDailyFallback(code);
        }
        List<StockChartBO.FundFlowPoint> history = stockFundFlowSnapshotBasicService.listRecentByCode(code, 30).stream()
                .map(row -> StockChartBO.FundFlowPoint.builder()
                        .tradeDate(row.getTradeDate())
                        .activeNetAmountWan(row.getActiveNetAmountWan())
                        .activeBuyRatio(row.getActiveBuyRatio())
                        .activeSellRatio(row.getActiveSellRatio())
                        .amountWan(row.getAmountWan())
                        .changePct(row.getChangePct())
                        .build())
                .toList();
        return StockChartBO.FundFlowSnapshot.builder()
                .ok(true)
                .symbol(code)
                .name(latest.getName())
                .tradeDate(latest.getTradeDate())
                .lastPrice(latest.getLastPrice())
                .changePct(latest.getChangePct())
                .amountWan(latest.getAmountWan())
                .turnoverRate(latest.getTurnoverRate())
                .activeNetAmountWan(latest.getActiveNetAmountWan())
                .activeBuyRatio(latest.getActiveBuyRatio())
                .activeSellRatio(latest.getActiveSellRatio())
                .buyBigRatio(latest.getBuyBigRatio())
                .sellBigRatio(latest.getSellBigRatio())
                .source(latest.getSource())
                .capturedAt(latest.getCapturedAt())
                .history(history)
                .build();
    }

    private StockChartBO.Bar dailyBar(IndustryApplicationBO.Bar bar) {
        return StockChartBO.Bar.builder()
                .tradeDate(bar.getTradeDate())
                .open(bar.getOpen())
                .high(bar.getHigh())
                .low(bar.getLow())
                .close(bar.getClose())
                .volume(toShares(bar.getVolumeLots()))
                .turnover(bar.getAmount())
                .build();
    }

    private StockChartBO.FundFlowSnapshot fundFlowDailyFallback(String code) {
        List<StockChartBO.Bar> bars = listDailyBars("stock", code, 31);
        if (bars.isEmpty()) {
            return StockChartBO.FundFlowSnapshot.builder()
                    .ok(false)
                    .symbol(code)
                    .error("stock fund flow snapshot is empty")
                    .history(List.of())
                    .build();
        }
        StockChartBO.StockMeta meta = stockMeta(code);
        List<StockChartBO.FundFlowPoint> history = new ArrayList<>();
        for (int index = Math.max(0, bars.size() - 30); index < bars.size(); index++) {
            StockChartBO.Bar current = bars.get(index);
            StockChartBO.Bar previous = index > 0 ? bars.get(index - 1) : null;
            history.add(StockChartBO.FundFlowPoint.builder()
                    .tradeDate(current.getTradeDate())
                    .amountWan(toWan(current.getTurnover()))
                    .changePct(changePct(current, previous))
                    .build());
        }
        Collections.reverse(history);
        StockChartBO.Bar latest = bars.getLast();
        StockChartBO.Bar previous = bars.size() > 1 ? bars.get(bars.size() - 2) : null;
        return StockChartBO.FundFlowSnapshot.builder()
                .ok(true)
                .symbol(code)
                .name(meta.getName())
                .tradeDate(latest.getTradeDate())
                .lastPrice(latest.getClose())
                .changePct(changePct(latest, previous))
                .amountWan(toWan(latest.getTurnover()))
                .source("java-tdx-local-daily-fallback")
                .error("stock_fund_flow_snapshot is empty; only daily amount/change fields are derived")
                .history(history)
                .build();
    }

    private BigDecimal toWan(BigDecimal amount) {
        return amount == null ? null : amount.divide(BigDecimal.valueOf(10000), 6, RoundingMode.HALF_UP);
    }

    private BigDecimal changePct(StockChartBO.Bar current, StockChartBO.Bar previous) {
        if (current == null || previous == null || current.getClose() == null || previous.getClose() == null
                || BigDecimal.ZERO.compareTo(previous.getClose()) == 0) {
            return null;
        }
        return current.getClose()
                .subtract(previous.getClose())
                .multiply(BigDecimal.valueOf(100))
                .divide(previous.getClose(), 6, RoundingMode.HALF_UP);
    }

    private BigDecimal toShares(Long volumeLots) {
        return volumeLots == null ? null : BigDecimal.valueOf(volumeLots).multiply(BigDecimal.valueOf(100));
    }

    private String normalizeTdxSymbol(String targetType, String symbol) {
        String code = normalizeCode(symbol);
        if ("index".equalsIgnoreCase(targetType)) {
            return code.startsWith("399") ? "sz" + code : "sh" + code;
        }
        if (code.startsWith("6") || code.startsWith("5") || code.startsWith("9")) {
            return "sh" + code;
        }
        return "sz" + code;
    }

    private MarketSymbol normalizeMarketSymbol(String targetType, String symbol) {
        String code = normalizeCode(symbol);
        if ("index".equalsIgnoreCase(targetType)) {
            return code.startsWith("399") ? new MarketSymbol("SZ", code) : new MarketSymbol("SH", code);
        }
        if (code.startsWith("6") || code.startsWith("5") || code.startsWith("9")) {
            return new MarketSymbol("SH", code);
        }
        if (code.startsWith("8") || code.startsWith("4")) {
            return new MarketSymbol("BJ", code);
        }
        return new MarketSymbol("SZ", code);
    }

    private String normalizeCode(String symbol) {
        String value = symbol == null ? "" : symbol.trim().toLowerCase();
        String digits = value.replaceAll("\\D", "");
        return digits.length() >= 6 ? digits.substring(digits.length() - 6) : value;
    }

    private record MarketSymbol(String market, String symbol) {
    }
}
