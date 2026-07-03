package cn.cccs7.cynexus.market.application.service;

import cn.cccs7.cynexus.market.api.dto.StockChartDTO;
import cn.cccs7.cynexus.market.domain.entity.StockChartBO;
import cn.cccs7.cynexus.market.domain.repository.StockChartQueryRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class StockChartQueryAppService {

    private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
    private static final int DEFAULT_KLINE_COUNT = 520;

    private final StockChartQueryRepository stockChartQueryRepository;

    public StockChartQueryAppService(StockChartQueryRepository stockChartQueryRepository) {
        this.stockChartQueryRepository = stockChartQueryRepository;
    }

    public StockChartDTO.SearchResponseDTO search(String keyword) {
        return StockChartDTO.SearchResponseDTO.builder()
                .items(stockChartQueryRepository.search(keyword, 20).stream()
                        .map(this::searchItem)
                        .toList())
                .build();
    }

    public StockChartDTO.KlineResponseDTO klines(String targetType, String symbol, String period, String adjust) {
        String normalizedPeriod = normalizePeriod(period);
        List<StockChartBO.Bar> rows;
        String source;
        if ("1d".equals(normalizedPeriod)) {
            String normalizedTargetType = blankDefault(targetType, "stock");
            String normalizedAdjust = blankDefault(adjust, "qfq");
            rows = stockChartQueryRepository.listDailyBars(targetType, symbol, DEFAULT_KLINE_COUNT, adjust);
            if ("stock".equalsIgnoreCase(normalizedTargetType) && "qfq".equalsIgnoreCase(normalizedAdjust)) {
                source = rows.isEmpty() ? "clickhouse.daily_qfq-empty" : "clickhouse.daily_qfq";
            } else {
                source = rows.isEmpty() ? "java-tdx-local-empty" : "java-tdx-local";
            }
        } else {
            int periodMinutes = periodMinutes(normalizedPeriod);
            LocalDate effectiveDate = stockChartQueryRepository
                    .findLatestMinuteDate(targetType, symbol, LocalDate.now(SHANGHAI))
                    .orElse(null);
            rows = effectiveDate == null
                    ? List.of()
                    : stockChartQueryRepository.listMinuteBars(targetType, symbol, effectiveDate, periodMinutes);
            source = rows.isEmpty() ? "clickhouse.ch_dwd_kline_1m-empty" : "clickhouse.ch_dwd_kline_1m";
        }
        return StockChartDTO.KlineResponseDTO.builder()
                .symbol(symbol)
                .targetType(blankDefault(targetType, "stock"))
                .period(normalizedPeriod)
                .adjust(blankDefault(adjust, "qfq"))
                .updatedAt(OffsetDateTime.now(SHANGHAI))
                .source(source)
                .items(rows.stream().map(this::bar).toList())
                .build();
    }

    public StockChartDTO.StockMetaDTO stockMeta(String symbol) {
        StockChartBO.StockMeta value = stockChartQueryRepository.stockMeta(symbol);
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
        StockChartBO.StockSectors value = stockChartQueryRepository.stockSectors(symbol);
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
        StockChartBO.FundFlowSnapshot value = stockChartQueryRepository.fundFlow(symbol);
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
        StockChartBO.SectorStocks value = stockChartQueryRepository.sectorStocks(sectorId, categoryRaw, topicId, name, limit);
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
        LocalDate requested = parseDateOrToday(tradeDate);
        LocalDate effectiveDate = stockChartQueryRepository.findLatestMinuteDate(targetType, symbol, requested).orElse(null);
        if (effectiveDate == null) {
            return StockChartDTO.IntradayDTO.builder()
                    .ok(true)
                    .symbol(symbol)
                    .targetType(blankDefault(targetType, "stock"))
                    .name(blankDefault(name, symbol))
                    .adjust(blankDefault(adjust, "qfq"))
                    .effectiveAdjust("none")
                    .requestedAdjust(blankDefault(adjust, "qfq"))
                    .source("clickhouse.ch_dwd_kline_1m-empty")
                    .requestedTradeDate(requested)
                    .timeshare(List.of())
                    .minuteBars(emptyMinuteBars())
                    .error("minute kline / timeshare data not found")
                    .build();
        }

        Map<String, List<StockChartDTO.KlineBarDTO>> minuteBars = new HashMap<>();
        for (String period : periods) {
            int minutes = periodMinutes(period);
            minuteBars.put(period, stockChartQueryRepository.listMinuteBars(targetType, symbol, effectiveDate, minutes).stream()
                    .map(this::bar)
                    .toList());
        }
        List<StockChartDTO.IntradayPointDTO> timeshare = stockChartQueryRepository.listTimeshare(targetType, symbol, effectiveDate).stream()
                .map(this::intradayPoint)
                .toList();
        boolean empty = timeshare.isEmpty() && minuteBars.values().stream().allMatch(List::isEmpty);
        return StockChartDTO.IntradayDTO.builder()
                .ok(true)
                .symbol(symbol)
                .targetType(blankDefault(targetType, "stock"))
                .name(blankDefault(name, symbol))
                .adjust(blankDefault(adjust, "qfq"))
                .effectiveAdjust("none")
                .requestedAdjust(blankDefault(adjust, "qfq"))
                .source(empty ? "clickhouse.ch_dwd_kline_1m-empty" : "clickhouse.ch_dwd_kline_1m")
                .tradeDate(effectiveDate)
                .requestedTradeDate(requested)
                .timeshare(timeshare)
                .minuteBars(minuteBars(minuteBars))
                .error(empty ? "minute kline / timeshare data not found" : null)
                .build();
    }

    public StockChartDTO.AuctionDTO auction(String symbol) {
        LocalDate requested = LocalDate.now(SHANGHAI);
        LocalDate effectiveDate = stockChartQueryRepository.findLatestAuctionDate("stock", symbol, requested).orElse(null);
        if (effectiveDate == null) {
            return StockChartDTO.AuctionDTO.builder()
                    .symbol(symbol)
                    .tradeDate(requested)
                    .source("clickhouse.ch_ods_stock_auction_point-empty")
                    .error("auction detail data not found")
                    .opening(auctionPhase(List.of(), "09:25:00", "openingPoints"))
                    .closing(auctionPhase(List.of(), "15:00:00", "closingPoints"))
                    .details(StockChartDTO.AuctionDetailsDTO.builder()
                            .auction0925(Map.of("has_auction_0925", false))
                            .openingPoints(List.of())
                            .closingPoints(List.of())
                            .allPoints(List.of())
                            .build())
                    .build();
        }
        List<StockChartBO.AuctionPoint> rows = stockChartQueryRepository.listAuctionPoints("stock", symbol, effectiveDate);
        List<StockChartBO.AuctionPoint> openingRows = rows.stream()
                .filter(row -> "opening".equals(row.getPhase()) || "auction_0925".equals(row.getPhase()))
                .toList();
        List<StockChartBO.AuctionPoint> closingRows = rows.stream()
                .filter(row -> "closing".equals(row.getPhase()))
                .toList();
        StockChartBO.AuctionPoint auction0925 = rows.stream()
                .filter(row -> "auction_0925".equals(row.getPhase()) || "09:25:00".equals(row.getTimeLabel()))
                .findFirst()
                .orElse(null);
        return StockChartDTO.AuctionDTO.builder()
                .symbol(symbol)
                .tradeDate(effectiveDate)
                .source(rows.isEmpty() ? "clickhouse.ch_ods_stock_auction_point-empty" : "clickhouse.ch_ods_stock_auction_point")
                .error(rows.isEmpty() ? "auction detail data not found" : null)
                .opening(auctionPhase(openingRows, "09:25:00", "openingPoints"))
                .closing(auctionPhase(closingRows, "15:00:00", "closingPoints"))
                .details(StockChartDTO.AuctionDetailsDTO.builder()
                        .auction0925(auction0925(auction0925))
                        .openingPoints(openingRows.stream().map(this::auctionPoint).toList())
                        .closingPoints(closingRows.stream().map(this::auctionPoint).toList())
                        .allPoints(rows.stream().map(this::auctionPoint).toList())
                        .build())
                .build();
    }

    public StockChartDTO.F10ValuationDTO valuation(String symbol, String reqId) {
        StockChartBO.F10Valuation value = stockChartQueryRepository.valuation(symbol, reqId);
        return StockChartDTO.F10ValuationDTO.builder()
                .symbol(value.getSymbol())
                .reqId(value.getReqId())
                .rows(value.getRows() == null ? List.of() : value.getRows().stream().map(this::valuationRow).toList())
                .fetchedAt(formatOffsetDateTime(value.getFetchedAt()))
                .source(value.getSource())
                .build();
    }

    public StockChartDTO.F10FinanceReportDTO financeReport(String symbol, String reportType) {
        StockChartBO.F10FinanceReport value = stockChartQueryRepository.financeReport(symbol, reportType);
        return StockChartDTO.F10FinanceReportDTO.builder()
                .symbol(value.getSymbol())
                .reportType(value.getReportType())
                .rows(value.getRows() == null ? List.of() : value.getRows().stream().map(this::financeReportRow).toList())
                .fetchedAt(formatOffsetDateTime(value.getFetchedAt()))
                .source(value.getSource())
                .build();
    }

    public StockChartDTO.F10BusinessCompositionDTO businessComposition(String symbol, String reportDate) {
        StockChartBO.F10BusinessComposition value = stockChartQueryRepository.businessComposition(symbol, reportDate);
        return StockChartDTO.F10BusinessCompositionDTO.builder()
                .symbol(value.getSymbol())
                .reportDate(value.getReportDate())
                .items(value.getItems() == null ? List.of() : value.getItems().stream().map(this::businessCompositionItem).toList())
                .fetchedAt(formatOffsetDateTime(value.getFetchedAt()))
                .source(value.getSource())
                .build();
    }

    public StockChartDTO.F10ProfitForecastDTO profitForecast(String symbol) {
        StockChartBO.F10ProfitForecast value = stockChartQueryRepository.profitForecast(symbol);
        return StockChartDTO.F10ProfitForecastDTO.builder()
                .symbol(value.getSymbol())
                .items(value.getItems() == null ? List.of() : value.getItems().stream().map(this::profitForecastItem).toList())
                .fetchedAt(formatOffsetDateTime(value.getFetchedAt()))
                .source(value.getSource())
                .build();
    }

    public StockChartDTO.F10AnnouncementsDTO announcements(String symbol) {
        StockChartBO.F10Announcements value = stockChartQueryRepository.announcements(symbol);
        return StockChartDTO.F10AnnouncementsDTO.builder()
                .symbol(value.getSymbol())
                .items(value.getItems() == null ? List.of() : value.getItems().stream().map(this::announcementItem).toList())
                .count(value.getCount())
                .fetchedAt(formatOffsetDateTime(value.getFetchedAt()))
                .source(value.getSource())
                .build();
    }

    public StockChartDTO.F10NewsDTO news(String symbol) {
        StockChartBO.F10News value = stockChartQueryRepository.news(symbol);
        return StockChartDTO.F10NewsDTO.builder()
                .symbol(value.getSymbol())
                .items(value.getItems() == null ? List.of() : value.getItems().stream().map(this::newsItem).toList())
                .count(value.getCount())
                .fetchedAt(formatOffsetDateTime(value.getFetchedAt()))
                .source(value.getSource())
                .build();
    }

    public StockChartDTO.F10RoadshowsDTO roadshows(String symbol) {
        StockChartBO.F10Roadshows value = stockChartQueryRepository.roadshows(symbol);
        return StockChartDTO.F10RoadshowsDTO.builder()
                .symbol(value.getSymbol())
                .items(value.getItems() == null ? List.of() : value.getItems().stream().map(this::roadshowItem).toList())
                .count(value.getCount())
                .fetchedAt(formatOffsetDateTime(value.getFetchedAt()))
                .source(value.getSource())
                .build();
    }

    public StockChartDTO.F10CompanyNewsDTO companyNews(String symbol, String section) {
        StockChartBO.F10CompanyNews value = stockChartQueryRepository.companyNews(symbol, section);
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
                .activeNetAmountWan(value.getActiveNetAmountWan())
                .activeBuyRatio(value.getActiveBuyRatio())
                .activeSellRatio(value.getActiveSellRatio())
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

    private StockChartDTO.AuctionPointDTO auctionPoint(StockChartBO.AuctionPoint value) {
        BigDecimal unmatchedBuy = zeroDefault(value.getUnmatchedBuyVolume());
        BigDecimal unmatchedSell = zeroDefault(value.getUnmatchedSellVolume());
        BigDecimal unmatchedVolume = unmatchedBuy.add(unmatchedSell);
        return StockChartDTO.AuctionPointDTO.builder()
                .timeLabel(value.getTimeLabel())
                .price(value.getPrice())
                .matchedVolume(value.getMatchedVolume())
                .unmatchedVolume(unmatchedVolume)
                .unmatchedDirectionRaw(directionRaw(unmatchedBuy, unmatchedSell))
                .matchedAmountEstimated(value.getMatchedAmount())
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

    private StockChartDTO.AuctionPhaseDTO auctionPhase(List<StockChartBO.AuctionPoint> rows,
                                                       String anchorTargetTime,
                                                       String anchorSourcePrefix) {
        if (rows == null || rows.isEmpty()) {
            return StockChartDTO.AuctionPhaseDTO.builder()
                    .anchorTargetTime(anchorTargetTime)
                    .anchorExact(false)
                    .dataConfidence("低")
                    .build();
        }
        StockChartBO.AuctionPoint anchor = chooseAnchor(rows, anchorTargetTime);
        BigDecimal unmatchedBuy = zeroDefault(anchor.getUnmatchedBuyVolume());
        BigDecimal unmatchedSell = zeroDefault(anchor.getUnmatchedSellVolume());
        BigDecimal unmatchedDelta = unmatchedBuy.subtract(unmatchedSell);
        boolean exact = anchorTargetTime.equals(anchor.getTimeLabel());
        return StockChartDTO.AuctionPhaseDTO.builder()
                .price(anchor.getPrice())
                .matchPrice(anchor.getPrice())
                .volume(anchor.getMatchedVolume())
                .amount(anchor.getMatchedAmount())
                .unmatchedBuyVolume(anchor.getUnmatchedBuyVolume())
                .unmatchedSellVolume(anchor.getUnmatchedSellVolume())
                .time(anchor.getTimeLabel())
                .unmatchedDelta(unmatchedDelta)
                .strengthLabel(strengthLabel(unmatchedDelta))
                .anchorExact(exact)
                .anchorSource(anchorSourcePrefix + (exact ? "-exact" : "-nearest"))
                .anchorTargetTime(anchorTargetTime)
                .priceRange(priceRange(rows))
                .recentPriceTrend(recentPriceTrend(rows))
                .recentPriceChange(recentPriceChange(rows))
                .recentVolumeDelta(recentVolumeDelta(rows))
                .directionStability(directionStability(rows))
                .directionFlipCount(directionFlipCount(rows))
                .dominantDirection(strengthLabel(sumUnmatchedDelta(rows)))
                .imbalancePressure(imbalancePressure(unmatchedBuy, unmatchedSell))
                .dataConfidence(exact ? "高" : "中")
                .build();
    }

    private StockChartBO.AuctionPoint chooseAnchor(List<StockChartBO.AuctionPoint> rows, String anchorTargetTime) {
        return rows.stream()
                .filter(row -> anchorTargetTime.equals(row.getTimeLabel()))
                .findFirst()
                .orElseGet(() -> rows.stream()
                        .filter(row -> row.getTimeLabel() != null && row.getTimeLabel().compareTo(anchorTargetTime) <= 0)
                        .max(Comparator.comparing(StockChartBO.AuctionPoint::getTimeLabel))
                        .orElse(rows.getLast()));
    }

    private Map<String, Object> auction0925(StockChartBO.AuctionPoint value) {
        if (value == null) {
            return Map.of("has_auction_0925", false);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("has_auction_0925", true);
        result.put("price", value.getPrice());
        result.put("volume", value.getMatchedVolume());
        result.put("amount", value.getMatchedAmount());
        return result;
    }

    private StockChartDTO.AuctionPriceRangeDTO priceRange(List<StockChartBO.AuctionPoint> rows) {
        List<BigDecimal> prices = rows.stream()
                .map(StockChartBO.AuctionPoint::getPrice)
                .filter(item -> item != null)
                .toList();
        if (prices.isEmpty()) {
            return null;
        }
        BigDecimal low = prices.stream().min(Comparator.naturalOrder()).orElse(null);
        BigDecimal high = prices.stream().max(Comparator.naturalOrder()).orElse(null);
        return StockChartDTO.AuctionPriceRangeDTO.builder()
                .low(low)
                .high(high)
                .spread(low == null || high == null ? null : high.subtract(low))
                .build();
    }

    private String recentPriceTrend(List<StockChartBO.AuctionPoint> rows) {
        BigDecimal change = recentPriceChange(rows);
        if (change == null) {
            return null;
        }
        int compared = change.compareTo(BigDecimal.ZERO);
        if (compared > 0) {
            return "上行";
        }
        if (compared < 0) {
            return "下行";
        }
        return "走平";
    }

    private BigDecimal recentPriceChange(List<StockChartBO.AuctionPoint> rows) {
        List<BigDecimal> prices = rows.stream()
                .map(StockChartBO.AuctionPoint::getPrice)
                .filter(item -> item != null)
                .toList();
        if (prices.size() < 2) {
            return null;
        }
        return prices.getLast().subtract(prices.get(prices.size() - 2));
    }

    private BigDecimal recentVolumeDelta(List<StockChartBO.AuctionPoint> rows) {
        List<BigDecimal> volumes = rows.stream()
                .map(StockChartBO.AuctionPoint::getMatchedVolume)
                .filter(item -> item != null)
                .toList();
        if (volumes.size() < 2) {
            return null;
        }
        return volumes.getLast().subtract(volumes.get(volumes.size() - 2));
    }

    private int directionFlipCount(List<StockChartBO.AuctionPoint> rows) {
        int flips = 0;
        Integer previous = null;
        for (StockChartBO.AuctionPoint row : rows) {
            int current = directionRaw(zeroDefault(row.getUnmatchedBuyVolume()), zeroDefault(row.getUnmatchedSellVolume()));
            if (current == 0) {
                continue;
            }
            if (previous != null && previous != current) {
                flips++;
            }
            previous = current;
        }
        return flips;
    }

    private String directionStability(List<StockChartBO.AuctionPoint> rows) {
        int flips = directionFlipCount(rows);
        if (flips == 0) {
            return "方向稳定";
        }
        if (flips <= 2) {
            return "方向轻微切换";
        }
        return "方向频繁切换";
    }

    private String strengthLabel(BigDecimal delta) {
        if (delta == null) {
            return null;
        }
        int compared = delta.compareTo(BigDecimal.ZERO);
        if (compared > 0) {
            return "买方占优";
        }
        if (compared < 0) {
            return "卖方占优";
        }
        return "均衡";
    }

    private BigDecimal sumUnmatchedDelta(List<StockChartBO.AuctionPoint> rows) {
        BigDecimal result = BigDecimal.ZERO;
        for (StockChartBO.AuctionPoint row : rows) {
            result = result
                    .add(zeroDefault(row.getUnmatchedBuyVolume()))
                    .subtract(zeroDefault(row.getUnmatchedSellVolume()));
        }
        return result;
    }

    private BigDecimal imbalancePressure(BigDecimal unmatchedBuy, BigDecimal unmatchedSell) {
        BigDecimal total = unmatchedBuy.add(unmatchedSell);
        if (BigDecimal.ZERO.compareTo(total) == 0) {
            return null;
        }
        return unmatchedBuy.subtract(unmatchedSell).abs().divide(total, 6, RoundingMode.HALF_UP);
    }

    private int directionRaw(BigDecimal unmatchedBuy, BigDecimal unmatchedSell) {
        return unmatchedBuy.compareTo(unmatchedSell) > 0 ? 1 : unmatchedSell.compareTo(unmatchedBuy) > 0 ? -1 : 0;
    }

    private BigDecimal zeroDefault(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
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

    private StockChartDTO.MinuteBarsDTO emptyMinuteBars() {
        return StockChartDTO.MinuteBarsDTO.builder()
                .oneMinute(List.of())
                .fiveMinute(List.of())
                .fifteenMinute(List.of())
                .thirtyMinute(List.of())
                .sixtyMinute(List.of())
                .build();
    }

    private String normalizePeriod(String period) {
        String value = period == null || period.isBlank() ? "1d" : period.trim().toLowerCase();
        return switch (value) {
            case "1", "1min", "1m" -> "1m";
            case "5", "5min", "5m" -> "5m";
            case "15", "15min", "15m" -> "15m";
            case "30", "30min", "30m" -> "30m";
            case "60", "60min", "60m" -> "60m";
            case "120", "120min", "120m" -> "120m";
            case "day", "daily", "1d" -> "1d";
            default -> "1d";
        };
    }

    private int periodMinutes(String period) {
        String normalized = normalizePeriod(period);
        return switch (normalized) {
            case "1m" -> 1;
            case "5m" -> 5;
            case "15m" -> 15;
            case "30m" -> 30;
            case "60m" -> 60;
            case "120m" -> 120;
            default -> 1;
        };
    }

    private String blankDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private LocalDate parseDateOrToday(String value) {
        return value == null || value.isBlank() ? LocalDate.now(SHANGHAI) : LocalDate.parse(value);
    }

    private String formatOffsetDateTime(OffsetDateTime value) {
        return value == null ? null : value.toString();
    }
}
