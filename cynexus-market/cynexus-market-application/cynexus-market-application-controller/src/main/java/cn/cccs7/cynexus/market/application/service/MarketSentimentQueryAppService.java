package cn.cccs7.cynexus.market.application.service;

import cn.cccs7.cynexus.market.api.dto.MarketPulseOverviewHistoryDTO;
import cn.cccs7.cynexus.market.api.dto.MarketSentimentIndexDailyDTO;
import cn.cccs7.cynexus.market.api.dto.MarketSentimentLimitEmotionSummaryDTO;
import cn.cccs7.cynexus.market.api.dto.MarketSentimentMaCountDTO;
import cn.cccs7.cynexus.market.api.dto.MarketSentimentMsiIndexDTO;
import cn.cccs7.cynexus.market.api.dto.MarketSentimentProfitEffectDTO;
import cn.cccs7.cynexus.market.api.dto.MarketSentimentRiskAppetiteDTO;
import cn.cccs7.cynexus.market.api.dto.MarketSentimentSectorBreadthDTO;
import cn.cccs7.cynexus.market.api.dto.MarketSentimentStyleRiskDTO;
import cn.cccs7.cynexus.market.api.dto.MarketSentimentTurnoverDTO;
import cn.cccs7.cynexus.market.api.dto.MarketSentimentVolatilityDTO;
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
import cn.cccs7.cynexus.market.domain.service.sentiment.MarketSentimentQueryDomainService;
import cn.cccs7.cynexus.market.domain.valueobject.sentiment.MarketSentimentHistoryResult;
import cn.cccs7.cynexus.market.domain.valueobject.sentiment.MarketSentimentIndexDailyResult;
import cn.cccs7.cynexus.market.domain.valueobject.sentiment.MarketSentimentQueryResult;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class MarketSentimentQueryAppService {

    private final MarketSentimentQueryDomainService domainService;

    public MarketSentimentQueryAppService(MarketSentimentQueryDomainService domainService) {
        this.domainService = domainService;
    }

    public MarketSentimentMaCountDTO getMaCount(LocalDate date) {
        MarketSentimentQueryResult<MaCountDailyBO> result = domainService.maCount(date);
        return result.present() ? maCount(result.value()) : emptyMaCount(result.effectiveDate());
    }

    public MarketSentimentMaCountDTO.HistoryResponseDTO getMaCountHistory(LocalDate start, LocalDate end) {
        MarketSentimentHistoryResult<MaCountDailyBO> result = domainService.maCountHistory(start, end);
        List<MarketSentimentMaCountDTO.HistoryItemDTO> items = result.items().stream()
                .map(MarketSentimentHistoryResult.Item::value)
                .map(this::maCountHistoryItem)
                .toList();
        return MarketSentimentMaCountDTO.HistoryResponseDTO.builder()
                .ok(true).start(start).end(end).count(items.size()).items(items).build();
    }

    public MarketSentimentRiskAppetiteDTO getRiskAppetite(LocalDate date) {
        MarketSentimentQueryResult<RiskAppetiteDailyBO> result = domainService.riskAppetite(date);
        return result.present() ? riskAppetite(result.value(), result.resolvedScore())
                : emptyRiskAppetite(result.effectiveDate());
    }

    public MarketSentimentRiskAppetiteDTO.HistoryResponseDTO getRiskAppetiteHistory(LocalDate start, LocalDate end) {
        MarketSentimentHistoryResult<RiskAppetiteDailyBO> result = domainService.riskAppetiteHistory(start, end);
        List<MarketSentimentRiskAppetiteDTO.HistoryItemDTO> items = result.items().stream()
                .map(item -> riskHistoryItem(item.value(), item.resolvedScore()))
                .toList();
        return MarketSentimentRiskAppetiteDTO.HistoryResponseDTO.builder()
                .ok(true).start(start).end(end).count(items.size()).items(items).build();
    }

    public MarketSentimentLimitEmotionSummaryDTO getLimitEmotion(LocalDate date) {
        MarketSentimentQueryResult<MarketSentimentLimitEmotionDailyBO> result = domainService.limitEmotion(date);
        return result.present() ? limitEmotion(result.value()) : emptyLimitEmotion(result.effectiveDate());
    }

    public MarketSentimentLimitEmotionSummaryDTO.HistoryResponseDTO getLimitEmotionHistory(LocalDate start, LocalDate end) {
        MarketSentimentHistoryResult<MarketSentimentLimitEmotionDailyBO> result = domainService.limitEmotionHistory(start, end);
        List<MarketSentimentLimitEmotionSummaryDTO.HistoryItemDTO> items = result.items().stream()
                .map(MarketSentimentHistoryResult.Item::value)
                .map(this::limitEmotionHistoryItem)
                .toList();
        return MarketSentimentLimitEmotionSummaryDTO.HistoryResponseDTO.builder()
                .ok(true).start(start).end(end).count(items.size()).items(items).build();
    }

    public MarketSentimentVolatilityDTO getVolatility(LocalDate date) {
        MarketSentimentQueryResult<VolatilityDailyBO> result = domainService.volatility(date);
        return result.present() ? volatility(result.value()) : emptyVolatility(result.effectiveDate());
    }

    public MarketSentimentVolatilityDTO.HistoryResponseDTO getVolatilityHistory(LocalDate start, LocalDate end) {
        MarketSentimentHistoryResult<VolatilityDailyBO> result = domainService.volatilityHistory(start, end);
        List<MarketSentimentVolatilityDTO.HistoryItemDTO> items = result.items().stream()
                .map(MarketSentimentHistoryResult.Item::value)
                .map(this::volatilityHistoryItem)
                .toList();
        return MarketSentimentVolatilityDTO.HistoryResponseDTO.builder()
                .ok(true).start(start).end(end).count(items.size()).items(items).build();
    }

    public MarketSentimentTurnoverDTO getTurnover(LocalDate date) {
        MarketSentimentQueryResult<TurnoverActivityDailyBO> result = domainService.turnover(date);
        return result.present() ? turnover(result.value()) : emptyTurnover(result.effectiveDate());
    }

    public MarketSentimentTurnoverDTO.HistoryResponseDTO getTurnoverHistory(LocalDate start, LocalDate end) {
        MarketSentimentHistoryResult<TurnoverActivityDailyBO> result = domainService.turnoverHistory(start, end);
        List<MarketSentimentTurnoverDTO.HistoryItemDTO> items = result.items().stream()
                .map(MarketSentimentHistoryResult.Item::value)
                .map(this::turnoverHistoryItem)
                .toList();
        return MarketSentimentTurnoverDTO.HistoryResponseDTO.builder()
                .ok(true).start(start).end(end).count(items.size()).items(items).build();
    }

    public MarketSentimentStyleRiskDTO getStyleRisk(LocalDate date) {
        MarketSentimentQueryResult<StyleRiskDailyBO> result = domainService.styleRisk(date);
        return result.present() ? styleRisk(result.value()) : emptyStyleRisk(result.effectiveDate());
    }

    public MarketSentimentStyleRiskDTO.HistoryResponseDTO getStyleRiskHistory(LocalDate start, LocalDate end) {
        MarketSentimentHistoryResult<StyleRiskDailyBO> result = domainService.styleRiskHistory(start, end);
        List<MarketSentimentStyleRiskDTO.HistoryItemDTO> items = result.items().stream()
                .map(MarketSentimentHistoryResult.Item::value)
                .map(this::styleRiskHistoryItem)
                .toList();
        return MarketSentimentStyleRiskDTO.HistoryResponseDTO.builder()
                .ok(true).start(start).end(end).count(items.size()).items(items).build();
    }

    public MarketSentimentProfitEffectDTO getProfitEffect(LocalDate date) {
        MarketSentimentQueryResult<ProfitEffectDailyBO> result = domainService.profitEffect(date);
        return result.present() ? profitEffect(result.value(), result.resolvedScore())
                : emptyProfitEffect(result.effectiveDate());
    }

    public MarketSentimentProfitEffectDTO.HistoryResponseDTO getProfitEffectHistory(LocalDate start, LocalDate end) {
        MarketSentimentHistoryResult<ProfitEffectDailyBO> result = domainService.profitEffectHistory(start, end);
        List<MarketSentimentProfitEffectDTO.HistoryItemDTO> items = result.items().stream()
                .map(item -> profitHistoryItem(item.value(), item.resolvedScore()))
                .toList();
        return MarketSentimentProfitEffectDTO.HistoryResponseDTO.builder()
                .ok(true).start(start).end(end).count(items.size()).items(items).build();
    }

    public MarketSentimentSectorBreadthDTO getSectorBreadth(LocalDate date) {
        MarketSentimentQueryResult<SectorBreadthDailyBO> result = domainService.sectorBreadth(date);
        return result.present() ? sectorBreadth(result.value(), result.resolvedScore())
                : emptySectorBreadth(result.effectiveDate());
    }

    public MarketSentimentSectorBreadthDTO.HistoryResponseDTO getSectorBreadthHistory(int days, LocalDate end) {
        MarketSentimentHistoryResult<SectorBreadthDailyBO> result = domainService.sectorBreadthHistory(days, end);
        List<MarketSentimentSectorBreadthDTO.HistoryItemDTO> items = result.items().stream()
                .map(item -> sectorBreadthHistoryItem(item.value(), item.resolvedScore()))
                .toList();
        return MarketSentimentSectorBreadthDTO.HistoryResponseDTO.builder()
                .ok(true).start(result.start()).end(result.end()).days(days).count(items.size()).items(items).build();
    }

    public MarketSentimentMsiIndexDTO getMsiIndex(LocalDate date) {
        MarketSentimentQueryResult<MsiIndexDailyBO> result = domainService.msiIndex(date);
        return result.present() ? msiIndex(result.value()) : emptyMsiIndex(result.effectiveDate());
    }

    public MarketSentimentMsiIndexDTO.HistoryResponseDTO getMsiIndexHistory(LocalDate start, LocalDate end) {
        MarketSentimentHistoryResult<MsiIndexDailyBO> result = domainService.msiIndexHistory(start, end);
        List<MarketSentimentMsiIndexDTO.HistoryItemDTO> items = result.items().stream()
                .map(MarketSentimentHistoryResult.Item::value)
                .map(this::msiIndexHistoryItem)
                .toList();
        return MarketSentimentMsiIndexDTO.HistoryResponseDTO.builder()
                .ok(true).start(start).end(end).count(items.size()).items(items).build();
    }

    public MarketPulseOverviewHistoryDTO getOverviewHistory(LocalDate start, LocalDate end) {
        MarketSentimentHistoryResult<MarketOverviewBO> result = domainService.overviewHistory(start, end);
        List<MarketPulseOverviewHistoryDTO.ItemDTO> items = result.items().stream()
                .map(MarketSentimentHistoryResult.Item::value)
                .map(this::overviewHistoryItem)
                .toList();
        return MarketPulseOverviewHistoryDTO.builder()
                .ok(true)
                .range(start + ":" + end)
                .source("java-pg")
                .count(items.size())
                .items(items)
                .build();
    }

    public MarketSentimentIndexDailyDTO getIndexDaily(String code, LocalDate start, LocalDate end) {
        MarketSentimentIndexDailyResult result = domainService.indexDaily(code, start, end);
        List<IndexDailyBarBO> rows = result.items();
        List<MarketSentimentIndexDailyDTO.ItemDTO> items = rows.stream()
                .map(row -> MarketSentimentIndexDailyDTO.ItemDTO.builder()
                        .tradeDate(row.getTradeDate())
                        .close(row.getClose())
                        .amount(row.getAmount())
                        .build())
                .toList();
        return MarketSentimentIndexDailyDTO.builder()
                .ok(true)
                .code(code)
                .name(result.name())
                .start(start)
                .end(end)
                .count(items.size())
                .items(items)
                .build();
    }

    private MarketSentimentMaCountDTO maCount(MaCountDailyBO row) {
        return MarketSentimentMaCountDTO.builder()
                .ok(true).tradeDate(row.getTradeDate()).totalEligible(n(row.getTotalEligible()))
                .aboveMa20(n(row.getAboveMa20())).aboveMa60(n(row.getAboveMa60())).aboveBoth(n(row.getAboveBoth()))
                .pctAboveMa20(row.getPctMa20()).pctAboveMa60(row.getPctMa60()).pctAboveBoth(row.getPctBoth())
                .up5dCount(n(row.getUp5dCount())).pctUp5d(row.getUp5dPct())
                .newLow60dCount(n(row.getNewLow60dCount())).pctNewLow60d(row.getNewLow60dPct())
                .newHigh252dCount(n(row.getNewHigh252dCount())).pctNewHigh252d(row.getNewHigh252dPct())
                .advancingCount(n(row.getAdvancingCount())).pctAdvancing(row.getAdvancingPct())
                .newHigh252dScore(row.getPriceStrengthScore()).newHigh252dRawValue(row.getNewHigh252dPct())
                .breadthScore(row.getBreadthScore()).breadthRawValue(row.getBreadthRaw())
                .elapsedMs(row.getElapsedMs()).source(row.getSource()).fromCache(true).build();
    }

    private MarketSentimentMaCountDTO.HistoryItemDTO maCountHistoryItem(MaCountDailyBO row) {
        return MarketSentimentMaCountDTO.HistoryItemDTO.builder()
                .tradeDate(row.getTradeDate()).totalEligible(n(row.getTotalEligible()))
                .aboveMa20(n(row.getAboveMa20())).aboveMa60(n(row.getAboveMa60())).aboveBoth(n(row.getAboveBoth()))
                .pctAboveMa20(row.getPctMa20()).pctAboveMa60(row.getPctMa60()).pctAboveBoth(row.getPctBoth())
                .up5dCount(n(row.getUp5dCount())).pctUp5d(row.getUp5dPct())
                .newLow60dCount(n(row.getNewLow60dCount())).pctNewLow60d(row.getNewLow60dPct())
                .newHigh252dCount(n(row.getNewHigh252dCount())).pctNewHigh252d(row.getNewHigh252dPct())
                .advancingCount(n(row.getAdvancingCount())).pctAdvancing(row.getAdvancingPct())
                .newHigh252dScore(row.getPriceStrengthScore()).breadthScore(row.getBreadthScore()).fromCache(true).build();
    }

    private MarketSentimentRiskAppetiteDTO riskAppetite(RiskAppetiteDailyBO row, BigDecimal score) {
        return MarketSentimentRiskAppetiteDTO.builder()
                .ok(true).tradeDate(row.getTradeDate()).windowDays(row.getWindowDays())
                .hs300(asset(row.getHs300Close(), row.getTradeDate(), row.getHs300BaseClose(), row.getHs300BaseDate(), row.getHs30020dReturn(), null))
                .treasury(MarketSentimentRiskAppetiteDTO.TreasuryDTO.builder()
                        .asset511010(asset(row.getTreasury511010Close(), row.getTradeDate(), row.getTreasury511010BaseClose(), null, row.getTreasury51101020dReturn(), domainService.treasuryWeight()))
                        .asset511090(asset(row.getTreasury511090Close(), row.getTradeDate(), row.getTreasury511090BaseClose(), null, row.getTreasury51109020dReturn(), domainService.treasuryWeight()))
                        .weighted(MarketSentimentRiskAppetiteDTO.WeightedDTO.builder().returnPct(row.getTreasuryWeighted20dReturn()).build())
                        .build())
                .spread(MarketSentimentRiskAppetiteDTO.SpreadDTO.builder()
                        .spread511010(row.getSpread511010()).spread511090(row.getSpread511090()).weighted(row.getSpreadWeighted()).build())
                .score(score).rawValue(row.getSpreadWeighted()).elapsedMs(row.getElapsedMs())
                .source(row.getSource()).fromCache(true).build();
    }

    private MarketSentimentRiskAppetiteDTO.AssetDTO asset(BigDecimal close, LocalDate currentDate, BigDecimal baseClose,
                                                          LocalDate baseDate, BigDecimal returnPct, BigDecimal weight) {
        return MarketSentimentRiskAppetiteDTO.AssetDTO.builder()
                .close(close).currentDate(currentDate).baseClose(baseClose).baseDate(baseDate).returnPct(returnPct).weight(weight).build();
    }

    private MarketSentimentRiskAppetiteDTO.HistoryItemDTO riskHistoryItem(RiskAppetiteDailyBO row, BigDecimal msiScore) {
                BigDecimal score = domainService.resolveScore(row.getScore(), msiScore);
        return MarketSentimentRiskAppetiteDTO.HistoryItemDTO.builder()
                .tradeDate(row.getTradeDate()).hs300ReturnPct(row.getHs30020dReturn())
                .treasury511010ReturnPct(row.getTreasury51101020dReturn())
                .treasury511090ReturnPct(row.getTreasury51109020dReturn())
                .treasuryWeightedReturnPct(row.getTreasuryWeighted20dReturn())
                .spread511010(row.getSpread511010()).spread511090(row.getSpread511090()).spreadWeighted(row.getSpreadWeighted())
                .score(score).fromCache(true).build();
    }

    private MarketSentimentLimitEmotionSummaryDTO limitEmotion(MarketSentimentLimitEmotionDailyBO row) {
        return MarketSentimentLimitEmotionSummaryDTO.builder()
                .ok(true).tradeDate(row.getTradeDate()).limitUpCount(n(row.getLimitUpCount())).limitDownCount(n(row.getLimitDownCount()))
                .touchedCount(n(row.getTouchedCount())).brokenCount(n(row.getBrokenCount())).breakBoardRate(row.getBreakBoardRate())
                .limitUpDownRatio(row.getLimitUpDownRatio()).yesterdayLimitUpCount(n(row.getYesterdayLimitUpCount()))
                .yesterdayLimitUpAvgReturn(row.getYesterdayLimitUpAvgReturn())
                .components(MarketSentimentLimitEmotionSummaryDTO.ComponentsDTO.builder()
                        .upDownScore(row.getUpDownScore()).breakBoardScore(row.getBreakBoardScore())
                        .yesterdayReturnScore(row.getYesterdayReturnScore()).build())
                .compositeScore(row.getCompositeScore()).level(row.getLevel()).elapsedMs(row.getElapsedMs())
                .source(row.getSource()).fromCache(true).build();
    }

    private MarketSentimentLimitEmotionSummaryDTO.HistoryItemDTO limitEmotionHistoryItem(MarketSentimentLimitEmotionDailyBO row) {
        return MarketSentimentLimitEmotionSummaryDTO.HistoryItemDTO.builder()
                .tradeDate(row.getTradeDate()).limitUpCount(n(row.getLimitUpCount())).limitDownCount(n(row.getLimitDownCount()))
                .touchedCount(n(row.getTouchedCount())).brokenCount(n(row.getBrokenCount())).breakBoardRate(row.getBreakBoardRate())
                .limitUpDownRatio(row.getLimitUpDownRatio()).yesterdayLimitUpCount(n(row.getYesterdayLimitUpCount()))
                .yesterdayLimitUpAvgReturn(row.getYesterdayLimitUpAvgReturn()).compositeScore(row.getCompositeScore())
                .level(row.getLevel()).fromCache(true).build();
    }

    private MarketSentimentVolatilityDTO volatility(VolatilityDailyBO row) {
        return MarketSentimentVolatilityDTO.builder()
                .ok(true).tradeDate(row.getTradeDate()).underlyingCode(row.getUnderlyingCode()).underlyingName(row.getUnderlyingName())
                .close(row.getClose()).dailyReturnPct(row.getDailyReturnPct()).realizedVol20d(row.getRealizedVol20d())
                .volWindowDays(row.getVolWindowDays()).volLookbackDays(row.getVolLookbackDays()).percentile1y(row.getPercentile1y())
                .score(row.getSentimentScore()).sentimentScore(row.getSentimentScore())
                .sampleCount(row.getSampleCount()).elapsedMs(row.getElapsedMs())
                .source(row.getSource()).fromCache(true).build();
    }

    private MarketSentimentVolatilityDTO.HistoryItemDTO volatilityHistoryItem(VolatilityDailyBO row) {
        return MarketSentimentVolatilityDTO.HistoryItemDTO.builder()
                .tradeDate(row.getTradeDate()).underlyingCode(row.getUnderlyingCode()).underlyingName(row.getUnderlyingName())
                .close(row.getClose()).dailyReturnPct(row.getDailyReturnPct()).realizedVol20d(row.getRealizedVol20d())
                .volWindowDays(row.getVolWindowDays()).volLookbackDays(row.getVolLookbackDays()).percentile1y(row.getPercentile1y())
                .score(row.getSentimentScore()).sentimentScore(row.getSentimentScore())
                .sampleCount(row.getSampleCount()).elapsedMs(row.getElapsedMs())
                .source(row.getSource()).fromCache(true).build();
    }

    private MarketSentimentTurnoverDTO turnover(TurnoverActivityDailyBO row) {
        return MarketSentimentTurnoverDTO.builder()
                .ok(true).tradeDate(row.getTradeDate()).totalAmount(row.getTotalAmount()).avg20dAmount(row.getAvg20dAmount())
                .ratio(row.getRatio()).score(row.getScore()).rawValue(row.getRatio()).elapsedMs(row.getElapsedMs())
                .source(row.getSource()).build();
    }

    private MarketSentimentTurnoverDTO.HistoryItemDTO turnoverHistoryItem(TurnoverActivityDailyBO row) {
        return MarketSentimentTurnoverDTO.HistoryItemDTO.builder()
                .tradeDate(row.getTradeDate()).totalAmount(row.getTotalAmount()).avg20dAmount(row.getAvg20dAmount())
                .ratio(row.getRatio()).score(row.getScore()).elapsedMs(row.getElapsedMs()).source(row.getSource()).fromCache(true).build();
    }

    private MarketSentimentStyleRiskDTO styleRisk(StyleRiskDailyBO row) {
        return MarketSentimentStyleRiskDTO.builder()
                .ok(true).tradeDate(row.getTradeDate()).windowDays(row.getWindowDays())
                .hs300(styleIndex("沪深300", "000300", row.getHs300Return(), row.getTradeDate()))
                .csi1000(styleIndex("中证1000", "000852", row.getCsi1000Return(), row.getTradeDate()))
                .spread(row.getSpread()).score(row.getScore()).rawValue(row.getSpread()).elapsedMs(row.getElapsedMs())
                .source(row.getSource()).fromCache(true).build();
    }

    private MarketSentimentStyleRiskDTO.IndexDTO styleIndex(String name, String code, BigDecimal returnPct, LocalDate tradeDate) {
        return MarketSentimentStyleRiskDTO.IndexDTO.builder()
                .name(name).code(code).returnPct(returnPct).currentDate(tradeDate).build();
    }

    private MarketSentimentStyleRiskDTO.HistoryItemDTO styleRiskHistoryItem(StyleRiskDailyBO row) {
        return MarketSentimentStyleRiskDTO.HistoryItemDTO.builder()
                .tradeDate(row.getTradeDate()).spread(row.getSpread()).score(row.getScore()).fromCache(true).build();
    }

    private MarketSentimentProfitEffectDTO profitEffect(ProfitEffectDailyBO row, BigDecimal msiScore) {
        BigDecimal score = domainService.resolveScore(msiScore, row.getScore());
        return MarketSentimentProfitEffectDTO.builder()
                .ok(true).tradeDate(row.getTradeDate()).up5dPct(row.getUp5dPct()).newLow60dPct(row.getNewLow60dPct())
                .score(score).rawValue(row.getScore()).elapsedMs(row.getElapsedMs()).source(row.getSource()).fromCache(true).build();
    }

    private MarketSentimentProfitEffectDTO.HistoryItemDTO profitHistoryItem(ProfitEffectDailyBO row, BigDecimal msiScore) {
        BigDecimal score = msiScore == null ? row.getScore() : msiScore;
        return MarketSentimentProfitEffectDTO.HistoryItemDTO.builder()
                .tradeDate(row.getTradeDate()).score(score).fromCache(true).build();
    }

    private MarketSentimentSectorBreadthDTO sectorBreadth(SectorBreadthDailyBO row, BigDecimal score) {
        return MarketSentimentSectorBreadthDTO.builder()
                .ok(true).tradeDate(row.getTradeDate()).advancing(n(row.getAdvancing())).declining(n(row.getDeclining()))
                .flat(n(row.getFlat())).total(n(row.getTotal())).advancePct(row.getAdvancePct()).score(score)
                .source(row.getSource()).elapsedMs(row.getElapsedMs()).fromCache(true).build();
    }

    private MarketSentimentSectorBreadthDTO.HistoryItemDTO sectorBreadthHistoryItem(SectorBreadthDailyBO row, BigDecimal score) {
        return MarketSentimentSectorBreadthDTO.HistoryItemDTO.builder()
                .tradeDate(row.getTradeDate()).advancing(n(row.getAdvancing())).declining(n(row.getDeclining()))
                .flat(n(row.getFlat())).total(n(row.getTotal())).advancePct(row.getAdvancePct()).score(score)
                .source(row.getSource()).elapsedMs(row.getElapsedMs()).fromCache(true).build();
    }

    private MarketSentimentMsiIndexDTO msiIndex(MsiIndexDailyBO row) {
        return MarketSentimentMsiIndexDTO.builder()
                .ok(true).tradeDate(row.getTradeDate()).components(msiComponents(row)).weights(msiWeights())
                .compositeScore(row.getCompositeScore()).componentCount(row.getComponentCount()).level(row.getLevel())
                .elapsedMs(row.getElapsedMs()).source(row.getSource()).fromCache(true).build();
    }

    private MarketSentimentMsiIndexDTO.HistoryItemDTO msiIndexHistoryItem(MsiIndexDailyBO row) {
        return MarketSentimentMsiIndexDTO.HistoryItemDTO.builder()
                .tradeDate(row.getTradeDate()).compositeScore(row.getCompositeScore()).level(row.getLevel())
                .componentCount(row.getComponentCount()).components(msiComponents(row)).fromCache(true).build();
    }

    private MarketSentimentMsiIndexDTO.ComponentsDTO msiComponents(MsiIndexDailyBO row) {
        return MarketSentimentMsiIndexDTO.ComponentsDTO.builder()
                .vol(row.getVolScore()).turnover(row.getTurnoverScore()).priceStrength(row.getPriceStrengthScore())
                .riskAppetite(row.getRiskAppetiteScore()).breadth(row.getBreadthScore()).limitEmotion(row.getLimitEmotionScore())
                .profitEffect(row.getProfitEffectScore()).sectorBreadth(row.getSectorBreadthScore()).styleRisk(row.getStyleRiskScore()).build();
    }

    private MarketSentimentMsiIndexDTO.WeightsDTO msiWeights() {
        return MarketSentimentMsiIndexDTO.WeightsDTO.builder()
                .vol(domainService.msiWeights().get("vol")).turnover(domainService.msiWeights().get("turnover"))
                .priceStrength(domainService.msiWeights().get("priceStrength"))
                .riskAppetite(domainService.msiWeights().get("riskAppetite")).breadth(domainService.msiWeights().get("breadth"))
                .limitEmotion(domainService.msiWeights().get("limitEmotion")).profitEffect(domainService.msiWeights().get("profitEffect"))
                .sectorBreadth(domainService.msiWeights().get("sectorBreadth")).styleRisk(domainService.msiWeights().get("styleRisk")).build();
    }

    private MarketPulseOverviewHistoryDTO.ItemDTO overviewHistoryItem(MarketOverviewBO row) {
        return MarketPulseOverviewHistoryDTO.ItemDTO.builder()
                .date(row.getTradeDate()).tradeDate(row.getTradeDate()).totalAmount(row.getTotalAmount()).totalVolume(row.getTotalVolume())
                .risingCount(row.getRisingCount()).fallingCount(row.getFallingCount()).flatCount(row.getFlatCount())
                .limitUpCount(row.getLimitUpCount()).limitDownCount(row.getLimitDownCount())
                .mainNetInflow(row.getMainNetInflow()).superLargeNetInflow(row.getSuperLargeNetInflow())
                .largeNetInflow(row.getLargeNetInflow()).mediumNetInflow(row.getMediumNetInflow())
                .smallNetInflow(row.getSmallNetInflow()).source(row.getSource()).build();
    }

    private MarketSentimentMaCountDTO emptyMaCount(LocalDate date) {
        return MarketSentimentMaCountDTO.builder().ok(false).tradeDate(date).error("ma count is empty").build();
    }

    private MarketSentimentRiskAppetiteDTO emptyRiskAppetite(LocalDate date) {
        return MarketSentimentRiskAppetiteDTO.builder().ok(false).tradeDate(date).error("risk appetite is empty").build();
    }

    private MarketSentimentLimitEmotionSummaryDTO emptyLimitEmotion(LocalDate date) {
        return MarketSentimentLimitEmotionSummaryDTO.builder().ok(false).tradeDate(date).error("limit emotion is empty").build();
    }

    private MarketSentimentVolatilityDTO emptyVolatility(LocalDate date) {
        return MarketSentimentVolatilityDTO.builder().ok(false).tradeDate(date).error("volatility is empty").build();
    }

    private MarketSentimentTurnoverDTO emptyTurnover(LocalDate date) {
        return MarketSentimentTurnoverDTO.builder().ok(false).tradeDate(date).error("turnover is empty").build();
    }

    private MarketSentimentStyleRiskDTO emptyStyleRisk(LocalDate date) {
        return MarketSentimentStyleRiskDTO.builder().ok(false).tradeDate(date).error("style risk is empty").build();
    }

    private MarketSentimentProfitEffectDTO emptyProfitEffect(LocalDate date) {
        return MarketSentimentProfitEffectDTO.builder().ok(false).tradeDate(date).error("profit effect is empty").build();
    }

    private MarketSentimentSectorBreadthDTO emptySectorBreadth(LocalDate date) {
        return MarketSentimentSectorBreadthDTO.builder().ok(false).tradeDate(date).error("sector breadth is empty").build();
    }

    private MarketSentimentMsiIndexDTO emptyMsiIndex(LocalDate date) {
        return MarketSentimentMsiIndexDTO.builder().ok(false).tradeDate(date).components(emptyComponents())
                .weights(msiWeights()).componentCount(0).level("normal").error("msi index is empty").build();
    }

    private MarketSentimentMsiIndexDTO.ComponentsDTO emptyComponents() {
        return MarketSentimentMsiIndexDTO.ComponentsDTO.builder().build();
    }

    private int n(Integer value) {
        return domainService.count(value);
    }
}
