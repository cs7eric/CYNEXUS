package cn.cccs7.cynexus.market.infra.repository;

import cn.cccs7.cynexus.market.domain.entity.HeatmapSnapshotBO;
import cn.cccs7.cynexus.market.domain.entity.IndexMinuteBarBO;
import cn.cccs7.cynexus.market.domain.entity.IndexReturnDailyBO;
import cn.cccs7.cynexus.market.domain.entity.MarketLimitDailySnapshotBO;
import cn.cccs7.cynexus.market.domain.entity.MarketLimitDailyStockBO;
import cn.cccs7.cynexus.market.domain.entity.MarketPulseCaptureBatchBO;
import cn.cccs7.cynexus.market.domain.entity.MarketOverviewBO;
import cn.cccs7.cynexus.market.domain.entity.MarketPulseSectorDailySnapshotBO;
import cn.cccs7.cynexus.market.domain.entity.StyleSectorSnapshotBO;
import cn.cccs7.cynexus.market.domain.repository.MarketPulseQueryRepository;
import cn.cccs7.cynexus.market.infra.basic.service.MarketPulseQueryBasicService;
import cn.cccs7.cynexus.market.infra.client.ClickHouseMarketPulseClient;
import cn.cccs7.cynexus.market.infra.convert.HeatmapSnapshotPOConverter;
import cn.cccs7.cynexus.market.infra.convert.IndexReturnDailyPOConverter;
import cn.cccs7.cynexus.market.infra.convert.MarketLimitDailySnapshotPOConverter;
import cn.cccs7.cynexus.market.infra.convert.MarketLimitDailyStockPOConverter;
import cn.cccs7.cynexus.market.infra.convert.MarketPulseCaptureBatchPOConverter;
import cn.cccs7.cynexus.market.infra.convert.MarketOverviewPOConverter;
import cn.cccs7.cynexus.market.infra.convert.MarketPulseSectorDailySnapshotPOConverter;
import cn.cccs7.cynexus.market.infra.convert.StyleSectorSnapshotPOConverter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class MarketPulseQueryRepositoryImpl implements MarketPulseQueryRepository {

    private final MarketPulseQueryBasicService marketPulseQueryBasicService;
    private final ClickHouseMarketPulseClient clickHouseMarketPulseClient;

    public MarketPulseQueryRepositoryImpl(MarketPulseQueryBasicService marketPulseQueryBasicService,
                                          ClickHouseMarketPulseClient clickHouseMarketPulseClient) {
        this.marketPulseQueryBasicService = marketPulseQueryBasicService;
        this.clickHouseMarketPulseClient = clickHouseMarketPulseClient;
    }

    @Override
    public Optional<MarketOverviewBO> findLatestOverview() {
        return marketPulseQueryBasicService.findLatestOverview().map(MarketOverviewPOConverter.INSTANCE::toTarget);
    }

    @Override
    public Optional<MarketOverviewBO> findOverviewByDate(LocalDate tradeDate) {
        return marketPulseQueryBasicService.findOverviewByDate(tradeDate).map(MarketOverviewPOConverter.INSTANCE::toTarget);
    }

    @Override
    public Optional<MarketOverviewBO> findPreviousOverview(LocalDate tradeDate) {
        return marketPulseQueryBasicService.findPreviousOverview(tradeDate).map(MarketOverviewPOConverter.INSTANCE::toTarget);
    }

    @Override
    public List<MarketOverviewBO> listOverviewSince(LocalDate startDate, int limit) {
        return MarketOverviewPOConverter.INSTANCE.toTargetList(marketPulseQueryBasicService.listOverviewSince(startDate, limit));
    }

    @Override
    public Optional<MarketOverviewBO> findManualFundFlow(LocalDate tradeDate) {
        return marketPulseQueryBasicService.findManualFundFlow(tradeDate).map(MarketOverviewPOConverter.INSTANCE::toTarget);
    }

    @Override
    public void upsertManualFundFlow(LocalDate tradeDate, MarketOverviewBO value) {
        marketPulseQueryBasicService.upsertManualFundFlow(tradeDate, MarketOverviewPOConverter.INSTANCE.toSource(value));
    }

    @Override
    public Optional<LocalDate> findLatestPulseTradeDate(LocalDate endDate) {
        return marketPulseQueryBasicService.findLatestPulseTradeDate(endDate);
    }

    @Override
    public Optional<MarketPulseCaptureBatchBO> findLatestPulseBatch(LocalDate tradeDate) {
        return marketPulseQueryBasicService.findLatestPulseBatch(tradeDate)
                .map(MarketPulseCaptureBatchPOConverter.INSTANCE::toTarget);
    }

    @Override
    public List<LocalDate> listLatestPulseTradeDates(int limit) {
        return marketPulseQueryBasicService.listLatestPulseTradeDates(limit);
    }

    @Override
    public List<MarketPulseSectorDailySnapshotBO> listPulseRowsByTradeDate(LocalDate tradeDate) {
        return MarketPulseSectorDailySnapshotPOConverter.INSTANCE.toTargetList(
                marketPulseQueryBasicService.listPulseRowsByTradeDate(tradeDate)
        );
    }

    @Override
    public List<MarketPulseSectorDailySnapshotBO> listPulseRowsByTradeDates(List<LocalDate> tradeDates) {
        return MarketPulseSectorDailySnapshotPOConverter.INSTANCE.toTargetList(
                marketPulseQueryBasicService.listPulseRowsByTradeDates(tradeDates)
        );
    }

    @Override
    public List<StyleSectorSnapshotBO> listLatestStyleSectors() {
        return StyleSectorSnapshotPOConverter.INSTANCE.toTargetList(marketPulseQueryBasicService.listLatestStyleSectors());
    }

    @Override
    public Optional<StyleSectorSnapshotBO> findLatestStyleSector(String name) {
        return marketPulseQueryBasicService.findLatestStyleSector(name).map(StyleSectorSnapshotPOConverter.INSTANCE::toTarget);
    }

    @Override
    public Optional<HeatmapSnapshotBO> findLatestHeatmap(String kind) {
        return marketPulseQueryBasicService.findLatestHeatmap(kind).map(HeatmapSnapshotPOConverter.INSTANCE::toTarget);
    }

    @Override
    public Optional<MarketLimitDailySnapshotBO> findLatestLimitEmotion() {
        return marketPulseQueryBasicService.findLatestLimitEmotion().map(MarketLimitDailySnapshotPOConverter.INSTANCE::toTarget);
    }

    @Override
    public Optional<MarketLimitDailySnapshotBO> findLimitEmotionByDate(LocalDate tradeDate) {
        return marketPulseQueryBasicService.findLimitEmotionByDate(tradeDate)
                .map(MarketLimitDailySnapshotPOConverter.INSTANCE::toTarget);
    }

    @Override
    public Optional<MarketLimitDailySnapshotBO> findLimitEmotionBefore(LocalDate tradeDate) {
        return marketPulseQueryBasicService.findLimitEmotionBefore(tradeDate)
                .map(MarketLimitDailySnapshotPOConverter.INSTANCE::toTarget);
    }

    @Override
    public List<MarketLimitDailyStockBO> listLimitStocks(LocalDate tradeDate, String category, int limit) {
        return MarketLimitDailyStockPOConverter.INSTANCE.toTargetList(
                marketPulseQueryBasicService.listLimitStocks(tradeDate, category, limit)
        );
    }

    @Override
    public List<IndexReturnDailyBO> listLatestIndexReturns(int days) {
        return IndexReturnDailyPOConverter.INSTANCE.toTargetList(marketPulseQueryBasicService.listLatestIndexReturns(days));
    }

    @Override
    public List<IndexReturnDailyBO> listIndexReturnsHistory(int window, LocalDate startDate, LocalDate endDate) {
        return IndexReturnDailyPOConverter.INSTANCE.toTargetList(
                marketPulseQueryBasicService.listIndexReturnsHistory(window, startDate, endDate)
        );
    }

    @Override
    public Optional<IndexReturnDailyBO> findLatestIndexReturnByCode(String code) {
        return marketPulseQueryBasicService.findLatestIndexReturnByCode(normalizeIndexCode(code))
                .map(IndexReturnDailyPOConverter.INSTANCE::toTarget);
    }

    @Override
    public Optional<LocalDate> findLatestIndexMinuteDate(String code, LocalDate requestedDate) {
        return clickHouseMarketPulseClient.findLatestIndexMinuteDate(code, requestedDate);
    }

    @Override
    public Optional<MarketOverviewBO> findDailyBreadth(LocalDate tradeDate) {
        return clickHouseMarketPulseClient.findDailyBreadth(tradeDate);
    }

    @Override
    public List<MarketOverviewBO> listDailyBreadth(LocalDate startDate, LocalDate endDate) {
        return clickHouseMarketPulseClient.listDailyBreadth(startDate, endDate);
    }

    @Override
    public List<IndexMinuteBarBO> listIndexMinuteBars(String code, LocalDate tradeDate) {
        return clickHouseMarketPulseClient.listIndexMinuteBars(code, tradeDate);
    }

    @Override
    public Optional<BigDecimal> findIndexPreviousClose(String code, LocalDate tradeDate) {
        return clickHouseMarketPulseClient.findIndexPreviousClose(code, tradeDate);
    }

    private String normalizeIndexCode(String code) {
        if (code == null) {
            return "";
        }
        if (code.startsWith("sh") || code.startsWith("sz")) {
            return code;
        }
        return switch (code) {
            case "399001", "399006" -> "sz" + code;
            default -> "sh" + code;
        };
    }
}
