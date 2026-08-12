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
import cn.cccs7.cynexus.market.domain.pulse.MarketPulseDomainService;
import cn.cccs7.cynexus.market.domain.pulse.MarketPulsePayloadDomainPolicy;
import cn.cccs7.cynexus.market.domain.service.pulse.MarketPulseQueryDomainService;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MarketPulseQueryAppService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MarketPulseQueryAppService.class);

    private final MarketPulseDomainService domainService;
    private final MarketPulseQueryDomainService queryDomainService;
    private final MarketPulsePayloadDomainPolicy payloadPolicy = new MarketPulsePayloadDomainPolicy();
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Autowired
    public MarketPulseQueryAppService(MarketPulseDomainService domainService,
                                      MarketPulseQueryDomainService queryDomainService) {
        this.domainService = domainService;
        this.queryDomainService = queryDomainService;
    }

    public MarketPulseOverviewDTO getOverviewAkshare() {
        MarketOverviewBO row = queryDomainService.latestOverview().orElse(null);
        if (row == null) {
            return overviewError("market overview is empty");
        }
        domainService.applyServingFacts(row);
        return overviewPayload(row, queryDomainService.previousOverview(row.getTradeDate()).orElse(null));
    }

    public MarketPulseOverviewEltdxDTO getOverviewEltdx() {
        MarketOverviewBO row = queryDomainService.latestOverview().orElse(null);
        if (row == null) {
            return MarketPulseOverviewEltdxDTO.builder().ok(false).error("market overview is empty").build();
        }
        domainService.applyServingFacts(row);
        MarketOverviewBO previous = queryDomainService.previousOverview(row.getTradeDate()).orElse(null);
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
        MarketOverviewBO row = queryDomainService.overviewByDate(date).orElse(null);
        if (row == null) {
            return overviewError("market overview archive not found");
        }
        domainService.applyServingFacts(row);
        return overviewPayload(row, queryDomainService.previousOverview(date).orElse(null));
    }

    public MarketPulseOverviewHistoryDTO getOverviewHistory(String range) {
        String normalizedRange = range == null ? "60d" : range;
        int days = domainService.historyDays(normalizedRange);
        LocalDate start = LocalDate.now().minusDays(days * 2L);
        List<MarketOverviewBO> rows = queryDomainService.overviewSince(start, days);
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
        MarketPulseQueryDomainService.SectorSnapshotResult result = queryDomainService.latestSectorSnapshot(LocalDate.now());
        if (result.tradeDate() == null) {
            return MarketPulseSectorSnapshotDTO.builder()
                    .ok(false)
                    .requestedTradeDate(result.requestedTradeDate())
                    .fallbackTradeDate(false)
                    .servingMode("close")
                    .rowCount(0)
                    .rows(List.of())
                    .error(result.error())
                    .build();
        }
        return MarketPulseSectorSnapshotDTO.builder()
                .ok(true)
                .tradeDate(result.tradeDate())
                .requestedTradeDate(result.requestedTradeDate())
                .fallbackTradeDate(result.fallbackTradeDate())
                .servingMode(result.servingMode())
                .sourceKind(result.sourceKind())
                .sourceName(result.sourceName())
                .status(result.status())
                .fetchedAt(result.fetchedAt())
                .rowCount(result.rows().size())
                .rows(MarketPulseSectorDailySnapshotDTOConverter.INSTANCE.toSourceList(result.rows()))
                .build();
    }

    public MarketPulseSectorSnapshotHistoryDTO getSectorSnapshotHistory(int days) {
        int safeDays = domainService.safeHistoryDays(days);
        List<LocalDate> dates = queryDomainService.latestPulseTradeDates(safeDays);
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
        List<MarketPulseSectorDailySnapshotBO> rows = queryDomainService.pulseRowsByTradeDates(dates);
        MarketPulseCaptureBatchBO latestBatch = queryDomainService.latestPulseBatch(dates.get(dates.size() - 1)).orElse(null);
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
        return queryDomainService.manualFundFlow(tradeDate).map(row -> manualPayload(row, true));
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
        MarketPulseQueryDomainService.ManualFundFlowSaveResult result = queryDomainService
                .saveManualFundFlow(request.getTradingDate(), value);
        return Optional.ofNullable(result.value())
                .map(row -> manualPayload(row, true))
                .orElseGet(() -> MarketPulseManualFundFlowDTO.builder()
                        .ok(false)
                        .error("manual fund flow not found after save")
                        .tradingDate(request.getTradingDate())
                        .build());
    }

    public MarketPulseIndexReturnsDTO getIndexReturns(int days) {
        List<MarketPulseIndexReturnsDTO.ItemDTO> items = MarketPulseIndexReturnItemDTOConverter.INSTANCE
                .toLatestItems(queryDomainService.latestIndexReturns(days));
        return MarketPulseIndexReturnsDTO.builder()
                .ok(true)
                .days(days)
                .items(items)
                .build();
    }

    public MarketPulseIndexReturnsHistoryDTO getIndexReturnsHistory(int window, LocalDate startDate, LocalDate endDate) {
        List<MarketPulseIndexReturnsHistoryDTO.ItemDTO> items = MarketPulseIndexReturnItemDTOConverter.INSTANCE
                .toHistoryItems(queryDomainService.indexReturnsHistory(window, startDate, endDate));
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
            MarketPulseQueryDomainService.IndexMinuteQuery index = queryDomainService.indexMinuteQuery(code, date);
            LocalDate tradeDate = index.tradeDate();
            List<IndexMinuteBarBO> points = index.points();
            BigDecimal previousClose = index.previousClose();
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
        List<StyleSectorSnapshotBO> rows = queryDomainService.latestStyleSectors();
        List<MarketPulseStyleSectorListDTO.ItemDTO> items = MarketPulseStyleSectorItemDTOConverter.INSTANCE.toSourceList(rows);
        return MarketPulseStyleSectorListDTO.builder()
                .ok(true)
                .items(items)
                .count(items.size())
                .names(rows.stream().map(StyleSectorSnapshotBO::getName).toList())
                .build();
    }

    public MarketPulseStyleSectorConstituentsDTO getStyleSectorConstituents(String name) {
        StyleSectorSnapshotBO row = queryDomainService.latestStyleSector(name).orElse(null);
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
        List<ThsIndustryConstituentSnapshotBO> rows = queryDomainService.latestIndustryConstituents(industryCode);
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
        MarketPulseQueryDomainService.HeatmapQuery query = queryDomainService.queryHeatmap(kind);
        if (query.isStyles()) {
            return styleHeatmap(query.styleRows());
        }
        return Optional.ofNullable(query.snapshot())
                .map(row -> toDto(payloadPolicy.normalizeHeatmap(row.getPayload(), query.kind(), row.getSource()),
                        MarketPulseHeatmapDTO.class))
                .orElseGet(() -> emptyHeatmap(query.kind()));
    }

    private MarketPulseHeatmapDTO styleHeatmap(List<StyleSectorSnapshotBO> rows) {
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
        return getLimitEmotion(null);
    }

    public MarketPulseLimitEmotionDTO getLimitEmotion(LocalDate requestedTradeDate) {
        MarketPulseQueryDomainService.LimitEmotionQuery limit = requestedTradeDate == null
                ? queryDomainService.latestLimitEmotion()
                : queryDomainService.limitEmotion(requestedTradeDate);
        MarketLimitDailySnapshotBO row = limit.snapshot();
        if (row == null) {
            return MarketPulseLimitEmotionDTO.builder().ok(false).error("limit emotion is empty").build();
        }
        return toDto(payloadPolicy.normalizeLimitEmotion(
                        row.getExtra(),
                        row,
                        limit.limitUp(), limit.limitDown(), limit.broken()),
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
