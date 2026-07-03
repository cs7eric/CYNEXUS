package cn.cccs7.cynexus.market.infra.repository;

import cn.cccs7.cynexus.market.domain.entity.IndustryApplicationBO;
import cn.cccs7.cynexus.market.domain.entity.StockChartBO;
import cn.cccs7.cynexus.market.domain.repository.StockChartQueryRepository;
import cn.cccs7.cynexus.market.infra.basic.entity.StockBasicPO;
import cn.cccs7.cynexus.market.infra.basic.entity.StockFundFlowSnapshotPO;
import cn.cccs7.cynexus.market.infra.basic.entity.StockSectorConstituentPO;
import cn.cccs7.cynexus.market.infra.basic.entity.StockSectorJoinedPO;
import cn.cccs7.cynexus.market.infra.basic.mapper.StockSectorMapper;
import cn.cccs7.cynexus.market.infra.basic.service.StockBasicBasicService;
import cn.cccs7.cynexus.market.infra.basic.service.StockFundFlowSnapshotBasicService;
import cn.cccs7.cynexus.market.infra.client.ClickHouseStockChartClient;
import cn.cccs7.cynexus.market.infra.client.HttpStockChartF10DataClient;
import cn.cccs7.cynexus.market.infra.client.TdxLocalDayKlineClient;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

@Repository
public class StockChartQueryRepositoryImpl implements StockChartQueryRepository {

    private final StockBasicBasicService stockBasicBasicService;
    private final StockFundFlowSnapshotBasicService stockFundFlowSnapshotBasicService;
    private final StockSectorMapper stockSectorMapper;
    private final TdxLocalDayKlineClient tdxLocalDayKlineClient;
    private final ClickHouseStockChartClient clickHouseStockChartClient;
    private final HttpStockChartF10DataClient stockChartF10DataClient;

