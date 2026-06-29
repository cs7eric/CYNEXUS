package cn.cccs7.cynexus.market.infra.basic.service.impl;

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
import cn.cccs7.cynexus.market.infra.basic.mapper.MarketSentimentQueryMapper;
import cn.cccs7.cynexus.market.infra.basic.service.MarketSentimentQueryBasicService;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class MarketSentimentQueryBasicServiceImpl implements MarketSentimentQueryBasicService {

    private final MarketSentimentQueryMapper mapper;

    public MarketSentimentQueryBasicServiceImpl(MarketSentimentQueryMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<MaCountDailyPO> findMaCount(LocalDate tradeDate) {
        return Optional.ofNullable(mapper.findMaCount(tradeDate));
    }

    @Override
    public List<MaCountDailyPO> listMaCount(LocalDate startDate, LocalDate endDate) {
        return mapper.listMaCount(startDate, endDate);
    }

    @Override
    public Optional<RiskAppetiteDailyPO> findRiskAppetite(LocalDate tradeDate) {
        return Optional.ofNullable(mapper.findRiskAppetite(tradeDate));
    }

    @Override
    public List<RiskAppetiteDailyPO> listRiskAppetite(LocalDate startDate, LocalDate endDate) {
        return mapper.listRiskAppetite(startDate, endDate);
    }

    @Override
    public Optional<MarketSentimentLimitEmotionDailyPO> findLimitEmotion(LocalDate tradeDate) {
        return Optional.ofNullable(mapper.findLimitEmotion(tradeDate));
    }

    @Override
    public List<MarketSentimentLimitEmotionDailyPO> listLimitEmotion(LocalDate startDate, LocalDate endDate) {
        return mapper.listLimitEmotion(startDate, endDate);
    }

    @Override
    public Optional<VolatilityDailyPO> findVolatility(LocalDate tradeDate) {
        return Optional.ofNullable(mapper.findVolatility(tradeDate));
    }

    @Override
    public List<VolatilityDailyPO> listVolatility(LocalDate startDate, LocalDate endDate) {
        return mapper.listVolatility(startDate, endDate);
    }

    @Override
    public Optional<TurnoverActivityDailyPO> findTurnover(LocalDate tradeDate) {
        return Optional.ofNullable(mapper.findTurnover(tradeDate));
    }

    @Override
    public List<TurnoverActivityDailyPO> listTurnover(LocalDate startDate, LocalDate endDate) {
        return mapper.listTurnover(startDate, endDate);
    }

    @Override
    public Optional<StyleRiskDailyPO> findStyleRisk(LocalDate tradeDate) {
        return Optional.ofNullable(mapper.findStyleRisk(tradeDate));
    }

    @Override
    public List<StyleRiskDailyPO> listStyleRisk(LocalDate startDate, LocalDate endDate) {
        return mapper.listStyleRisk(startDate, endDate);
    }

    @Override
    public Optional<ProfitEffectDailyPO> findProfitEffect(LocalDate tradeDate) {
        return Optional.ofNullable(mapper.findProfitEffect(tradeDate));
    }

    @Override
    public List<ProfitEffectDailyPO> listProfitEffect(LocalDate startDate, LocalDate endDate) {
        return mapper.listProfitEffect(startDate, endDate);
    }

    @Override
    public Optional<SectorBreadthDailyPO> findSectorBreadth(LocalDate tradeDate) {
        return Optional.ofNullable(mapper.findSectorBreadth(tradeDate));
    }

    @Override
    public List<SectorBreadthDailyPO> listSectorBreadth(LocalDate startDate, LocalDate endDate) {
        return mapper.listSectorBreadth(startDate, endDate);
    }

    @Override
    public Optional<MsiIndexDailyPO> findMsiIndex(LocalDate tradeDate) {
        return Optional.ofNullable(mapper.findMsiIndex(tradeDate));
    }

    @Override
    public List<MsiIndexDailyPO> listMsiIndex(LocalDate startDate, LocalDate endDate) {
        return mapper.listMsiIndex(startDate, endDate);
    }

    @Override
    public List<MarketOverviewPO> listOverview(LocalDate startDate, LocalDate endDate) {
        return mapper.listOverview(startDate, endDate);
    }

    @Override
    public List<IndexDailyBarPO> listIndexDaily(String code, LocalDate startDate, LocalDate endDate) {
        return mapper.listIndexDaily(code, startDate, endDate);
    }
}
