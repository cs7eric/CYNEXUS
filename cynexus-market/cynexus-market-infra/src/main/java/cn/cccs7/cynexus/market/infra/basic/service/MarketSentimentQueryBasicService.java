package cn.cccs7.cynexus.market.infra.basic.service;

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
import java.util.Optional;

public interface MarketSentimentQueryBasicService {

    Optional<MaCountDailyPO> findMaCount(LocalDate tradeDate);

    List<MaCountDailyPO> listMaCount(LocalDate startDate, LocalDate endDate);

    Optional<RiskAppetiteDailyPO> findRiskAppetite(LocalDate tradeDate);

    List<RiskAppetiteDailyPO> listRiskAppetite(LocalDate startDate, LocalDate endDate);

    Optional<MarketSentimentLimitEmotionDailyPO> findLimitEmotion(LocalDate tradeDate);

    List<MarketSentimentLimitEmotionDailyPO> listLimitEmotion(LocalDate startDate, LocalDate endDate);

    Optional<VolatilityDailyPO> findVolatility(LocalDate tradeDate);

    List<VolatilityDailyPO> listVolatility(LocalDate startDate, LocalDate endDate);

    Optional<TurnoverActivityDailyPO> findTurnover(LocalDate tradeDate);

    List<TurnoverActivityDailyPO> listTurnover(LocalDate startDate, LocalDate endDate);

    Optional<StyleRiskDailyPO> findStyleRisk(LocalDate tradeDate);

    List<StyleRiskDailyPO> listStyleRisk(LocalDate startDate, LocalDate endDate);

    Optional<ProfitEffectDailyPO> findProfitEffect(LocalDate tradeDate);

    List<ProfitEffectDailyPO> listProfitEffect(LocalDate startDate, LocalDate endDate);

    Optional<SectorBreadthDailyPO> findSectorBreadth(LocalDate tradeDate);

    List<SectorBreadthDailyPO> listSectorBreadth(LocalDate startDate, LocalDate endDate);

    Optional<MsiIndexDailyPO> findMsiIndex(LocalDate tradeDate);

    List<MsiIndexDailyPO> listMsiIndex(LocalDate startDate, LocalDate endDate);

    List<MarketOverviewPO> listOverview(LocalDate startDate, LocalDate endDate);

    List<IndexDailyBarPO> listIndexDaily(String code, LocalDate startDate, LocalDate endDate);
}
