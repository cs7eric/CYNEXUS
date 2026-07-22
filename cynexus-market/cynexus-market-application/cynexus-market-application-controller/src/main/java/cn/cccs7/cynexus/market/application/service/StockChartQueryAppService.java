package cn.cccs7.cynexus.market.application.service;

import cn.cccs7.cynexus.market.api.dto.StockChartDTO;
import cn.cccs7.cynexus.market.domain.entity.StockChartBO;
import cn.cccs7.cynexus.market.domain.service.stock.StockChartQueryDomainService;
import cn.cccs7.cynexus.market.domain.stock.StockAuctionDomainPolicy;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class StockChartQueryAppService {

    private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");

    private final StockChartQueryDomainService queryDomainService;

    public StockChartQueryAppService(StockChartQueryDomainService queryDomainService) {
        this.queryDomainService = queryDomainService;
    }

    public StockChartDTO.SearchResponseDTO search(String keyword) {
        return StockChartDTO.SearchResponseDTO.builder()
                .items(queryDomainService.search(keyword).stream()
                        .map(this::searchItem)
                        .toList())
                .build();
    }

    public StockChartDTO.KlineResponseDTO klines(String targetType, String symbol, String period, String adjust) {
        StockChartQueryDomainService.KlineResult result = queryDomainService.klines(targetType, symbol, period, adjust);
        return StockChartDTO.KlineResponseDTO.builder()
                .symbol(symbol)
                .targetType(result.targetType())
                .period(result.period())
                .adjust(result.adjust())
                .updatedAt(OffsetDateTime.now(SHANGHAI))
                .source(result.source())
                .items(result.items().stream().map(this::bar).toList())
                .build();
    }

    public StockChartDTO.StockMetaDTO stockMeta(String symbol) {
        StockChartBO.StockMeta value = queryDomainService.stockMeta(symbol);
        return StockChartDTO.StockMetaDTO.builder()
                .symbol(value.getSymbol())
                .name(value.getName())
                .totalMarketCap(value.getTotalMarketCap())
                .circMarketCap(value.getCircMarketCap())
                .industry(value.getIndustry())
                .capStyle(value.getCapStyle())
                .sectorIndexSymbol(value.getSectorIndexSymbol())
                .sectorIndexName(value.getSectorIndexName())
                .build();
    }

    public StockChartDTO.StockSectorsResponseDTO stockSectors(String symbol) {
        StockChartBO.StockSectors value = queryDomainService.stockSectors(symbol);
        List<StockChartDTO.StockSectorEntryDTO> industries = value.getIndustries() == null
                ? List.of()
                : value.getIndustries().stream().map(this::stockSectorEntry).toList();
        List<StockChartDTO.StockSectorEntryDTO> concepts = value.getConcepts() == null
                ? List.of()
                : value.getConcepts().stream().map(this::stockSectorEntry).toList();
        List<StockChartDTO.StockSectorEntryDTO> styles = value.getStyles() == null
                ? List.of()
                : value.getStyles().stream().map(this::stockSectorEntry).toList();
        return StockChartDTO.StockSectorsResponseDTO.builder()
                .code(value.getCode())
                .industries(industries)
                .concepts(concepts)
                .styles(styles)
                .count(industries.size() + concepts.size() + styles.size())
                .source(value.getSource())
                .build();
    }

    public StockChartDTO.FundFlowSnapshotDTO fundFlow(String symbol) {
        StockChartBO.FundFlowSnapshot value = queryDomainService.fundFlow(symbol);
        return StockChartDTO.FundFlowSnapshotDTO.builder()
                .ok(value.getOk())
                .symbol(value.getSymbol())
                .name(value.getName())
                .tradeDate(value.getTradeDate())
                .lastPrice(value.getLastPrice())
                .changePct(value.getChangePct())
                .amountWan(value.getAmountWan())
                .turnoverRate(value.getTurnoverRate())
                .activeNetAmountWan(value.getActiveNetAmountWan())
                .mainNetAmountWan(value.getMainNetAmountWan())
                .superNetAmountWan(value.getSuperNetAmountWan())
                .largeNetAmountWan(value.getLargeNetAmountWan())
                .mediumNetAmountWan(value.getMediumNetAmountWan())
                .smallNetAmountWan(value.getSmallNetAmountWan())
                .activeBuyRatio(value.getActiveBuyRatio())
                .activeSellRatio(value.getActiveSellRatio())
                .buyBigRatio(value.getBuyBigRatio())
                .sellBigRatio(value.getSellBigRatio())
                .source(value.getSource())
                .capturedAt(value.getCapturedAt())
                .error(value.getError())
                .history(value.getHistory() == null ? List.of() : value.getHistory().stream().map(this::fundFlowPoint).toList())
                .build();
    }

    private StockChartDTO.StockSectorEntryDTO stockSectorEntry(StockChartBO.SectorEntry value) {
        return StockChartDTO.StockSectorEntryDTO.builder()
                .sectorId(value.getSectorId())
                .name(value.getName())
                .topicId(value.getTopicId())
                .categoryRaw(value.getCategoryRaw())
                .changePercent(value.getChangePercent())
                .source(value.getSource())
                .build();
    }

    public StockChartDTO.SectorStocksResponseDTO sectorStocks(String sectorId, Integer categoryRaw,
                                                              String topicId, String name, int limit) {
        StockChartBO.SectorStocks value = queryDomainService.sectorStocks(sectorId, categoryRaw, topicId, name, limit);
        List<StockChartDTO.SectorStockDTO> stocks = value.getStocks() == null
                ? List.of()
                : value.getStocks().stream().map(this::sectorStock).toList();
        return StockChartDTO.SectorStocksResponseDTO.builder()
                .sector(sectorMeta(value.getSector()))
                .stocks(stocks)
                .count(stocks.size())
                .source(value.getSource())
                .build();
    }

    public StockChartDTO.IntradayDTO intraday(String targetType, String symbol, String name, String adjust,
                                              String tradeDate, List<String> periods) {
        StockChartQueryDomainService.IntradayResult result = queryDomainService.intraday(
                targetType, symbol, name, adjust, tradeDate, periods);
        return StockChartDTO.IntradayDTO.builder()
                .ok(true)
                .symbol(result.symbol())
                .targetType(result.targetType())
                .name(result.name())
                .adjust(result.adjust())
                .effectiveAdjust(result.effectiveAdjust())
                .requestedAdjust(result.requestedAdjust())
                .source(result.source())
                .tradeDate(result.tradeDate())
                .requestedTradeDate(result.requestedTradeDate())
                .timeshare(result.timeshare().stream().map(this::intradayPoint).toList())
                .minuteBars(minuteBars(result.minuteBars().entrySet().stream()
                        .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey,
                                entry -> entry.getValue().stream().map(this::bar).toList()))))
                .error(result.error())
                .build();
    }

    public StockChartDTO.AuctionDTO auction(String symbol, String tradeDate) {
        return auction(queryDomainService.auction(symbol, tradeDate));
    }

    public StockChartDTO.AuctionHistoryResponseDTO auctionHistory(String symbol, int limit) {
        List<StockChartDTO.AuctionDTO> items = queryDomainService.auctionHistory(symbol, limit).stream()
                .map(this::auction)
                .toList();
        return StockChartDTO.AuctionHistoryResponseDTO.builder()
                .symbol(symbol)
                .source(items.isEmpty() ? "clickhouse.ch_ods_stock_auction_point-empty" : "clickhouse.ch_ods_stock_auction_point")
                .items(items)
                .build();
    }

    private StockChartDTO.AuctionDTO auction(StockChartQueryDomainService.AuctionResult result) {
        return StockChartDTO.AuctionDTO.builder()
                .symbol(result.symbol())
                .tradeDate(result.tradeDate())
                .source(result.source())
                .error(result.error())
                .opening(auctionPhase(result.opening()))
                .closing(auctionPhase(result.closing()))
                .details(StockChartDTO.AuctionDetailsDTO.builder()
                        .auction0925(result.auction0925())
                        .openingPoints(result.openingPoints().stream().map(this::auctionPoint).toList())
                        .closingPoints(result.closingPoints().stream().map(this::auctionPoint).toList())
                        .allPoints(result.allPoints().stream().map(this::auctionPoint).toList())
                        .build())
                .build();
    }

    public StockChartDTO.F10ValuationDTO valuation(String symbol, String reqId) {
        StockChartBO.F10Valuation value = queryDomainService.valuation(symbol, reqId);
        return StockChartDTO.F10ValuationDTO.builder()
                .symbol(value.getSymbol())
                .reqId(value.getReqId())
                .rows(value.getRows() == null ? List.of() : value.getRows().stream().map(this::valuationRow).toList())
                .fetchedAt(formatOffsetDateTime(value.getFetchedAt()))
                .source(value.getSource())
                .build();
    }

    public StockChartDTO.F10FinanceReportDTO financeReport(String symbol, String reportType) {
        StockChartBO.F10FinanceReport value = queryDomainService.financeReport(symbol, reportType);
        return StockChartDTO.F10FinanceReportDTO.builder()
                .symbol(value.getSymbol())
                .reportType(value.getReportType())
                .rows(value.getRows() == null ? List.of() : value.getRows().stream().map(this::financeReportRow).toList())
                .fetchedAt(formatOffsetDateTime(value.getFetchedAt()))
                .source(value.getSource())
                .build();
    }

    public StockChartDTO.F10BusinessCompositionDTO businessComposition(String symbol, String reportDate) {
        StockChartBO.F10BusinessComposition value = queryDomainService.businessComposition(symbol, reportDate);
        return StockChartDTO.F10BusinessCompositionDTO.builder()
                .symbol(value.getSymbol())
                .reportDate(value.getReportDate())
                .items(value.getItems() == null ? List.of() : value.getItems().stream().map(this::businessCompositionItem).toList())
                .fetchedAt(formatOffsetDateTime(value.getFetchedAt()))
                .source(value.getSource())
                .build();
    }

    public StockChartDTO.F10ProfitForecastDTO profitForecast(String symbol) {
        StockChartBO.F10ProfitForecast value = queryDomainService.profitForecast(symbol);
        return StockChartDTO.F10ProfitForecastDTO.builder()
                .symbol(value.getSymbol())
                .items(value.getItems() == null ? List.of() : value.getItems().stream().map(this::profitForecastItem).toList())
                .fetchedAt(formatOffsetDateTime(value.getFetchedAt()))
                .source(value.getSource())
                .build();
    }

    public StockChartDTO.F10AnnouncementsDTO announcements(String symbol) {
        StockChartBO.F10Announcements value = queryDomainService.announcements(symbol);
        return StockChartDTO.F10AnnouncementsDTO.builder()
                .symbol(value.getSymbol())
                .items(value.getItems() == null ? List.of() : value.getItems().stream().map(this::announcementItem).toList())
                .count(value.getCount())
                .fetchedAt(formatOffsetDateTime(value.getFetchedAt()))
                .source(value.getSource())
                .build();
    }

    public StockChartDTO.F10NewsDTO news(String symbol) {
        StockChartBO.F10News value = queryDomainService.news(symbol);
        return StockChartDTO.F10NewsDTO.builder()
                .symbol(value.getSymbol())
                .items(value.getItems() == null ? List.of() : value.getItems().stream().map(this::newsItem).toList())
                .count(value.getCount())
                .fetchedAt(formatOffsetDateTime(value.getFetchedAt()))
                .source(value.getSource())
                .build();
    }

    public StockChartDTO.F10RoadshowsDTO roadshows(String symbol) {
        StockChartBO.F10Roadshows value = queryDomainService.roadshows(symbol);
        return StockChartDTO.F10RoadshowsDTO.builder()
                .symbol(value.getSymbol())
                .items(value.getItems() == null ? List.of() : value.getItems().stream().map(this::roadshowItem).toList())
                .count(value.getCount())
                .fetchedAt(formatOffsetDateTime(value.getFetchedAt()))
                .source(value.getSource())
                .build();
    }

    public StockChartDTO.F10CompanyNewsDTO companyNews(String symbol, String section) {
        StockChartBO.F10CompanyNews value = queryDomainService.companyNews(symbol, section);
        return StockChartDTO.F10CompanyNewsDTO.builder()
                .symbol(value.getSymbol())
                .section(value.getSection())
                .items(value.getItems() == null ? List.of() : value.getItems().stream().map(this::companyNewsItem).toList())
                .count(value.getCount())
                .fetchedAt(formatOffsetDateTime(value.getFetchedAt()))
                .source(value.getSource())
                .build();
    }

    private StockChartDTO.SearchItemDTO searchItem(StockChartBO.SearchItem value) {
        return StockChartDTO.SearchItemDTO.builder()
                .targetType(value.getTargetType())
                .symbol(value.getSymbol())
                .name(value.getName())
                .build();
    }

    private StockChartDTO.KlineBarDTO bar(StockChartBO.Bar value) {
        Long timestamp = value.getTimestamp();
        if (timestamp == null && value.getTradeDate() != null) {
            timestamp = value.getTradeDate().atTime(15, 0).atZone(SHANGHAI).toInstant().toEpochMilli();
        }
        return StockChartDTO.KlineBarDTO.builder()
                .timestamp(timestamp)
                .tradeDate(value.getTradeDate())
                .open(value.getOpen())
                .high(value.getHigh())
                .low(value.getLow())
                .close(value.getClose())
                .volume(value.getVolume())
                .turnover(value.getTurnover())
                .turnoverRate(value.getTurnoverRate())
                .volumeRatio(value.getVolumeRatio())
                .build();
    }

    private StockChartDTO.FundFlowPointDTO fundFlowPoint(StockChartBO.FundFlowPoint value) {
        return StockChartDTO.FundFlowPointDTO.builder()
                .tradeDate(value.getTradeDate())
                .lastPrice(value.getLastPrice())
                .turnoverRate(value.getTurnoverRate())
                .activeNetAmountWan(value.getActiveNetAmountWan())
                .mainNetAmountWan(value.getMainNetAmountWan())
                .superNetAmountWan(value.getSuperNetAmountWan())
                .largeNetAmountWan(value.getLargeNetAmountWan())
                .mediumNetAmountWan(value.getMediumNetAmountWan())
                .smallNetAmountWan(value.getSmallNetAmountWan())
                .activeBuyRatio(value.getActiveBuyRatio())
                .activeSellRatio(value.getActiveSellRatio())
                .buyBigRatio(value.getBuyBigRatio())
                .sellBigRatio(value.getSellBigRatio())
                .amountWan(value.getAmountWan())
                .changePct(value.getChangePct())
                .build();
    }

    private StockChartDTO.SectorMetaDTO sectorMeta(StockChartBO.SectorMeta value) {
        if (value == null) {
            return null;
        }
        return StockChartDTO.SectorMetaDTO.builder()
                .sectorId(value.getSectorId())
                .name(value.getName())
                .categoryRaw(value.getCategoryRaw())
                .categoryLabel(value.getCategoryLabel())
                .topicId(value.getTopicId())
                .source(value.getSource())
                .build();
    }

    private StockChartDTO.SectorStockDTO sectorStock(StockChartBO.SectorStock value) {
        return StockChartDTO.SectorStockDTO.builder()
                .symbol(value.getSymbol())
                .name(value.getName())
                .market(value.getMarket())
                .relationLevel(value.getRelationLevel())
                .tradeDate(value.getTradeDate())
                .lastPrice(value.getLastPrice())
                .preClose(value.getPreClose())
                .open(value.getOpen())
                .high(value.getHigh())
                .low(value.getLow())
                .change(value.getChange())
                .changePct(value.getChangePct())
                .volumeLots(value.getVolumeLots())
                .amountWan(value.getAmountWan())
                .turnoverRate(value.getTurnoverRate())
                .amplitude(value.getAmplitude())
                .totalMarketCap(value.getTotalMarketCap())
                .floatMarketCap(value.getFloatMarketCap())
                .returnPct5d(value.getReturnPct5d())
                .returnPct10d(value.getReturnPct10d())
                .returnPct30d(value.getReturnPct30d())
                .returnPct60d(value.getReturnPct60d())
                .returnPct252d(value.getReturnPct252d())
                .capturedAt(value.getCapturedAt())
                .source(value.getSource())
                .build();
    }

    private StockChartDTO.IntradayPointDTO intradayPoint(StockChartBO.IntradayPoint value) {
        return StockChartDTO.IntradayPointDTO.builder()
                .timestamp(value.getTimestamp())
                .tradeDate(value.getTradeDate())
                .timeLabel(value.getTimeLabel())
                .price(value.getPrice())
                .open(value.getOpen())
                .avgPrice(value.getAvgPrice())
                .volume(value.getVolume())
                .turnover(value.getTurnover())
                .turnoverRate(value.getTurnoverRate())
                .build();
    }

    private StockChartDTO.AuctionPointDTO auctionPoint(StockAuctionDomainPolicy.AuctionPointFacts facts) {
        return StockChartDTO.AuctionPointDTO.builder()
                .timeLabel(facts.timeLabel())
                .price(facts.price())
                .matchedVolume(facts.matchedVolume())
                .unmatchedVolume(facts.unmatchedVolume())
                .unmatchedDirectionRaw(facts.unmatchedDirectionRaw())
                .matchedAmountEstimated(facts.matchedAmount())
                .build();
    }

    private StockChartDTO.F10ValuationRowDTO valuationRow(StockChartBO.F10ValuationRow value) {
        return StockChartDTO.F10ValuationRowDTO.builder()
                .date(value.getDate())
                .peTtm(value.getPeTtm())
                .peBfw(value.getPeBfw())
                .pbMrq(value.getPbMrq())
                .pbBfw(value.getPbBfw())
                .pcfOcfTtm(value.getPcfOcfTtm())
                .pcfBfw(value.getPcfBfw())
                .psTtm(value.getPsTtm())
                .psBfw(value.getPsBfw())
                .peg(value.getPeg())
                .avgMarketCap(value.getAvgMarketCap())
                .aliqMarketCap(value.getAliqMarketCap())
                .build();
    }

    private StockChartDTO.F10FinanceReportRowDTO financeReportRow(StockChartBO.F10FinanceReportRow value) {
        return StockChartDTO.F10FinanceReportRowDTO.builder()
                .rq(value.getRq())
                .fields(value.getFields() == null ? Map.of() : value.getFields())
                .build();
    }

    private StockChartDTO.F10BusinessCompositionItemDTO businessCompositionItem(StockChartBO.F10BusinessCompositionItem value) {
        return StockChartDTO.F10BusinessCompositionItemDTO.builder()
                .category(value.getCategory())
                .subType(value.getSubType())
                .name(value.getName())
                .revenue(value.getRevenue())
                .ratio(value.getRatio())
                .extras(value.getExtras() == null ? List.of() : value.getExtras())
                .build();
    }

    private StockChartDTO.F10ProfitForecastItemDTO profitForecastItem(StockChartBO.F10ProfitForecastItem value) {
        return StockChartDTO.F10ProfitForecastItemDTO.builder()
                .year(value.getYear())
                .netProfit(value.getNetProfit())
                .eps(value.getEps())
                .revenue(value.getRevenue())
                .extras(value.getExtras() == null ? List.of() : value.getExtras())
                .build();
    }

    private StockChartDTO.F10AnnouncementItemDTO announcementItem(StockChartBO.F10AnnouncementItem value) {
        return StockChartDTO.F10AnnouncementItemDTO.builder()
                .issueDate(value.getIssueDate())
                .title(value.getTitle())
                .typecode(value.getTypecode())
                .typename(value.getTypename())
                .recId(value.getRecId())
                .tableid(value.getTableid())
                .url(value.getUrl())
                .redistime(value.getRedistime())
                .source(value.getSource())
                .build();
    }

    private StockChartDTO.F10NewsItemDTO newsItem(StockChartBO.F10NewsItem value) {
        return StockChartDTO.F10NewsItemDTO.builder()
                .issueDate(value.getIssueDate())
                .title(value.getTitle())
                .recId(value.getRecId())
                .tableid(value.getTableid())
                .redistime(value.getRedistime())
                .source(value.getSource())
                .relatecolumn(value.getRelatecolumn())
                .build();
    }

    private StockChartDTO.F10RoadshowItemDTO roadshowItem(StockChartBO.F10RoadshowItem value) {
        return StockChartDTO.F10RoadshowItemDTO.builder()
                .title(value.getTitle())
                .roadshowType(value.getRoadshowType())
                .startDate(value.getStartDate())
                .startTime(value.getStartTime())
                .endTime(value.getEndTime())
                .summary(value.getSummary())
                .url(value.getUrl())
                .build();
    }

    private StockChartDTO.F10CompanyNewsItemDTO companyNewsItem(StockChartBO.F10CompanyNewsItem value) {
        return StockChartDTO.F10CompanyNewsItemDTO.builder()
                .rating(value.getRating())
                .analysts(value.getAnalysts())
                .recId(value.getRecId())
                .issueDate(value.getIssueDate())
                .title(value.getTitle())
                .nflag(value.getNflag())
                .docHash(value.getDocHash())
                .build();
    }

    private StockChartDTO.AuctionPhaseDTO auctionPhase(StockAuctionDomainPolicy.AuctionPhaseFacts facts) {
        return StockChartDTO.AuctionPhaseDTO.builder()
                .price(facts.price()).matchPrice(facts.matchPrice()).volume(facts.volume()).amount(facts.amount())
                .unmatchedBuyVolume(facts.unmatchedBuyVolume()).unmatchedSellVolume(facts.unmatchedSellVolume())
                .time(facts.time()).unmatchedDelta(facts.unmatchedDelta()).strengthLabel(facts.strengthLabel())
                .anchorExact(facts.anchorExact()).anchorSource(facts.anchorSource()).anchorTargetTime(facts.anchorTargetTime())
                .priceRange(toPriceRange(facts.priceRange())).recentPriceTrend(facts.recentPriceTrend())
                .recentPriceChange(facts.recentPriceChange()).recentVolumeDelta(facts.recentVolumeDelta())
                .directionStability(facts.directionStability()).directionFlipCount(facts.directionFlipCount())
                .dominantDirection(facts.dominantDirection()).imbalancePressure(facts.imbalancePressure())
                .dataConfidence(facts.dataConfidence())
                .build();
    }

    private StockChartDTO.AuctionPriceRangeDTO toPriceRange(StockAuctionDomainPolicy.PriceRange value) {
        if (value == null) {
            return null;
        }
        return StockChartDTO.AuctionPriceRangeDTO.builder()
                .low(value.low()).high(value.high()).spread(value.spread())
                .build();
    }

    private StockChartDTO.MinuteBarsDTO minuteBars(Map<String, List<StockChartDTO.KlineBarDTO>> bars) {
        return StockChartDTO.MinuteBarsDTO.builder()
                .oneMinute(bars.getOrDefault("1m", List.of()))
                .fiveMinute(bars.getOrDefault("5m", List.of()))
                .fifteenMinute(bars.getOrDefault("15m", List.of()))
                .thirtyMinute(bars.getOrDefault("30m", List.of()))
                .sixtyMinute(bars.getOrDefault("60m", List.of()))
                .build();
    }

    private String formatOffsetDateTime(OffsetDateTime value) {
        return value == null ? null : value.toString();
    }
}
