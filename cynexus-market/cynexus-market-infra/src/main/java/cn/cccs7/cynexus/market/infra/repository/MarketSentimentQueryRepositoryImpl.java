package cn.cccs7.cynexus.market.infra.repository;

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
import cn.cccs7.cynexus.market.domain.repository.MarketSentimentQueryRepository;
import cn.cccs7.cynexus.market.infra.basic.service.MarketSentimentQueryBasicService;
import cn.cccs7.cynexus.market.infra.convert.IndexDailyBarPOConverter;
import cn.cccs7.cynexus.market.infra.convert.MaCountDailyPOConverter;
import cn.cccs7.cynexus.market.infra.convert.MarketOverviewPOConverter;
import cn.cccs7.cynexus.market.infra.convert.MarketSentimentLimitEmotionDailyPOConverter;
import cn.cccs7.cynexus.market.infra.convert.MsiIndexDailyPOConverter;
import cn.cccs7.cynexus.market.infra.convert.ProfitEffectDailyPOConverter;
import cn.cccs7.cynexus.market.infra.convert.RiskAppetiteDailyPOConverter;
import cn.cccs7.cynexus.market.infra.convert.SectorBreadthDailyPOConverter;
import cn.cccs7.cynexus.market.infra.convert.StyleRiskDailyPOConverter;
import cn.cccs7.cynexus.market.infra.convert.TurnoverActivityDailyPOConverter;
import cn.cccs7.cynexus.market.infra.convert.VolatilityDailyPOConverter;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class MarketSentimentQueryRepositoryImpl implements MarketSentimentQueryRepository {

    private final MarketSentimentQueryBasicService basicService;

    public MarketSentimentQueryRepositoryImpl(MarketSentimentQueryBasicService basicService) {
        this.basicService = basicService;
    }

    @Override
    public Optional<MaCountDailyBO> findMaCount(LocalDate tradeDate) {
        return basicService.findMaCount(tradeDate).map(MaCountDailyPOConverter.INSTANCE::toTarget);
    }

    @Override
    public List<MaCountDailyBO> listMaCount(LocalDate startDate, LocalDate endDate) {
        return MaCountDailyPOConverter.INSTANCE.toTargetList(basicService.listMaCount(startDate, endDate));
    }

    @Override
    public Optional<RiskAppetiteDailyBO> findRiskAppetite(LocalDate tradeDate) {
        return basicService.findRiskAppetite(tradeDate).map(RiskAppetiteDailyPOConverter.INSTANCE::toTarget);
    }

    @Override
    public List<RiskAppetiteDailyBO> listRiskAppetite(LocalDate startDate, LocalDate endDate) {
        return RiskAppetiteDailyPOConverter.INSTANCE.toTargetList(basicService.listRiskAppetite(startDate, endDate));
    }

    @Override
    public Optional<MarketSentimentLimitEmotionDailyBO> findLimitEmotion(LocalDate tradeDate) {
        return basicService.findLimitEmotion(tradeDate).map(MarketSentimentLimitEmotionDailyPOConverter.INSTANCE::toTarget);
    }

    @Override
    public List<MarketSentimentLimitEmotionDailyBO> listLimitEmotion(LocalDate startDate, LocalDate endDate) {
        return MarketSentimentLimitEmotionDailyPOConverter.INSTANCE.toTargetList(basicService.listLimitEmotion(startDate, endDate));
    }

    @Override
    public Optional<VolatilityDailyBO> findVolatility(LocalDate tradeDate) {
        return basicService.findVolatility(tradeDate).map(VolatilityDailyPOConverter.INSTANCE::toTarget);
    }

    @Override
    public List<VolatilityDailyBO> listVolatility(LocalDate startDate, LocalDate endDate) {
        return VolatilityDailyPOConverter.INSTANCE.toTargetList(basicService.listVolatility(startDate, endDate));
    }

    @Override
    public Optional<TurnoverActivityDailyBO> findTurnover(LocalDate tradeDate) {
        return basicService.findTurnover(tradeDate).map(TurnoverActivityDailyPOConverter.INSTANCE::toTarget);
    }

    @Override
    public List<TurnoverActivityDailyBO> listTurnover(LocalDate startDate, LocalDate endDate) {
        return TurnoverActivityDailyPOConverter.INSTANCE.toTargetList(basicService.listTurnover(startDate, endDate));
    }

    @Override
    public Optional<StyleRiskDailyBO> findStyleRisk(LocalDate tradeDate) {
        return basicService.findStyleRisk(tradeDate).map(StyleRiskDailyPOConverter.INSTANCE::toTarget);
    }

    @Override
    public List<StyleRiskDailyBO> listStyleRisk(LocalDate startDate, LocalDate endDate) {
        return StyleRiskDailyPOConverter.INSTANCE.toTargetList(basicService.listStyleRisk(startDate, endDate));
    }

    @Override
    public Optional<ProfitEffectDailyBO> findProfitEffect(LocalDate tradeDate) {
        return basicService.findProfitEffect(tradeDate).map(ProfitEffectDailyPOConverter.INSTANCE::toTarget);
    }

    @Override
    public List<ProfitEffectDailyBO> listProfitEffect(LocalDate startDate, LocalDate endDate) {
        return ProfitEffectDailyPOConverter.INSTANCE.toTargetList(basicService.listProfitEffect(startDate, endDate));
    }

    @Override
    public Optional<SectorBreadthDailyBO> findSectorBreadth(LocalDate tradeDate) {
        return basicService.findSectorBreadth(tradeDate).map(SectorBreadthDailyPOConverter.INSTANCE::toTarget);
    }

    @Override
    public List<SectorBreadthDailyBO> listSectorBreadth(LocalDate startDate, LocalDate endDate) {
        return SectorBreadthDailyPOConverter.INSTANCE.toTargetList(basicService.listSectorBreadth(startDate, endDate));
    }

    @Override
    public Optional<MsiIndexDailyBO> findMsiIndex(LocalDate tradeDate) {
        return basicService.findMsiIndex(tradeDate).map(MsiIndexDailyPOConverter.INSTANCE::toTarget);
    }

    @Override
    public List<MsiIndexDailyBO> listMsiIndex(LocalDate startDate, LocalDate endDate) {
        return MsiIndexDailyPOConverter.INSTANCE.toTargetList(basicService.listMsiIndex(startDate, endDate));
    }

    @Override
    public List<MarketOverviewBO> listOverview(LocalDate startDate, LocalDate endDate) {
        return MarketOverviewPOConverter.INSTANCE.toTargetList(basicService.listOverview(startDate, endDate));
    }

    @Override
    public List<IndexDailyBarBO> listIndexDaily(String code, LocalDate startDate, LocalDate endDate) {
        return IndexDailyBarPOConverter.INSTANCE.toTargetList(basicService.listIndexDaily(normalizeIndexCode(code), startDate, endDate));
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
