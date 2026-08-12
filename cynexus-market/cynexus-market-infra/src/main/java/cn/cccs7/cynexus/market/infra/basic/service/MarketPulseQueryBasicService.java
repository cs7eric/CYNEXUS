package cn.cccs7.cynexus.market.infra.basic.service;

import cn.cccs7.cynexus.market.infra.basic.entity.HeatmapSnapshotPO;
import cn.cccs7.cynexus.market.infra.basic.entity.IndexReturnDailyPO;
import cn.cccs7.cynexus.market.infra.basic.entity.MarketLimitDailySnapshotPO;
import cn.cccs7.cynexus.market.infra.basic.entity.MarketLimitDailyStockPO;
import cn.cccs7.cynexus.market.infra.basic.entity.MarketPulseCaptureBatchPO;
import cn.cccs7.cynexus.market.infra.basic.entity.MarketOverviewPO;
import cn.cccs7.cynexus.market.infra.basic.entity.MarketPulseSectorDailySnapshotPO;
import cn.cccs7.cynexus.market.infra.basic.entity.StyleSectorSnapshotPO;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MarketPulseQueryBasicService {

    Optional<MarketOverviewPO> findLatestOverview();

    Optional<MarketOverviewPO> findOverviewByDate(LocalDate tradeDate);

    Optional<MarketOverviewPO> findPreviousOverview(LocalDate tradeDate);

    List<MarketOverviewPO> listOverviewSince(LocalDate startDate, int limit);

    Optional<MarketOverviewPO> findManualFundFlow(LocalDate tradeDate);

    void upsertManualFundFlow(LocalDate tradeDate, MarketOverviewPO value);

    Optional<LocalDate> findLatestPulseTradeDate(LocalDate endDate);

    Optional<MarketPulseCaptureBatchPO> findLatestPulseBatch(LocalDate tradeDate);

    List<LocalDate> listLatestPulseTradeDates(int limit);

    List<MarketPulseSectorDailySnapshotPO> listPulseRowsByTradeDate(LocalDate tradeDate);

    List<MarketPulseSectorDailySnapshotPO> listPulseRowsByTradeDates(List<LocalDate> tradeDates);

    List<StyleSectorSnapshotPO> listLatestStyleSectors();

    Optional<StyleSectorSnapshotPO> findLatestStyleSector(String name);

    Optional<HeatmapSnapshotPO> findLatestHeatmap(String kind);

    Optional<MarketLimitDailySnapshotPO> findLatestLimitEmotion();

    Optional<MarketLimitDailySnapshotPO> findLimitEmotionByDate(LocalDate tradeDate);

    Optional<MarketLimitDailySnapshotPO> findLimitEmotionBefore(LocalDate tradeDate);

    List<MarketLimitDailyStockPO> listLimitStocks(LocalDate tradeDate, String category, int limit);

    List<IndexReturnDailyPO> listLatestIndexReturns(int windowDays);

    List<IndexReturnDailyPO> listIndexReturnsHistory(int windowDays, LocalDate startDate, LocalDate endDate);

    Optional<IndexReturnDailyPO> findLatestIndexReturnByCode(String indexCode);
}
