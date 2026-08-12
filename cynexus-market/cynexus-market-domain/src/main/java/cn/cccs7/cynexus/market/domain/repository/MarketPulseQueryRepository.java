package cn.cccs7.cynexus.market.domain.repository;

import cn.cccs7.cynexus.market.domain.entity.HeatmapSnapshotBO;
import cn.cccs7.cynexus.market.domain.entity.IndexMinuteBarBO;
import cn.cccs7.cynexus.market.domain.entity.IndexReturnDailyBO;
import cn.cccs7.cynexus.market.domain.entity.MarketLimitDailySnapshotBO;
import cn.cccs7.cynexus.market.domain.entity.MarketLimitDailyStockBO;
import cn.cccs7.cynexus.market.domain.entity.MarketPulseCaptureBatchBO;
import cn.cccs7.cynexus.market.domain.entity.MarketOverviewBO;
import cn.cccs7.cynexus.market.domain.entity.MarketPulseSectorDailySnapshotBO;
import cn.cccs7.cynexus.market.domain.entity.StyleSectorSnapshotBO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MarketPulseQueryRepository {

    Optional<MarketOverviewBO> findLatestOverview();

    Optional<MarketOverviewBO> findOverviewByDate(LocalDate tradeDate);

    Optional<MarketOverviewBO> findPreviousOverview(LocalDate tradeDate);

    List<MarketOverviewBO> listOverviewSince(LocalDate startDate, int limit);

    Optional<MarketOverviewBO> findManualFundFlow(LocalDate tradeDate);

    void upsertManualFundFlow(LocalDate tradeDate, MarketOverviewBO value);

    Optional<LocalDate> findLatestPulseTradeDate(LocalDate endDate);

    Optional<MarketPulseCaptureBatchBO> findLatestPulseBatch(LocalDate tradeDate);

    List<LocalDate> listLatestPulseTradeDates(int limit);

    List<MarketPulseSectorDailySnapshotBO> listPulseRowsByTradeDate(LocalDate tradeDate);

    List<MarketPulseSectorDailySnapshotBO> listPulseRowsByTradeDates(List<LocalDate> tradeDates);

    List<StyleSectorSnapshotBO> listLatestStyleSectors();

    Optional<StyleSectorSnapshotBO> findLatestStyleSector(String name);

    Optional<HeatmapSnapshotBO> findLatestHeatmap(String kind);

    Optional<MarketLimitDailySnapshotBO> findLatestLimitEmotion();

    Optional<MarketLimitDailySnapshotBO> findLimitEmotionByDate(LocalDate tradeDate);

    Optional<MarketLimitDailySnapshotBO> findLimitEmotionBefore(LocalDate tradeDate);

    List<MarketLimitDailyStockBO> listLimitStocks(LocalDate tradeDate, String category, int limit);

    List<IndexReturnDailyBO> listLatestIndexReturns(int days);

    List<IndexReturnDailyBO> listIndexReturnsHistory(int window, LocalDate startDate, LocalDate endDate);

    Optional<IndexReturnDailyBO> findLatestIndexReturnByCode(String code);

    Optional<LocalDate> findLatestIndexMinuteDate(String code, LocalDate requestedDate);

    Optional<MarketOverviewBO> findDailyBreadth(LocalDate tradeDate);

    List<MarketOverviewBO> listDailyBreadth(LocalDate startDate, LocalDate endDate);

    List<IndexMinuteBarBO> listIndexMinuteBars(String code, LocalDate tradeDate);

    Optional<BigDecimal> findIndexPreviousClose(String code, LocalDate tradeDate);
}