    public StockChartQueryRepositoryImpl(StockBasicBasicService stockBasicBasicService,
                                         StockFundFlowSnapshotBasicService stockFundFlowSnapshotBasicService,
                                         StockSectorMapper stockSectorMapper,
                                         TdxLocalDayKlineClient tdxLocalDayKlineClient,
                                         ClickHouseStockChartClient clickHouseStockChartClient,
                                         HttpStockChartF10DataClient stockChartF10DataClient) {
        this.stockBasicBasicService = stockBasicBasicService;
        this.stockFundFlowSnapshotBasicService = stockFundFlowSnapshotBasicService;
        this.stockSectorMapper = stockSectorMapper;
        this.tdxLocalDayKlineClient = tdxLocalDayKlineClient;
        this.clickHouseStockChartClient = clickHouseStockChartClient;
        this.stockChartF10DataClient = stockChartF10DataClient;
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
    public List<StockChartBO.Bar> listDailyBars(String targetType, String symbol, int limit, String adjust) {
        String normalizedTargetType = blankToNull(targetType) == null ? "stock" : targetType.trim();
        String normalizedAdjust = blankToNull(adjust) == null ? "qfq" : adjust.trim().toLowerCase();
        if ("stock".equalsIgnoreCase(normalizedTargetType) && "qfq".equals(normalizedAdjust)) {
            String code = normalizeCode(symbol);
            return clickHouseStockChartClient.listQfqDailyBars(code, limit).stream()
                    .map(row -> StockChartBO.Bar.builder()
                            .tradeDate(row.getTradeDate())
                            .open(row.getOpen())
                            .high(row.getHigh())
                            .low(row.getLow())
                            .close(row.getClose())
                            .volume(row.getVolume())
                            .turnover(row.getAmount())
                            .build())
                    .toList();
        }
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
    public Optional<LocalDate> findLatestAuctionDate(String targetType, String symbol, LocalDate requestedDate) {
        MarketSymbol marketSymbol = normalizeMarketSymbol(targetType, symbol);
        return clickHouseStockChartClient.findLatestAuctionDate(marketSymbol.market(), marketSymbol.symbol(), requestedDate);
    }

    @Override
    public List<StockChartBO.AuctionPoint> listAuctionPoints(String targetType, String symbol, LocalDate tradeDate) {
        MarketSymbol marketSymbol = normalizeMarketSymbol(targetType, symbol);
        return clickHouseStockChartClient.listAuctionPoints(marketSymbol.market(), marketSymbol.symbol(), tradeDate).stream()
                .map(row -> StockChartBO.AuctionPoint.builder()
                        .timestamp(row.getTimestamp())
                        .tradeDate(row.getTradeDate())
                        .timeLabel(row.getTimeLabel())
                        .phase(row.getPhase())
                        .price(row.getPrice())
                        .matchedVolume(row.getMatchedVolume())
                        .matchedAmount(row.getMatchedAmount())
                        .unmatchedBuyVolume(row.getUnmatchedBuyVolume())
                        .unmatchedSellVolume(row.getUnmatchedSellVolume())
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
    public StockChartBO.StockSectors stockSectors(String symbol) {
        String code = normalizeCode(symbol);
        List<StockChartBO.SectorEntry> industries = new ArrayList<>();
        List<StockChartBO.SectorEntry> concepts = new ArrayList<>();
        List<StockChartBO.SectorEntry> styles = new ArrayList<>();
        for (StockSectorJoinedPO row : stockSectorMapper.listJoinedByCode(code)) {
            StockChartBO.SectorEntry entry = StockChartBO.SectorEntry.builder()
                    .sectorId(row.getSectorId())
                    .name(row.getName())
                    .topicId(row.getTopicId())
                    .categoryRaw(row.getCategoryRaw())
                    .changePercent(null)
                    .source(row.getSource())
                    .build();
            if (Integer.valueOf(0).equals(row.getCategoryRaw())) {
                industries.add(entry);
            } else if (Integer.valueOf(2).equals(row.getCategoryRaw())) {
                concepts.add(entry);
            } else if (Integer.valueOf(4).equals(row.getCategoryRaw())) {
                styles.add(entry);
            }
        }
        int count = industries.size() + concepts.size() + styles.size();
        return StockChartBO.StockSectors.builder()
                .code(code)
                .industries(industries)
                .concepts(concepts)
                .styles(styles)
                .source(count == 0 ? "postgres.sec_stock_sector-empty" : "postgres.sec_stock_sector")
                .build();
    }

    @Override
    public StockChartBO.SectorStocks sectorStocks(String sectorId, Integer categoryRaw, String topicId, String name, int limit) {
        String resolvedSectorId = blankToNull(sectorId);
        String resolvedTopicId = blankToNull(topicId);
        String resolvedName = blankToNull(name);
        int resolvedLimit = Math.min(Math.max(limit, 1), 1000);
        boolean hasSelector = resolvedSectorId != null
                || categoryRaw != null
                || resolvedTopicId != null
                || resolvedName != null;
        if (!hasSelector) {
            return StockChartBO.SectorStocks.builder()
                    .sector(emptySectorMeta(resolvedSectorId, categoryRaw, resolvedTopicId, resolvedName))
                    .stocks(List.of())
                    .source("postgres.sec_stock_sector-empty")
                    .build();
        }

        List<StockSectorConstituentPO> rows = stockSectorMapper.listConstituentsBySector(
                resolvedSectorId,
                categoryRaw,
                resolvedTopicId,
                resolvedName,
                resolvedLimit
        );
        StockChartBO.SectorMeta sector = rows.isEmpty()
                ? emptySectorMeta(resolvedSectorId, categoryRaw, resolvedTopicId, resolvedName)
                : sectorMeta(rows.get(0));
        Map<String, ClickHouseStockChartClient.DailyQuote> dailyQuotes = loadLatestDailyQuotes(rows);
        List<StockChartBO.SectorStock> stocks = rows.stream()
                .map(row -> sectorStock(row, dailyQuotes.get(quoteKey(row.getMarket(), row.getCode()))))
                .toList();
        return StockChartBO.SectorStocks.builder()
                .sector(sector)
                .stocks(stocks)
                .source(rows.isEmpty() ? "postgres.sec_stock_sector-empty" : "postgres.sec_stock_sector")
                .build();
    }

    private Map<String, ClickHouseStockChartClient.DailyQuote> loadLatestDailyQuotes(List<StockSectorConstituentPO> rows) {
        List<ClickHouseStockChartClient.MarketSymbol> symbols = rows.stream()
                .map(row -> new ClickHouseStockChartClient.MarketSymbol(resolveMarket(row.getMarket(), row.getCode()), row.getCode()))
                .distinct()
                .toList();
        return clickHouseStockChartClient.listLatestDailyQuotes(symbols).stream()
                .collect(Collectors.toMap(
                        row -> quoteKey(row.getMarket(), row.getSymbol()),
                        Function.identity(),
                        (left, right) -> left
                ));
    }

    private StockChartBO.SectorStock sectorStock(StockSectorConstituentPO row,
                                                 ClickHouseStockChartClient.DailyQuote quote) {
        return StockChartBO.SectorStock.builder()
                .symbol(row.getCode())
                .name(row.getStockName() == null || row.getStockName().isBlank() ? row.getCode() : row.getStockName())
                .market(row.getMarket())
                .relationLevel(row.getRelationLevel())
                .tradeDate(row.getTradeDate() != null ? row.getTradeDate() : quote == null ? null : quote.getTradeDate())
                .lastPrice(row.getLastPrice() != null ? row.getLastPrice() : quote == null ? null : quote.getClose())
                .preClose(row.getPreClose() != null ? row.getPreClose() : quote == null ? null : quote.getPreClose())
                .open(row.getOpen() != null ? row.getOpen() : quote == null ? null : quote.getOpen())
                .high(row.getHigh() != null ? row.getHigh() : quote == null ? null : quote.getHigh())
                .low(row.getLow() != null ? row.getLow() : quote == null ? null : quote.getLow())
                .change(row.getChange() != null ? row.getChange() : quote == null ? null : quote.getChange())
                .changePct(row.getChangePct() != null ? row.getChangePct() : quote == null ? null : quote.getChangePct())
                .volumeLots(row.getVolumeLots() != null ? row.getVolumeLots() : quote == null ? null : toLots(quote.getVolume()))
                .amountWan(row.getAmountWan() != null ? row.getAmountWan() : quote == null ? null : toWan(quote.getAmount()))
                .turnoverRate(row.getTurnoverRate())
                .amplitude(row.getAmplitude() != null ? row.getAmplitude() : quote == null ? null : quote.getAmplitude())
                .totalMarketCap(resolveMarketCap(row.getTotalMarketCap(), row.getFundFlowTotalMarketCapYi(), row.getTotalShares(), quote))
                .floatMarketCap(resolveMarketCap(row.getFloatMarketCap(), row.getFundFlowFloatMarketCapYi(), row.getCirculatingShares(), quote))
                .returnPct5d(quote == null ? null : quote.getReturnPct5d())
                .returnPct10d(quote == null ? null : quote.getReturnPct10d())
                .returnPct30d(quote == null ? null : quote.getReturnPct30d())
                .returnPct60d(quote == null ? null : quote.getReturnPct60d())
                .returnPct252d(quote == null ? null : quote.getReturnPct252d())
                .capturedAt(row.getCapturedAt())
                .source(row.getSource())
                .build();
    }

    private BigDecimal resolveMarketCap(BigDecimal storedMarketCap,
                                        BigDecimal fundFlowMarketCapYi,
                                        BigDecimal shares,
                                        ClickHouseStockChartClient.DailyQuote quote) {
        if (storedMarketCap != null) {
            return storedMarketCap;
        }
        if (fundFlowMarketCapYi != null) {
            return fundFlowMarketCapYi.multiply(BigDecimal.valueOf(100_000_000L));
        }
        if (shares == null || quote == null || quote.getClose() == null) {
            return null;
        }
        return shares.multiply(quote.getClose()).setScale(4, RoundingMode.HALF_UP);
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

    @Override
    public StockChartBO.F10Valuation valuation(String symbol, String reqId) {
        return stockChartF10DataClient.valuation(normalizeCode(symbol), reqId);
    }

    @Override
    public StockChartBO.F10FinanceReport financeReport(String symbol, String reportType) {
        return stockChartF10DataClient.financeReport(normalizeCode(symbol), reportType);
    }

    @Override
    public StockChartBO.F10BusinessComposition businessComposition(String symbol, String reportDate) {
        return stockChartF10DataClient.businessComposition(normalizeCode(symbol), reportDate);
    }

    @Override
    public StockChartBO.F10ProfitForecast profitForecast(String symbol) {
        return stockChartF10DataClient.profitForecast(normalizeCode(symbol));
    }

    @Override
    public StockChartBO.F10Announcements announcements(String symbol) {
        return stockChartF10DataClient.announcements(normalizeCode(symbol));
    }

    @Override
    public StockChartBO.F10News news(String symbol) {
        return stockChartF10DataClient.news(normalizeCode(symbol));
    }

    @Override
    public StockChartBO.F10Roadshows roadshows(String symbol) {
        return stockChartF10DataClient.roadshows(normalizeCode(symbol));
    }

    @Override
    public StockChartBO.F10CompanyNews companyNews(String symbol, String section) {
        return stockChartF10DataClient.companyNews(normalizeCode(symbol), blankToNull(section) == null ? "gsyj" : section.trim());
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
        List<StockChartBO.Bar> bars = listDailyBars("stock", code, 31, "none");
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

    private BigDecimal toLots(BigDecimal volume) {
        return volume == null ? null : volume.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
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

    private StockChartBO.SectorMeta sectorMeta(StockSectorConstituentPO row) {
        return StockChartBO.SectorMeta.builder()
                .sectorId(row.getSectorId())
                .name(row.getSectorName())
                .categoryRaw(row.getCategoryRaw())
                .categoryLabel(row.getCategoryLabel())
                .topicId(row.getTopicId())
                .source(row.getSectorSource())
                .build();
    }

    private StockChartBO.SectorMeta emptySectorMeta(String sectorId, Integer categoryRaw, String topicId, String name) {
        return StockChartBO.SectorMeta.builder()
                .sectorId(sectorId)
                .name(name)
                .categoryRaw(categoryRaw)
                .categoryLabel(categoryLabel(categoryRaw))
                .topicId(topicId)
                .build();
    }

    private String categoryLabel(Integer categoryRaw) {
        if (Integer.valueOf(0).equals(categoryRaw)) {
            return "industries";
        }
        if (Integer.valueOf(2).equals(categoryRaw)) {
            return "concepts";
        }
        if (Integer.valueOf(4).equals(categoryRaw)) {
            return "styles";
        }
        return null;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String quoteKey(String market, String symbol) {
        return (market == null ? "" : market.trim().toUpperCase()) + ":" + normalizeCode(symbol);
    }

    private String resolveMarket(String market, String code) {
        if (market != null && !market.isBlank()) {
            return market.trim().toUpperCase();
        }
        return normalizeCode(code).startsWith("6") ? "SH" : "SZ";
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
