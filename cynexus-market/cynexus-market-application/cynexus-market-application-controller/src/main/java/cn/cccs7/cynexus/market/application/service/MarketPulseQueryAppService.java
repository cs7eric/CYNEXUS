package cn.cccs7.cynexus.market.application.service;

import cn.cccs7.cynexus.market.api.dto.MarketPulseHeatmapDTO;
import cn.cccs7.cynexus.market.api.dto.MarketPulseIndexKlineBatchDTO;
import cn.cccs7.cynexus.market.api.dto.MarketPulseIndexReturnsDTO;
import cn.cccs7.cynexus.market.api.dto.MarketPulseIndexReturnsHistoryDTO;
import cn.cccs7.cynexus.market.api.dto.MarketPulseLimitEmotionDTO;
import cn.cccs7.cynexus.market.api.dto.MarketPulseManualFundFlowDTO;
import cn.cccs7.cynexus.market.api.dto.MarketPulseOverviewDTO;
import cn.cccs7.cynexus.market.api.dto.MarketPulseOverviewEltdxDTO;
import cn.cccs7.cynexus.market.api.dto.MarketPulseOverviewHistoryDTO;
import cn.cccs7.cynexus.market.api.dto.MarketPulseStyleSectorConstituentsDTO;
import cn.cccs7.cynexus.market.api.dto.MarketPulseStyleSectorListDTO;
import cn.cccs7.cynexus.market.api.request.MarketPulseManualFundFlowRequest;
import cn.cccs7.cynexus.market.application.convert.MarketPulseIndexKlinePointDTOConverter;
import cn.cccs7.cynexus.market.application.convert.MarketPulseIndexReturnItemDTOConverter;
import cn.cccs7.cynexus.market.application.convert.MarketPulseLimitEmotionStockDTOConverter;
import cn.cccs7.cynexus.market.application.convert.MarketPulseOverviewResponseDTOConverter;
import cn.cccs7.cynexus.market.application.convert.MarketPulseStyleSectorItemDTOConverter;
import cn.cccs7.cynexus.market.domain.entity.IndexMinuteBarBO;
import cn.cccs7.cynexus.market.domain.entity.MarketLimitDailySnapshotBO;
import cn.cccs7.cynexus.market.domain.entity.MarketOverviewBO;
import cn.cccs7.cynexus.market.domain.entity.StyleSectorSnapshotBO;
import cn.cccs7.cynexus.market.domain.entity.ThsIndustryConstituentSnapshotBO;
import cn.cccs7.cynexus.market.domain.repository.MarketPulseQueryRepository;
import cn.cccs7.cynexus.market.domain.repository.ThsIndustryConstituentSnapshotRepository;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MarketPulseQueryAppService {

    private static final List<String> DEFAULT_INDEX_CODES = List.of("000001", "399001", "399006");
    private static final Logger LOGGER = LoggerFactory.getLogger(MarketPulseQueryAppService.class);

    private final MarketPulseQueryRepository marketPulseQueryRepository;
    private final ThsIndustryConstituentSnapshotRepository thsIndustryConstituentSnapshotRepository;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public MarketPulseQueryAppService(MarketPulseQueryRepository marketPulseQueryRepository,
                                      ThsIndustryConstituentSnapshotRepository thsIndustryConstituentSnapshotRepository) {
        this.marketPulseQueryRepository = marketPulseQueryRepository;
        this.thsIndustryConstituentSnapshotRepository = thsIndustryConstituentSnapshotRepository;
    }

    public MarketPulseOverviewDTO getOverviewAkshare() {
        MarketOverviewBO row = marketPulseQueryRepository.findLatestOverview().orElse(null);
        if (row == null) {
            return overviewError("market overview is empty");
        }
        applyServingFacts(row);
        return overviewPayload(row, marketPulseQueryRepository.findPreviousOverview(row.getTradeDate()).orElse(null));
    }

    public MarketPulseOverviewEltdxDTO getOverviewEltdx() {
        MarketOverviewBO row = marketPulseQueryRepository.findLatestOverview().orElse(null);
        if (row == null) {
            return MarketPulseOverviewEltdxDTO.builder().ok(false).error("market overview is empty").build();
        }
        applyServingFacts(row);
        MarketOverviewBO previous = marketPulseQueryRepository.findPreviousOverview(row.getTradeDate()).orElse(null);
        return MarketPulseOverviewEltdxDTO.builder()
                .ok(true)
                .tradingDate(row.getTradeDate())
                .fetchedAt(row.getUpdatedAt())
                .totalAmount(row.getTotalAmount())
                .totalVolume(row.getTotalVolume())
                .risingCount(row.getRisingCount())
                .fallingCount(row.getFallingCount())
                .flatCount(row.getFlatCount())
                .limitUpCount(row.getLimitUpCount())
                .limitDownCount(row.getLimitDownCount())
                .stockCount(row.getStockCount())
                .prevDayTotalAmount(previous == null ? null : previous.getTotalAmount())
                .prevDayTradingDate(previous == null ? null : previous.getTradeDate())
                .build();
    }

    public MarketPulseOverviewDTO getOverviewArchive(String tradingDate) {
        LocalDate date = LocalDate.parse(normalizeDate(tradingDate));
        MarketOverviewBO row = marketPulseQueryRepository.findOverviewByDate(date).orElse(null);
        if (row == null) {
            return overviewError("market overview archive not found");
        }
        applyServingFacts(row);
        return overviewPayload(row, marketPulseQueryRepository.findPreviousOverview(date).orElse(null));
    }

    public MarketPulseOverviewHistoryDTO getOverviewHistory(String range) {
        String normalizedRange = range == null ? "60d" : range;
        int days = switch (normalizedRange) {
            case "20d" -> 20;
            case "120d" -> 120;
            case "1y" -> 365;
            default -> 60;
        };
        LocalDate start = LocalDate.now().minusDays(days * 2L);
        List<MarketOverviewBO> rows = marketPulseQueryRepository.listOverviewSince(start, days);
        applyServingFacts(rows);
        List<MarketPulseOverviewHistoryDTO.ItemDTO> items = rows.stream()
                .sorted(Comparator.comparing(MarketOverviewBO::getTradeDate))
                .map(this::historyItem)
                .toList();
        return MarketPulseOverviewHistoryDTO.builder()
                .ok(true)
                .range(normalizedRange)
                .source("java-pg")
                .count(items.size())
                .items(items)
                .build();
    }

    public Optional<MarketPulseManualFundFlowDTO> getManualFundFlow(LocalDate tradeDate) {
        return marketPulseQueryRepository.findManualFundFlow(tradeDate).map(row -> manualPayload(row, true));
    }

    public MarketPulseManualFundFlowDTO saveManualFundFlow(MarketPulseManualFundFlowRequest request) {
        MarketOverviewBO value = MarketOverviewBO.builder()
                .mainNetInflow(request.getMainNetInflow())
                .mainNetInflowRatio(request.getMainNetInflowRatio())
                .superLargeNetInflow(request.getSuperLargeNetInflow())
                .superLargeNetRatio(request.getSuperLargeNetInflowRatio())
                .largeNetInflow(request.getLargeNetInflow())
                .largeNetRatio(request.getLargeNetInflowRatio())
                .mediumNetInflow(request.getMediumNetInflow())
                .mediumNetRatio(request.getMediumNetInflowRatio())
                .smallNetInflow(request.getSmallNetInflow())
                .smallNetRatio(request.getSmallNetInflowRatio())
                .build();
        marketPulseQueryRepository.upsertManualFundFlow(request.getTradingDate(), value);
        return getManualFundFlow(request.getTradingDate())
                .orElseGet(() -> MarketPulseManualFundFlowDTO.builder()
                        .ok(false)
                        .error("manual fund flow not found after save")
                        .tradingDate(request.getTradingDate())
                        .build());
    }

    public MarketPulseIndexReturnsDTO getIndexReturns(int days) {
        List<MarketPulseIndexReturnsDTO.ItemDTO> items = MarketPulseIndexReturnItemDTOConverter.INSTANCE
                .toLatestItems(marketPulseQueryRepository.listLatestIndexReturns(days));
        return MarketPulseIndexReturnsDTO.builder()
                .ok(true)
                .days(days)
                .items(items)
                .build();
    }

    public MarketPulseIndexReturnsHistoryDTO getIndexReturnsHistory(int window, LocalDate startDate, LocalDate endDate) {
        List<MarketPulseIndexReturnsHistoryDTO.ItemDTO> items = MarketPulseIndexReturnItemDTOConverter.INSTANCE
                .toHistoryItems(marketPulseQueryRepository.listIndexReturnsHistory(window, startDate, endDate));
        return MarketPulseIndexReturnsHistoryDTO.builder()
                .ok(true)
                .window(window)
                .start(startDate)
                .end(endDate)
                .count(items.size())
                .items(items)
                .build();
    }

    public MarketPulseIndexKlineBatchDTO getIndexKlineBatch(List<String> codes, LocalDate date, String interval) {
        List<String> targetCodes = codes == null || codes.isEmpty() ? DEFAULT_INDEX_CODES : codes;
        List<MarketPulseIndexKlineBatchDTO.ItemDTO> items = new ArrayList<>();
        List<String> returnedDates = new ArrayList<>();
        String normalizedInterval = interval == null ? "1m" : interval;
        for (String code : targetCodes) {
            LocalDate tradeDate = marketPulseQueryRepository.findLatestIndexMinuteDate(code, date).orElse(date);
            List<IndexMinuteBarBO> points = marketPulseQueryRepository.listIndexMinuteBars(code, tradeDate);
            BigDecimal previousClose = marketPulseQueryRepository.findIndexPreviousClose(code, tradeDate).orElse(null);
            items.add(MarketPulseIndexKlineBatchDTO.ItemDTO.builder()
                    .ok(!points.isEmpty())
                    .code(code)
                    .name(indexName(code))
                    .date(tradeDate.toString())
                    .interval(normalizedInterval)
                    .previousClose(previousClose)
                    .source("clickhouse.ch_dwd_kline_1m")
                    .points(MarketPulseIndexKlinePointDTOConverter.INSTANCE.toSourceList(points))
                    .error(points.isEmpty() ? "index minute bars not found" : null)
                    .build());
            returnedDates.add(tradeDate.toString());
        }
        return MarketPulseIndexKlineBatchDTO.builder()
                .ok(true)
                .date(returnedDates.isEmpty() ? date.toString() : returnedDates.get(0))
                .interval(normalizedInterval)
                .items(items)
                .build();
    }

    public MarketPulseStyleSectorListDTO getStyleSectors() {
        List<StyleSectorSnapshotBO> rows = marketPulseQueryRepository.listLatestStyleSectors();
        List<MarketPulseStyleSectorListDTO.ItemDTO> items = MarketPulseStyleSectorItemDTOConverter.INSTANCE.toSourceList(rows);
        return MarketPulseStyleSectorListDTO.builder()
                .ok(true)
                .items(items)
                .count(items.size())
                .names(rows.stream().map(StyleSectorSnapshotBO::getName).toList())
                .build();
    }

    public MarketPulseStyleSectorConstituentsDTO getStyleSectorConstituents(String name) {
        StyleSectorSnapshotBO row = marketPulseQueryRepository.findLatestStyleSector(name).orElse(null);
        List<MarketPulseStyleSectorConstituentsDTO.ConstituentDTO> constituents = List.of();
        if (row != null) {
            MarketPulseStyleSectorConstituentsDTO parsed = readJson(row.getPayload(), MarketPulseStyleSectorConstituentsDTO.class);
            if (parsed != null && parsed.getConstituents() != null) {
                constituents = parsed.getConstituents();
            }
        }
        List<String> codes = constituents.stream()
                .map(MarketPulseStyleSectorConstituentsDTO.ConstituentDTO::getCode)
                .filter(code -> code != null && !code.isBlank())
                .toList();
        return MarketPulseStyleSectorConstituentsDTO.builder()
                .ok(true)
                .name(name)
                .codes(codes)
                .constituents(constituents)
                .sampleSize(row == null ? 0 : row.getSampleSize())
                .validSize(row == null ? 0 : row.getValidSize())
                .changePct(row == null ? null : row.getChangePct())
                .fetchedAt(row == null ? OffsetDateTime.now().toString() : timeString(row.getIngestedAt()))
                .build();
    }

    public MarketPulseStyleSectorConstituentsDTO getIndustryConstituents(String industryCode) {
        List<ThsIndustryConstituentSnapshotBO> rows = thsIndustryConstituentSnapshotRepository.listLatestByIndustryCode(industryCode);
        LocalDate snapshotDate = rows.stream()
                .map(ThsIndustryConstituentSnapshotBO::getSnapshotDate)
                .filter(item -> item != null)
                .findFirst()
                .orElse(null);
        String rowsFetchedAt = rows.stream()
                .map(ThsIndustryConstituentSnapshotBO::getCapturedAt)
                .filter(item -> item != null)
                .max(Comparator.naturalOrder())
                .map(OffsetDateTime::toString)
                .orElse(null);
        String name = rows.stream()
                .map(ThsIndustryConstituentSnapshotBO::getIndustryCode)
                .filter(item -> item != null && !item.isBlank())
                .findFirst()
                .orElse(industryCode);
        return MarketPulseStyleSectorConstituentsDTO.builder()
                .ok(true)
                .name(name)
                .code(industryCode)
                .count(rows.size())
                .matched(rows.size())
                .indexFetchedAt(null)
                .rowsFetchedAt(rowsFetchedAt)
                .dataSource("pg")
                .isTradingDay(true)
                .isMarketOpen(false)
                .tradingHoursMode("trading_day_off_hours")
                .snapshotDate(snapshotDate)
                .rows(rows.stream().map(this::industryRow).toList())
                .build();
    }

    public MarketPulseHeatmapDTO getHeatmap(String kind) {
        return marketPulseQueryRepository.findLatestHeatmap(kind)
                .map(row -> {
                    MarketPulseHeatmapDTO parsed = readJson(row.getPayload(), MarketPulseHeatmapDTO.class);
                    if (parsed == null) {
                        return emptyHeatmap(kind);
                    }
                    parsed.setOk(true);
                    if (parsed.getKind() == null) {
                        parsed.setKind(kind);
                    }
                    if (parsed.getSource() == null) {
                        parsed.setSource(row.getSource());
                    }
                    if (parsed.getItems() == null) {
                        parsed.setItems(List.of());
                    }
                    parsed.getItems().forEach(this::normalizeHeatmapItem);
                    if (parsed.getCount() == null) {
                        parsed.setCount(parsed.getItems().size());
                    }
                    return parsed;
                })
                .orElseGet(() -> emptyHeatmap(kind));
    }

    public MarketPulseLimitEmotionDTO getLimitEmotion() {
        MarketLimitDailySnapshotBO row = marketPulseQueryRepository.findLatestLimitEmotion().orElse(null);
        if (row == null) {
            return MarketPulseLimitEmotionDTO.builder().ok(false).error("limit emotion is empty").build();
        }
        MarketPulseLimitEmotionDTO parsed = readJson(row.getExtra(), MarketPulseLimitEmotionDTO.class);
        if (parsed != null && parsed.getLimitUp() != null) {
            normalizeLimitEmotion(parsed, row);
            return parsed;
        }
        return MarketPulseLimitEmotionDTO.builder()
                .ok(true)
                .tradeDate(row.getTradeDate())
                .updateTime(timeString(row.getUpdatedAt()))
                .marketStatus(row.getMarketStatus())
                .dataStatus(row.getDataStatus() == null ? "normal" : row.getDataStatus())
                .limitUp(limitBlock(row.getTradeDate(), "limit_up", "涨停", row.getLimitUpCount()))
                .limitDown(limitBlock(row.getTradeDate(), "limit_down", "跌停", row.getLimitDownCount()))
                .breakBoard(breakBoardBlock(row))
                .streak(streakBlock(row))
                .build();
    }

    private MarketPulseStyleSectorConstituentsDTO.IndustryRowDTO industryRow(ThsIndustryConstituentSnapshotBO row) {
        return MarketPulseStyleSectorConstituentsDTO.IndustryRowDTO.builder()
                .rank(row.getRank())
                .code(row.getStockCode())
                .name(row.getStockName())
                .price(row.getPrice())
                .changePct(row.getChangePct())
                .change(row.getChange())
                .speedPct(row.getSpeedPct())
                .turnoverPct(row.getTurnoverPct())
                .volumeRatio(row.getVolumeRatio())
                .amplitudePct(row.getAmplitudePct())
                .amount(row.getAmountText())
                .floatShares(row.getFloatSharesText())
                .floatMarketCap(row.getFloatMarketCapText())
                .pe(row.getPe())
                .build();
    }

    private MarketPulseOverviewDTO overviewPayload(MarketOverviewBO row, MarketOverviewBO previous) {
        MarketPulseOverviewDTO payload = MarketPulseOverviewResponseDTOConverter.INSTANCE.toSource(row);
        payload.setSource(row.getSource() == null ? "java-pg" : row.getSource());
        if (previous == null) {
            payload.setPrevDayFlow(null);
        } else {
            payload.setPrevDayFlow(MarketPulseOverviewDTO.MarketPulsePrevDayFlowDTO.builder()
                    .mainNetInflow(previous.getMainNetInflow())
                    .superLargeNetInflow(previous.getSuperLargeNetInflow())
                    .largeNetInflow(previous.getLargeNetInflow())
                    .mediumNetInflow(previous.getMediumNetInflow())
                    .smallNetInflow(previous.getSmallNetInflow())
                    .totalAmount(previous.getTotalAmount())
                    .build());
            payload.setPrevDayTradingDate(previous.getTradeDate());
        }
        return payload;
    }

    private MarketPulseOverviewHistoryDTO.ItemDTO historyItem(MarketOverviewBO row) {
        return MarketPulseOverviewHistoryDTO.ItemDTO.builder()
                .date(row.getTradeDate())
                .tradeDate(row.getTradeDate())
                .totalAmount(row.getTotalAmount())
                .totalVolume(row.getTotalVolume())
                .risingCount(row.getRisingCount())
                .fallingCount(row.getFallingCount())
                .flatCount(row.getFlatCount())
                .limitUpCount(row.getLimitUpCount())
                .limitDownCount(row.getLimitDownCount())
                .mainNetInflow(row.getMainNetInflow())
                .superLargeNetInflow(row.getSuperLargeNetInflow())
                .largeNetInflow(row.getLargeNetInflow())
                .mediumNetInflow(row.getMediumNetInflow())
                .smallNetInflow(row.getSmallNetInflow())
                .source(row.getSource() == null ? "java-pg" : row.getSource())
                .build();
    }

    private MarketPulseManualFundFlowDTO manualPayload(MarketOverviewBO row, boolean ok) {
        return MarketPulseManualFundFlowDTO.builder()
                .ok(ok)
                .tradingDate(row.getTradeDate())
                .savedAt(row.getManualUpdatedAt())
                .source("manual")
                .mainNetInflow(row.getMainNetInflow())
                .mainNetInflowRatio(row.getMainNetInflowRatio())
                .superLargeNetInflow(row.getSuperLargeNetInflow())
                .superLargeNetInflowRatio(row.getSuperLargeNetRatio())
                .largeNetInflow(row.getLargeNetInflow())
                .largeNetInflowRatio(row.getLargeNetRatio())
                .mediumNetInflow(row.getMediumNetInflow())
                .mediumNetInflowRatio(row.getMediumNetRatio())
                .smallNetInflow(row.getSmallNetInflow())
                .smallNetInflowRatio(row.getSmallNetRatio())
                .build();
    }

    private MarketPulseLimitEmotionDTO.LimitBlockDTO limitBlock(LocalDate tradeDate,
                                                                String category,
                                                                String label,
                                                                Integer count) {
        return MarketPulseLimitEmotionDTO.LimitBlockDTO.builder()
                .count(count)
                .label(label)
                .stocks(stockItems(tradeDate, category, 80))
                .build();
    }

    private MarketPulseLimitEmotionDTO.BreakBoardDTO breakBoardBlock(MarketLimitDailySnapshotBO row) {
        return MarketPulseLimitEmotionDTO.BreakBoardDTO.builder()
                .touchedCount(row.getTouchedCount())
                .brokenCount(row.getBrokenCount())
                .rate(row.getBreakBoardRate())
                .status("ready")
                .label("炸板率")
                .brokenStocks(stockItems(row.getTradeDate(), "broken", 80))
                .build();
    }

    private MarketPulseLimitEmotionDTO.StreakDTO streakBlock(MarketLimitDailySnapshotBO row) {
        return MarketPulseLimitEmotionDTO.StreakDTO.builder()
                .maxHeight(row.getMaxStreakHeight())
                .label("连板高度")
                .leaders(List.of())
                .distribution(List.of())
                .promotion(MarketPulseLimitEmotionDTO.PromotionDTO.builder()
                        .overallRate(row.getPromotionOverallRate())
                        .levels(List.of())
                        .build())
                .broken(MarketPulseLimitEmotionDTO.BrokenDTO.builder()
                        .count(row.getBrokenCount())
                        .highStreakBrokenCount(0)
                        .stocks(List.of())
                        .build())
                .sentiment(MarketPulseLimitEmotionDTO.SentimentDTO.builder()
                        .level(row.getSentimentLevel() == null ? "normal" : row.getSentimentLevel())
                        .text(row.getSentimentText() == null ? "" : row.getSentimentText())
                        .build())
                .build();
    }

    private List<MarketPulseLimitEmotionDTO.StockDTO> stockItems(LocalDate tradeDate, String category, int limit) {
        return MarketPulseLimitEmotionStockDTOConverter.INSTANCE
                .toSourceList(marketPulseQueryRepository.listLimitStocks(tradeDate, category, limit));
    }

    private void applyServingFacts(MarketOverviewBO row) {
        if (row == null || row.getTradeDate() == null) {
            return;
        }
        marketPulseQueryRepository.findDailyBreadth(row.getTradeDate()).ifPresent(breadth -> applyBreadth(row, breadth));
        marketPulseQueryRepository.findLimitEmotionByDate(row.getTradeDate()).ifPresent(limit -> applyLimitCounts(row, limit));
    }

    private void applyServingFacts(List<MarketOverviewBO> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        LocalDate startDate = rows.stream()
                .map(MarketOverviewBO::getTradeDate)
                .filter(date -> date != null)
                .min(LocalDate::compareTo)
                .orElse(null);
        LocalDate endDate = rows.stream()
                .map(MarketOverviewBO::getTradeDate)
                .filter(date -> date != null)
                .max(LocalDate::compareTo)
                .orElse(null);
        Map<LocalDate, MarketOverviewBO> breadthByDate = startDate == null || endDate == null
                ? Map.of()
                : marketPulseQueryRepository.listDailyBreadth(startDate, endDate)
                        .stream()
                        .filter(item -> item.getTradeDate() != null)
                        .collect(Collectors.toMap(MarketOverviewBO::getTradeDate, Function.identity(), (left, right) -> right));
        for (MarketOverviewBO row : rows) {
            if (row.getTradeDate() == null) {
                continue;
            }
            MarketOverviewBO breadth = breadthByDate.get(row.getTradeDate());
            if (breadth != null) {
                applyBreadth(row, breadth);
            }
            marketPulseQueryRepository.findLimitEmotionByDate(row.getTradeDate()).ifPresent(limit -> applyLimitCounts(row, limit));
        }
    }

    private void applyBreadth(MarketOverviewBO row, MarketOverviewBO breadth) {
        row.setRisingCount(breadth.getRisingCount());
        row.setFallingCount(breadth.getFallingCount());
        row.setFlatCount(breadth.getFlatCount());
        row.setStockCount(breadth.getStockCount());
    }

    private void applyLimitCounts(MarketOverviewBO row, MarketLimitDailySnapshotBO limit) {
        row.setLimitUpCount(limit.getLimitUpCount());
        row.setLimitDownCount(limit.getLimitDownCount());
    }

    private void normalizeLimitEmotion(MarketPulseLimitEmotionDTO value, MarketLimitDailySnapshotBO row) {
        value.setOk(true);
        if (value.getTradeDate() == null) {
            value.setTradeDate(row.getTradeDate());
        }
        if (value.getUpdateTime() == null) {
            value.setUpdateTime(timeString(row.getUpdatedAt()));
        }
        if (value.getMarketStatus() == null) {
            value.setMarketStatus(row.getMarketStatus());
        }
        if (value.getDataStatus() == null) {
            value.setDataStatus(row.getDataStatus() == null ? "normal" : row.getDataStatus());
        }
        if (value.getLimitUp() == null) {
            value.setLimitUp(limitBlock(row.getTradeDate(), "limit_up", "涨停", row.getLimitUpCount()));
        } else {
            if (value.getLimitUp().getCount() == null) {
                value.getLimitUp().setCount(row.getLimitUpCount());
            }
            if (value.getLimitUp().getStocks() == null) {
                value.getLimitUp().setStocks(List.of());
            }
        }
        if (value.getLimitDown() == null) {
            value.setLimitDown(limitBlock(row.getTradeDate(), "limit_down", "跌停", row.getLimitDownCount()));
        } else {
            if (value.getLimitDown().getCount() == null) {
                value.getLimitDown().setCount(row.getLimitDownCount());
            }
            if (value.getLimitDown().getStocks() == null) {
                value.getLimitDown().setStocks(List.of());
            }
        }
        if (value.getBreakBoard() == null) {
            value.setBreakBoard(breakBoardBlock(row));
        } else if (value.getBreakBoard().getBrokenStocks() == null) {
            value.getBreakBoard().setBrokenStocks(List.of());
        }
        if (value.getStreak() == null) {
            value.setStreak(streakBlock(row));
            return;
        }
        if (value.getStreak().getMaxHeight() == null) {
            value.getStreak().setMaxHeight(row.getMaxStreakHeight());
        }
        if (value.getStreak().getLeaders() == null) {
            value.getStreak().setLeaders(List.of());
        }
        if (value.getStreak().getDistribution() == null) {
            value.getStreak().setDistribution(List.of());
        } else {
            value.getStreak().getDistribution().forEach(item -> {
                if (item.getStreak() == null) {
                    item.setStreak(item.getHeight());
                }
                if (item.getStocks() == null) {
                    item.setStocks(List.of());
                }
            });
            fillDistributionStocks(value);
        }
        if (value.getStreak().getPromotion() == null) {
            value.getStreak().setPromotion(MarketPulseLimitEmotionDTO.PromotionDTO.builder()
                    .overallRate(row.getPromotionOverallRate())
                    .levels(List.of())
                    .build());
        } else {
            if (value.getStreak().getPromotion().getLevels() == null) {
                value.getStreak().getPromotion().setLevels(List.of());
            }
            fillPromotionLevels(value);
        }
        if (value.getStreak().getBroken() == null) {
            value.getStreak().setBroken(MarketPulseLimitEmotionDTO.BrokenDTO.builder()
                    .count(row.getBrokenCount())
                    .highStreakBrokenCount(0)
                    .stocks(List.of())
                    .build());
        } else if (value.getStreak().getBroken().getStocks() == null) {
            value.getStreak().getBroken().setStocks(List.of());
        }
        if (value.getStreak().getSentiment() == null) {
            value.getStreak().setSentiment(MarketPulseLimitEmotionDTO.SentimentDTO.builder()
                    .level(row.getSentimentLevel() == null ? "normal" : row.getSentimentLevel())
                    .text(row.getSentimentText() == null ? "" : row.getSentimentText())
                    .build());
        }
    }

    private void fillDistributionStocks(MarketPulseLimitEmotionDTO value) {
        if (value.getLimitUp() == null || value.getLimitUp().getStocks() == null) {
            return;
        }
        List<MarketPulseLimitEmotionDTO.StockDTO> limitUpStocks = value.getLimitUp().getStocks();
        for (MarketPulseLimitEmotionDTO.DistributionRowDTO row : value.getStreak().getDistribution()) {
            if (row.getStreak() == null || !row.getStocks().isEmpty()) {
                continue;
            }
            row.setStocks(limitUpStocks.stream()
                    .filter(stock -> row.getStreak().equals(stock.getStreak()))
                    .toList());
        }
    }

    private void fillPromotionLevels(MarketPulseLimitEmotionDTO value) {
        if (value.getStreak() == null || value.getStreak().getPromotion() == null) {
            return;
        }
        Map<Integer, Integer> denominator = new LinkedHashMap<>();
        Map<Integer, Integer> numerator = new LinkedHashMap<>();
        if (value.getLimitUp() != null && value.getLimitUp().getStocks() != null) {
            for (MarketPulseLimitEmotionDTO.StockDTO stock : value.getLimitUp().getStocks()) {
                Integer streak = stock.getStreak();
                if (streak != null && streak > 1) {
                    int from = streak - 1;
                    denominator.put(from, denominator.getOrDefault(from, 0) + 1);
                    numerator.put(from, numerator.getOrDefault(from, 0) + 1);
                }
            }
        }
        if (value.getStreak().getBroken() != null && value.getStreak().getBroken().getStocks() != null) {
            for (MarketPulseLimitEmotionDTO.BrokenStockDTO stock : value.getStreak().getBroken().getStocks()) {
                Integer prev = stock.getPreviousStreak();
                if (prev != null && prev > 0) {
                    denominator.put(prev, denominator.getOrDefault(prev, 0) + 1);
                }
            }
        }
        List<MarketPulseLimitEmotionDTO.PromotionLevelDTO> levels = new ArrayList<>();
        int totalDenominator = 0;
        int totalNumerator = 0;
        for (Integer from : denominator.keySet()) {
            int yc = denominator.getOrDefault(from, 0);
            int tc = numerator.getOrDefault(from, 0);
            totalDenominator += yc;
            totalNumerator += tc;
            levels.add(MarketPulseLimitEmotionDTO.PromotionLevelDTO.builder()
                    .from(from)
                    .to(from + 1)
                    .yesterdayCount(yc)
                    .todayPromotedCount(tc)
                    .rate(yc > 0 ? BigDecimal.valueOf(tc).divide(BigDecimal.valueOf(yc), 4, java.math.RoundingMode.HALF_UP) : null)
                    .build());
        }
        value.getStreak().getPromotion().setLevels(levels);
        if (value.getStreak().getPromotion().getOverallRate() == null && totalDenominator > 0) {
            value.getStreak().getPromotion().setOverallRate(
                    BigDecimal.valueOf(totalNumerator).divide(BigDecimal.valueOf(totalDenominator), 4, java.math.RoundingMode.HALF_UP)
            );
        }
    }

    private MarketPulseHeatmapDTO emptyHeatmap(String kind) {
        return MarketPulseHeatmapDTO.builder()
                .ok(true)
                .kind(kind)
                .source("java-pg")
                .count(0)
                .items(List.of())
                .build();
    }

    private void normalizeHeatmapItem(MarketPulseHeatmapDTO.ItemDTO item) {
        if (item.getChangePercent() == null) {
            item.setChangePercent(item.getChangePct());
        }
        if (item.getChangePct() == null) {
            item.setChangePct(item.getChangePercent());
        }
        if (item.getStockCount() == null) {
            item.setStockCount(item.getCount());
        }
        if (item.getCount() == null) {
            item.setCount(item.getStockCount());
        }
        if (item.getConceptTags() == null) {
            item.setConceptTags(List.of());
        }
        if (item.getChildren() == null) {
            item.setChildren(List.of());
        }
        item.getChildren().forEach(child -> {
            if (child.getConceptTags() == null) {
                child.setConceptTags(List.of());
            }
            if (child.getIsLimitUp() == null) {
                child.setIsLimitUp(false);
            }
        });
    }

    private MarketPulseOverviewDTO overviewError(String message) {
        return MarketPulseOverviewDTO.builder()
                .ok(false)
                .error(message)
                .build();
    }

    private <T> T readJson(String value, Class<T> type) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(value, type);
        } catch (Exception ex) {
            LOGGER.warn("Failed to parse persisted market pulse JSON as {}: {}", type.getSimpleName(), ex.getMessage());
            return null;
        }
    }

    private String normalizeDate(String tradingDate) {
        String normalized = tradingDate == null ? "" : tradingDate;
        if (normalized.length() == 8 && !normalized.contains("-")) {
            return normalized.substring(0, 4) + "-" + normalized.substring(4, 6) + "-" + normalized.substring(6, 8);
        }
        return normalized;
    }

    private String timeString(OffsetDateTime value) {
        return value == null ? null : value.toString();
    }

    private String indexName(String code) {
        return switch (code) {
            case "000001" -> "上证指数";
            case "399001" -> "深证成指";
            case "399006" -> "创业板指";
            default -> code;
        };
    }
}
