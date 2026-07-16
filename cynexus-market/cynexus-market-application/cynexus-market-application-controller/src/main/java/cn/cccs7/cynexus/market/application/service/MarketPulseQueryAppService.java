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
import cn.cccs7.cynexus.market.api.dto.MarketPulseSectorSnapshotDTO;
import cn.cccs7.cynexus.market.api.dto.MarketPulseSectorSnapshotHistoryDTO;
import cn.cccs7.cynexus.market.api.dto.MarketPulseStyleSectorConstituentsDTO;
import cn.cccs7.cynexus.market.api.dto.MarketPulseStyleSectorListDTO;
import cn.cccs7.cynexus.market.api.request.MarketPulseManualFundFlowRequest;
import cn.cccs7.cynexus.market.application.convert.MarketPulseIndexKlinePointDTOConverter;
import cn.cccs7.cynexus.market.application.convert.MarketPulseIndexReturnItemDTOConverter;
import cn.cccs7.cynexus.market.application.convert.MarketPulseLimitEmotionStockDTOConverter;
import cn.cccs7.cynexus.market.application.convert.MarketPulseOverviewResponseDTOConverter;
import cn.cccs7.cynexus.market.application.convert.MarketPulseSectorDailySnapshotDTOConverter;
import cn.cccs7.cynexus.market.application.convert.MarketPulseStyleSectorItemDTOConverter;
import cn.cccs7.cynexus.market.domain.entity.IndexMinuteBarBO;
import cn.cccs7.cynexus.market.domain.entity.MarketLimitDailySnapshotBO;
import cn.cccs7.cynexus.market.domain.entity.MarketOverviewBO;
import cn.cccs7.cynexus.market.domain.entity.MarketPulseCaptureBatchBO;
import cn.cccs7.cynexus.market.domain.entity.MarketPulseSectorDailySnapshotBO;
import cn.cccs7.cynexus.market.domain.entity.StyleSectorSnapshotBO;
import cn.cccs7.cynexus.market.domain.entity.ThsIndustryConstituentSnapshotBO;
import cn.cccs7.cynexus.market.domain.repository.MarketPulseQueryRepository;
import cn.cccs7.cynexus.market.domain.repository.ThsIndustryConstituentSnapshotRepository;
import cn.cccs7.cynexus.market.domain.pulse.MarketPulseDomainService;
import cn.cccs7.cynexus.market.domain.pulse.MarketPulsePayloadDomainPolicy;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MarketPulseQueryAppService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MarketPulseQueryAppService.class);

    private final MarketPulseQueryRepository marketPulseQueryRepository;
    private final ThsIndustryConstituentSnapshotRepository thsIndustryConstituentSnapshotRepository;
    private final MarketPulseDomainService domainService;
    private final MarketPulsePayloadDomainPolicy payloadPolicy = new MarketPulsePayloadDomainPolicy();
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public MarketPulseQueryAppService(MarketPulseQueryRepository marketPulseQueryRepository,
                                      ThsIndustryConstituentSnapshotRepository thsIndustryConstituentSnapshotRepository,
                                      MarketPulseDomainService domainService) {
        this.marketPulseQueryRepository = marketPulseQueryRepository;
        this.thsIndustryConstituentSnapshotRepository = thsIndustryConstituentSnapshotRepository;
        this.domainService = domainService;
    }

    public MarketPulseOverviewDTO getOverviewAkshare() {
        MarketOverviewBO row = marketPulseQueryRepository.findLatestOverview().orElse(null);
        if (row == null) {
            return overviewError("market overview is empty");
        }
        domainService.applyServingFacts(row);
        return overviewPayload(row, marketPulseQueryRepository.findPreviousOverview(row.getTradeDate()).orElse(null));
    }

    public MarketPulseOverviewEltdxDTO getOverviewEltdx() {
        MarketOverviewBO row = marketPulseQueryRepository.findLatestOverview().orElse(null);
        if (row == null) {
            return MarketPulseOverviewEltdxDTO.builder().ok(false).error("market overview is empty").build();
        }
        domainService.applyServingFacts(row);
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
        LocalDate date = LocalDate.parse(domainService.normalizeDate(tradingDate));
        MarketOverviewBO row = marketPulseQueryRepository.findOverviewByDate(date).orElse(null);
        if (row == null) {
            return overviewError("market overview archive not found");
        }
        domainService.applyServingFacts(row);
        return overviewPayload(row, marketPulseQueryRepository.findPreviousOverview(date).orElse(null));
    }

    public MarketPulseOverviewHistoryDTO getOverviewHistory(String range) {
        String normalizedRange = range == null ? "60d" : range;
        int days = domainService.historyDays(normalizedRange);
        LocalDate start = LocalDate.now().minusDays(days * 2L);
        List<MarketOverviewBO> rows = marketPulseQueryRepository.listOverviewSince(start, days);
        domainService.applyServingFacts(rows);
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

    public MarketPulseSectorSnapshotDTO getLatestSectorSnapshot() {
        LocalDate requestedTradeDate = LocalDate.now();
        LocalDate tradeDate = marketPulseQueryRepository.findLatestPulseTradeDate(requestedTradeDate)
                .or(() -> marketPulseQueryRepository.findLatestPulseTradeDate(null))
                .orElse(null);
        if (tradeDate == null) {
            return MarketPulseSectorSnapshotDTO.builder()
                    .ok(false)
                    .requestedTradeDate(requestedTradeDate)
                    .fallbackTradeDate(false)
                    .servingMode("close")
                    .rowCount(0)
                    .rows(List.of())
                    .error("market pulse snapshot is empty")
                    .build();
        }
        MarketPulseCaptureBatchBO batch = marketPulseQueryRepository.findLatestPulseBatch(tradeDate).orElse(null);
        List<MarketPulseSectorDailySnapshotBO> rows = marketPulseQueryRepository.listPulseRowsByTradeDate(tradeDate);
        return MarketPulseSectorSnapshotDTO.builder()
                .ok(true)
                .tradeDate(tradeDate)
                .requestedTradeDate(requestedTradeDate)
                .fallbackTradeDate(!tradeDate.equals(requestedTradeDate))
                .servingMode(domainService.resolveServingMode(tradeDate, batch == null ? null : batch.getSourceKind()))
                .sourceKind(batch == null ? null : batch.getSourceKind())
                .sourceName(batch == null ? null : batch.getSourceName())
                .status(batch == null ? null : batch.getStatus())
                .fetchedAt(batch == null ? null : batch.getFetchedAt())
                .rowCount(rows.size())
                .rows(MarketPulseSectorDailySnapshotDTOConverter.INSTANCE.toSourceList(rows))
                .build();
    }

    public MarketPulseSectorSnapshotHistoryDTO getSectorSnapshotHistory(int days) {
        int safeDays = domainService.safeHistoryDays(days);
        List<LocalDate> dates = marketPulseQueryRepository.listLatestPulseTradeDates(safeDays);
        if (dates.isEmpty()) {
            return MarketPulseSectorSnapshotHistoryDTO.builder()
                    .ok(false)
                    .days(safeDays)
                    .dates(List.of())
                    .rowCount(0)
                    .rows(List.of())
                    .error("market pulse history is empty")
                    .build();
        }
        List<MarketPulseSectorDailySnapshotBO> rows = marketPulseQueryRepository.listPulseRowsByTradeDates(dates);
        MarketPulseCaptureBatchBO latestBatch = marketPulseQueryRepository.findLatestPulseBatch(dates.get(dates.size() - 1)).orElse(null);
        return MarketPulseSectorSnapshotHistoryDTO.builder()
                .ok(true)
                .days(safeDays)
                .dates(dates)
                .rowCount(rows.size())
                .sourceKind(latestBatch == null ? null : latestBatch.getSourceKind())
                .sourceName(latestBatch == null ? null : latestBatch.getSourceName())
                .fetchedAt(latestBatch == null ? null : latestBatch.getFetchedAt())
                .rows(MarketPulseSectorDailySnapshotDTOConverter.INSTANCE.toSourceList(rows))
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
        List<String> targetCodes = codes == null || codes.isEmpty() ? domainService.defaultIndexCodes() : codes;
        List<MarketPulseIndexKlineBatchDTO.ItemDTO> items = new ArrayList<>();
        List<String> returnedDates = new ArrayList<>();
        String normalizedInterval = domainService.normalizeInterval(interval);
        for (String code : targetCodes) {
            LocalDate tradeDate = marketPulseQueryRepository.findLatestIndexMinuteDate(code, date).orElse(date);
            List<IndexMinuteBarBO> points = marketPulseQueryRepository.listIndexMinuteBars(code, tradeDate);
            BigDecimal previousClose = marketPulseQueryRepository.findIndexPreviousClose(code, tradeDate).orElse(null);
            items.add(MarketPulseIndexKlineBatchDTO.ItemDTO.builder()
                    .ok(!points.isEmpty())
                    .code(code)
                    .name(domainService.indexName(code))
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
            MarketPulseStyleSectorConstituentsDTO parsed = toDto(payloadPolicy.parsePayload(row.getPayload()),
                    MarketPulseStyleSectorConstituentsDTO.class);
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
        if ("styles".equals(kind)) {
            return styleHeatmap();
        }
        return marketPulseQueryRepository.findLatestHeatmap(kind)
                .map(row -> toDto(payloadPolicy.normalizeHeatmap(row.getPayload(), kind, row.getSource()),
                        MarketPulseHeatmapDTO.class))
                .orElseGet(() -> emptyHeatmap(kind));
    }

    private MarketPulseHeatmapDTO styleHeatmap() {
        List<StyleSectorSnapshotBO> rows = marketPulseQueryRepository.listLatestStyleSectors();
        List<MarketPulseHeatmapDTO.ItemDTO> items = rows.stream()
                .map(row -> {
                    BigDecimal sizeValue = BigDecimal.valueOf(row.getValidSize() == null ? 0 : row.getValidSize());
                    return MarketPulseHeatmapDTO.ItemDTO.builder()
                            .name(row.getName())
                            .value(sizeValue)
                            .changePct(row.getChangePct())
                            .changePercent(row.getChangePct())
                            .kind("styles")
                            .sectorCode(row.getName())
                            .kindLabel("风格")
                            .validSize(row.getValidSize())
                            .stockCount(row.getSampleSize())
                            .count(row.getSampleSize())
                            .amount(sizeValue)
                            .mainNetInflow(BigDecimal.ZERO)
                            .circulatingMarketCap(BigDecimal.ZERO)
                            .limitStreakCount(0)
                            .conceptTags(List.of())
                            .children(List.of())
                            .build();
                })
                .toList();
        return MarketPulseHeatmapDTO.builder()
                .ok(true)
                .kind("styles")
                .source("mkt_style_sector_snapshot")
                .count(items.size())
                .items(items)
                .build();
    }

    public MarketPulseLimitEmotionDTO getLimitEmotion() {
        MarketLimitDailySnapshotBO row = marketPulseQueryRepository.findLatestLimitEmotion().orElse(null);
        if (row == null) {
            return MarketPulseLimitEmotionDTO.builder().ok(false).error("limit emotion is empty").build();
        }
        return toDto(payloadPolicy.normalizeLimitEmotion(
                        row.getExtra(),
                        row,
                        marketPulseQueryRepository.listLimitStocks(row.getTradeDate(), "limit_up", 80),
                        marketPulseQueryRepository.listLimitStocks(row.getTradeDate(), "limit_down", 80),
                        marketPulseQueryRepository.listLimitStocks(row.getTradeDate(), "broken", 80)),
                MarketPulseLimitEmotionDTO.class);
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

    private MarketPulseHeatmapDTO emptyHeatmap(String kind) {
        return MarketPulseHeatmapDTO.builder()
                .ok(true)
                .kind(kind)
                .source("java-pg")
                .count(0)
                .items(List.of())
                .build();
    }

    private MarketPulseOverviewDTO overviewError(String message) {
        return MarketPulseOverviewDTO.builder()
                .ok(false)
                .error(message)
                .build();
    }

    private <T> T toDto(com.fasterxml.jackson.databind.JsonNode value, Class<T> type) {
        if (value == null || value.isNull()) {
            return null;
        }
        try {
            return objectMapper.treeToValue(value, type);
        } catch (Exception ex) {
            LOGGER.warn("Failed to map persisted market pulse payload as {}: {}", type.getSimpleName(), ex.getMessage());
            return null;
        }
    }

    private String timeString(OffsetDateTime value) {
        return value == null ? null : value.toString();
    }

}
