package cn.cccs7.cynexus.market.domain.repository;

import cn.cccs7.cynexus.market.domain.entity.IndexDailyBarBO;
import cn.cccs7.cynexus.market.domain.entity.MaCountDailyBO;
import cn.cccs7.cynexus.market.domain.entity.MarketOverviewBO;
import cn.cccs7.cynexus.market.domain.entity.MarketSentimentLimitEmotionDailyBO;
import cn.cccs7.cynexus.market.domain.entity.MsiIndexDailyBO;
import cn.cccs7.cynexus.market.domain.entity.ProfitEffectDailyBO;
import cn.cccs7.cynexus.market.domain.entity.RiskAppetiteDailyBO;
import cn.cccs7.cynexus.market.domain.entity.SectorBreadthDailyBO;
import cn.cccs7.cynexus.market.domain.entity.StyleRiskDailyBO;
import cn.cccs7.cynexus.market.domain.entity.TurnoverActivityDailyBO;
import cn.cccs7.cynexus.market.domain.entity.VolatilityDailyBO;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MarketSentimentQueryRepository {

    Optional<MaCountDailyBO> findMaCount(LocalDate tradeDate);

    List<MaCountDailyBO> listMaCount(LocalDate startDate, LocalDate endDate);

    Optional<RiskAppetiteDailyBO> findRiskAppetite(LocalDate tradeDate);

    List<RiskAppetiteDailyBO> listRiskAppetite(LocalDate startDate, LocalDate endDate);

    Optional<MarketSentimentLimitEmotionDailyBO> findLimitEmotion(LocalDate tradeDate);

    List<MarketSentimentLimitEmotionDailyBO> listLimitEmotion(LocalDate startDate, LocalDate endDate);

    Optional<VolatilityDailyBO> findVolatility(LocalDate tradeDate);

    List<VolatilityDailyBO> listVolatility(LocalDate startDate, LocalDate endDate);

    Optional<TurnoverActivityDailyBO> findTurnover(LocalDate tradeDate);

    List<TurnoverActivityDailyBO> listTurnover(LocalDate startDate, LocalDate endDate);

    Optional<StyleRiskDailyBO> findStyleRisk(LocalDate tradeDate);

    List<StyleRiskDailyBO> listStyleRisk(LocalDate startDate, LocalDate endDate);

    Optional<ProfitEffectDailyBO> findProfitEffect(LocalDate tradeDate);

    List<ProfitEffectDailyBO> listProfitEffect(LocalDate startDate, LocalDate endDate);

    Optional<SectorBreadthDailyBO> findSectorBreadth(LocalDate tradeDate);

    List<SectorBreadthDailyBO> listSectorBreadth(LocalDate startDate, LocalDate endDate);

    Optional<MsiIndexDailyBO> findMsiIndex(LocalDate tradeDate);

    List<MsiIndexDailyBO> listMsiIndex(LocalDate startDate, LocalDate endDate);

    List<MarketOverviewBO> listOverview(LocalDate startDate, LocalDate endDate);

    List<IndexDailyBarBO> listIndexDaily(String code, LocalDate startDate, LocalDate endDate);
}
