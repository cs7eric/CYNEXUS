package cn.cccs7.cynexus.market.domain.service.sentiment;

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
import cn.cccs7.cynexus.market.domain.sentiment.MarketSentimentDomainService;
import cn.cccs7.cynexus.market.domain.valueobject.sentiment.MarketSentimentHistoryResult;
import cn.cccs7.cynexus.market.domain.valueobject.sentiment.MarketSentimentIndexDailyResult;
import cn.cccs7.cynexus.market.domain.valueobject.sentiment.MarketSentimentQueryResult;
import java.time.LocalDate;

/**
 * Unified domain entry point for market sentiment query use cases.
 *
 * <p>Repository access, serving-date resolution, metric fallback and score
 * derivation are deliberately kept behind this port. The application layer
 * only adapts the returned domain results to API DTOs.</p>
 */
public interface MarketSentimentQueryDomainService extends MarketSentimentDomainService {

    MarketSentimentQueryResult<MaCountDailyBO> maCount(LocalDate date);

    MarketSentimentHistoryResult<MaCountDailyBO> maCountHistory(LocalDate start, LocalDate end);

    MarketSentimentQueryResult<RiskAppetiteDailyBO> riskAppetite(LocalDate date);

    MarketSentimentHistoryResult<RiskAppetiteDailyBO> riskAppetiteHistory(LocalDate start, LocalDate end);

    MarketSentimentQueryResult<MarketSentimentLimitEmotionDailyBO> limitEmotion(LocalDate date);

    MarketSentimentHistoryResult<MarketSentimentLimitEmotionDailyBO> limitEmotionHistory(LocalDate start, LocalDate end);

    MarketSentimentQueryResult<VolatilityDailyBO> volatility(LocalDate date);

    MarketSentimentHistoryResult<VolatilityDailyBO> volatilityHistory(LocalDate start, LocalDate end);

    MarketSentimentQueryResult<TurnoverActivityDailyBO> turnover(LocalDate date);

    MarketSentimentHistoryResult<TurnoverActivityDailyBO> turnoverHistory(LocalDate start, LocalDate end);

    MarketSentimentQueryResult<StyleRiskDailyBO> styleRisk(LocalDate date);

    MarketSentimentHistoryResult<StyleRiskDailyBO> styleRiskHistory(LocalDate start, LocalDate end);

    MarketSentimentQueryResult<ProfitEffectDailyBO> profitEffect(LocalDate date);

    MarketSentimentHistoryResult<ProfitEffectDailyBO> profitEffectHistory(LocalDate start, LocalDate end);

    MarketSentimentQueryResult<SectorBreadthDailyBO> sectorBreadth(LocalDate date);

    MarketSentimentHistoryResult<SectorBreadthDailyBO> sectorBreadthHistory(int days, LocalDate end);

    MarketSentimentQueryResult<MsiIndexDailyBO> msiIndex(LocalDate date);

    MarketSentimentHistoryResult<MsiIndexDailyBO> msiIndexHistory(LocalDate start, LocalDate end);

    MarketSentimentHistoryResult<MarketOverviewBO> overviewHistory(LocalDate start, LocalDate end);

    MarketSentimentIndexDailyResult indexDaily(String code, LocalDate start, LocalDate end);

    /** Kept public for existing domain policy consumers during migration. */
    boolean matchesTradeDate(LocalDate expectedDate, LocalDate actualDate);

    /** Kept public for existing domain policy consumers during migration. */
    int count(Integer value);
}
