package cn.cccs7.cynexus.market.infra.basic.service.impl;

import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import cn.cccs7.cynexus.market.infra.basic.entity.HeatmapSnapshotPO;
import cn.cccs7.cynexus.market.infra.basic.entity.IndexReturnDailyPO;
import cn.cccs7.cynexus.market.infra.basic.entity.MarketLimitDailySnapshotPO;
import cn.cccs7.cynexus.market.infra.basic.entity.MarketLimitDailyStockPO;
import cn.cccs7.cynexus.market.infra.basic.entity.MarketPulseCaptureBatchPO;
import cn.cccs7.cynexus.market.infra.basic.entity.MarketOverviewPO;
import cn.cccs7.cynexus.market.infra.basic.entity.MarketPulseSectorDailySnapshotPO;
import cn.cccs7.cynexus.market.infra.basic.entity.StyleSectorSnapshotPO;
import cn.cccs7.cynexus.market.infra.basic.mapper.MarketPulseQueryMapper;
import cn.cccs7.cynexus.market.infra.basic.service.MarketPulseQueryBasicService;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class MarketPulseQueryBasicServiceImpl implements MarketPulseQueryBasicService {

    private final MarketPulseQueryMapper marketPulseQueryMapper;
    private final SnowflakeIdGenerator snowflakeIdGenerator;

    public MarketPulseQueryBasicServiceImpl(MarketPulseQueryMapper marketPulseQueryMapper,
                                            SnowflakeIdGenerator snowflakeIdGenerator) {
        this.marketPulseQueryMapper = marketPulseQueryMapper;
        this.snowflakeIdGenerator = snowflakeIdGenerator;
    }

    @Override
    public Optional<MarketOverviewPO> findLatestOverview() {
        return Optional.ofNullable(marketPulseQueryMapper.findLatestOverview());
    }

    @Override
    public Optional<MarketOverviewPO> findOverviewByDate(LocalDate tradeDate) {
        return Optional.ofNullable(marketPulseQueryMapper.findOverviewByDate(tradeDate));
    }

    @Override
    public Optional<MarketOverviewPO> findPreviousOverview(LocalDate tradeDate) {
        return Optional.ofNullable(marketPulseQueryMapper.findPreviousOverview(tradeDate));
    }

    @Override
    public List<MarketOverviewPO> listOverviewSince(LocalDate startDate, int limit) {
        return marketPulseQueryMapper.listOverviewSince(startDate, limit);
    }

    @Override
    public Optional<MarketOverviewPO> findManualFundFlow(LocalDate tradeDate) {
        return Optional.ofNullable(marketPulseQueryMapper.findManualFundFlow(tradeDate));
    }

    @Override
    public void upsertManualFundFlow(LocalDate tradeDate, MarketOverviewPO value) {
        OffsetDateTime now = OffsetDateTime.now();
        int updated = marketPulseQueryMapper.updateManualFundFlow(
                tradeDate,
                value.getMainNetInflow(),
                value.getMainNetInflowRatio(),
                value.getSuperLargeNetInflow(),
                value.getSuperLargeNetRatio(),
                value.getLargeNetInflow(),
                value.getLargeNetRatio(),
                value.getMediumNetInflow(),
                value.getMediumNetRatio(),
                value.getSmallNetInflow(),
                value.getSmallNetRatio(),
                now
        );
        if (updated > 0) {
            return;
        }
        value.setId(snowflakeIdGenerator.nextId());
        value.setTradeDate(tradeDate);
        value.setSource("manual");
        value.setManualOverride(true);
        value.setManualUpdatedAt(now);
        value.setCreatedAt(now);
        value.setUpdatedAt(now);
        marketPulseQueryMapper.insertManualFundFlow(value);
    }

    @Override
    public Optional<LocalDate> findLatestPulseTradeDate(LocalDate endDate) {
        return Optional.ofNullable(marketPulseQueryMapper.findLatestPulseTradeDate(endDate));
    }

    @Override
    public Optional<MarketPulseCaptureBatchPO> findLatestPulseBatch(LocalDate tradeDate) {
        return Optional.ofNullable(marketPulseQueryMapper.findLatestPulseBatch(tradeDate));
    }

    @Override
    public List<LocalDate> listLatestPulseTradeDates(int limit) {
        return marketPulseQueryMapper.listLatestPulseTradeDates(limit);
    }

    @Override
    public List<MarketPulseSectorDailySnapshotPO> listPulseRowsByTradeDate(LocalDate tradeDate) {
        return marketPulseQueryMapper.listPulseRowsByTradeDate(tradeDate);
    }

    @Override
    public List<MarketPulseSectorDailySnapshotPO> listPulseRowsByTradeDates(List<LocalDate> tradeDates) {
        if (tradeDates == null || tradeDates.isEmpty()) {
            return List.of();
        }
        return marketPulseQueryMapper.listPulseRowsByTradeDates(tradeDates);
    }

    @Override
    public List<StyleSectorSnapshotPO> listLatestStyleSectors() {
        return marketPulseQueryMapper.listLatestStyleSectors();
    }

    @Override
    public Optional<StyleSectorSnapshotPO> findLatestStyleSector(String name) {
        return Optional.ofNullable(marketPulseQueryMapper.findLatestStyleSector(name));
    }

    @Override
    public Optional<HeatmapSnapshotPO> findLatestHeatmap(String kind) {
        return Optional.ofNullable(marketPulseQueryMapper.findLatestHeatmap(kind));
    }

    @Override
    public Optional<MarketLimitDailySnapshotPO> findLatestLimitEmotion() {
        return Optional.ofNullable(marketPulseQueryMapper.findLatestLimitEmotion());
    }

    @Override
    public Optional<MarketLimitDailySnapshotPO> findLimitEmotionByDate(LocalDate tradeDate) {
        return Optional.ofNullable(marketPulseQueryMapper.findLimitEmotionByDate(tradeDate));
    }

    @Override
    public List<MarketLimitDailyStockPO> listLimitStocks(LocalDate tradeDate, String category, int limit) {
        return marketPulseQueryMapper.listLimitStocks(tradeDate, category, limit);
    }

    @Override
    public List<IndexReturnDailyPO> listLatestIndexReturns(int windowDays) {
        return marketPulseQueryMapper.listLatestIndexReturns(windowDays);
    }

    @Override
    public List<IndexReturnDailyPO> listIndexReturnsHistory(int windowDays, LocalDate startDate, LocalDate endDate) {
        return marketPulseQueryMapper.listIndexReturnsHistory(windowDays, startDate, endDate);
    }

    @Override
    public Optional<IndexReturnDailyPO> findLatestIndexReturnByCode(String indexCode) {
        return Optional.ofNullable(marketPulseQueryMapper.findLatestIndexReturnByCode(indexCode));
    }
}
