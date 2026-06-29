package cn.cccs7.cynexus.market.infra.basic.mapper;

import cn.cccs7.cynexus.market.infra.basic.entity.IndexDailyBarPO;
import cn.cccs7.cynexus.market.infra.basic.entity.MaCountDailyPO;
import cn.cccs7.cynexus.market.infra.basic.entity.MarketOverviewPO;
import cn.cccs7.cynexus.market.infra.basic.entity.MarketSentimentLimitEmotionDailyPO;
import cn.cccs7.cynexus.market.infra.basic.entity.MsiIndexDailyPO;
import cn.cccs7.cynexus.market.infra.basic.entity.ProfitEffectDailyPO;
import cn.cccs7.cynexus.market.infra.basic.entity.RiskAppetiteDailyPO;
import cn.cccs7.cynexus.market.infra.basic.entity.SectorBreadthDailyPO;
import cn.cccs7.cynexus.market.infra.basic.entity.StyleRiskDailyPO;
import cn.cccs7.cynexus.market.infra.basic.entity.TurnoverActivityDailyPO;
import cn.cccs7.cynexus.market.infra.basic.entity.VolatilityDailyPO;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MarketSentimentQueryMapper {

    MaCountDailyPO findMaCount(@Param("tradeDate") LocalDate tradeDate);

    List<MaCountDailyPO> listMaCount(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    RiskAppetiteDailyPO findRiskAppetite(@Param("tradeDate") LocalDate tradeDate);

    List<RiskAppetiteDailyPO> listRiskAppetite(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    MarketSentimentLimitEmotionDailyPO findLimitEmotion(@Param("tradeDate") LocalDate tradeDate);

    List<MarketSentimentLimitEmotionDailyPO> listLimitEmotion(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    VolatilityDailyPO findVolatility(@Param("tradeDate") LocalDate tradeDate);

    List<VolatilityDailyPO> listVolatility(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    TurnoverActivityDailyPO findTurnover(@Param("tradeDate") LocalDate tradeDate);

    List<TurnoverActivityDailyPO> listTurnover(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    StyleRiskDailyPO findStyleRisk(@Param("tradeDate") LocalDate tradeDate);

    List<StyleRiskDailyPO> listStyleRisk(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    ProfitEffectDailyPO findProfitEffect(@Param("tradeDate") LocalDate tradeDate);

    List<ProfitEffectDailyPO> listProfitEffect(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    SectorBreadthDailyPO findSectorBreadth(@Param("tradeDate") LocalDate tradeDate);

    List<SectorBreadthDailyPO> listSectorBreadth(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    MsiIndexDailyPO findMsiIndex(@Param("tradeDate") LocalDate tradeDate);

    List<MsiIndexDailyPO> listMsiIndex(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    List<MarketOverviewPO> listOverview(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    List<IndexDailyBarPO> listIndexDaily(@Param("code") String code, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
