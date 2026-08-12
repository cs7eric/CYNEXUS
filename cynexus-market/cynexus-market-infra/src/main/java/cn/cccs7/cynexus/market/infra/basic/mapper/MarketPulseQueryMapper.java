package cn.cccs7.cynexus.market.infra.basic.mapper;

import cn.cccs7.cynexus.market.infra.basic.entity.HeatmapSnapshotPO;
import cn.cccs7.cynexus.market.infra.basic.entity.IndexReturnDailyPO;
import cn.cccs7.cynexus.market.infra.basic.entity.MarketLimitDailySnapshotPO;
import cn.cccs7.cynexus.market.infra.basic.entity.MarketLimitDailyStockPO;
import cn.cccs7.cynexus.market.infra.basic.entity.MarketPulseCaptureBatchPO;
import cn.cccs7.cynexus.market.infra.basic.entity.MarketOverviewPO;
import cn.cccs7.cynexus.market.infra.basic.entity.MarketPulseSectorDailySnapshotPO;
import cn.cccs7.cynexus.market.infra.basic.entity.StyleSectorSnapshotPO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MarketPulseQueryMapper {

    MarketOverviewPO findLatestOverview();

    MarketOverviewPO findOverviewByDate(@Param("tradeDate") LocalDate tradeDate);

    MarketOverviewPO findPreviousOverview(@Param("tradeDate") LocalDate tradeDate);

    List<MarketOverviewPO> listOverviewSince(@Param("startDate") LocalDate startDate, @Param("limit") int limit);

    MarketOverviewPO findManualFundFlow(@Param("tradeDate") LocalDate tradeDate);

    int updateManualFundFlow(
            @Param("tradeDate") LocalDate tradeDate,
            @Param("mainNetInflow") BigDecimal mainNetInflow,
            @Param("mainNetInflowRatio") BigDecimal mainNetInflowRatio,
            @Param("superLargeNetInflow") BigDecimal superLargeNetInflow,
            @Param("superLargeNetRatio") BigDecimal superLargeNetRatio,
            @Param("largeNetInflow") BigDecimal largeNetInflow,
            @Param("largeNetRatio") BigDecimal largeNetRatio,
            @Param("mediumNetInflow") BigDecimal mediumNetInflow,
            @Param("mediumNetRatio") BigDecimal mediumNetRatio,
            @Param("smallNetInflow") BigDecimal smallNetInflow,
            @Param("smallNetRatio") BigDecimal smallNetRatio,
            @Param("now") OffsetDateTime now
    );

    int insertManualFundFlow(@Param("value") MarketOverviewPO value);

    LocalDate findLatestPulseTradeDate(@Param("endDate") LocalDate endDate);

    MarketPulseCaptureBatchPO findLatestPulseBatch(@Param("tradeDate") LocalDate tradeDate);

    List<LocalDate> listLatestPulseTradeDates(@Param("limit") int limit);

    List<MarketPulseSectorDailySnapshotPO> listPulseRowsByTradeDate(@Param("tradeDate") LocalDate tradeDate);

    List<MarketPulseSectorDailySnapshotPO> listPulseRowsByTradeDates(@Param("tradeDates") List<LocalDate> tradeDates);

    List<StyleSectorSnapshotPO> listLatestStyleSectors();

    StyleSectorSnapshotPO findLatestStyleSector(@Param("name") String name);

    HeatmapSnapshotPO findLatestHeatmap(@Param("kind") String kind);

    MarketLimitDailySnapshotPO findLatestLimitEmotion();

    MarketLimitDailySnapshotPO findLimitEmotionByDate(@Param("tradeDate") LocalDate tradeDate);

    MarketLimitDailySnapshotPO findLimitEmotionBefore(@Param("tradeDate") LocalDate tradeDate);

    List<MarketLimitDailyStockPO> listLimitStocks(
            @Param("tradeDate") LocalDate tradeDate,
            @Param("category") String category,
            @Param("limit") int limit
    );

    List<IndexReturnDailyPO> listLatestIndexReturns(@Param("windowDays") int windowDays);

    List<IndexReturnDailyPO> listIndexReturnsHistory(
            @Param("windowDays") int windowDays,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    IndexReturnDailyPO findLatestIndexReturnByCode(@Param("indexCode") String indexCode);
}
