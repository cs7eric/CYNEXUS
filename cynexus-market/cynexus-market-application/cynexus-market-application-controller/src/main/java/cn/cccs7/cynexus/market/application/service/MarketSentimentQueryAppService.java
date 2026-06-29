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
import cn.cccs7.cynexus.market.domain.repository.MarketSentimentQueryRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class MarketSentimentQueryAppService {

    private static final BigDecimal WEIGHT_VOL = bd("0.15");
    private static final BigDecimal WEIGHT_TURNOVER = bd("0.15");
    private static final BigDecimal WEIGHT_PRICE_STRENGTH = bd("0.10");
    private static final BigDecimal WEIGHT_RISK_APPETITE = bd("0.10");
    private static final BigDecimal WEIGHT_BREADTH = bd("0.15");
    private static final BigDecimal WEIGHT_LIMIT_EMOTION = bd("0.15");
    private static final BigDecimal WEIGHT_PROFIT_EFFECT = bd("0.10");
    private static final BigDecimal WEIGHT_SECTOR_BREADTH = bd("0.05");
    private static final BigDecimal WEIGHT_STYLE_RISK = bd("0.05");

    private final MarketSentimentQueryRepository repository;

    public MarketSentimentQueryAppService(MarketSentimentQueryRepository repository) {
        this.repository = repository;
    }

    public MarketSentimentMaCountDTO getMaCount(LocalDate date) {
        return repository.findMaCount(servingDate(date)).map(this::maCount).orElseGet(() -> emptyMaCount(date));
    }

    public MarketSentimentMaCountDTO.HistoryResponseDTO getMaCountHistory(LocalDate start, LocalDate end) {
        List<MarketSentimentMaCountDTO.HistoryItemDTO> items = repository.listMaCount(start, end).stream()
                .map(this::maCountHistoryItem)
                .toList();
        return MarketSentimentMaCountDTO.HistoryResponseDTO.builder()
                .ok(true).start(start).end(end).count(items.size()).items(items).build();
    }

    public MarketSentimentRiskAppetiteDTO getRiskAppetite(LocalDate date) {
        return repository.findRiskAppetite(servingDate(date)).map(this::riskAppetite).orElseGet(() -> emptyRiskAppetite(date));
    }

    public MarketSentimentRiskAppetiteDTO.HistoryResponseDTO getRiskAppetiteHistory(LocalDate start, LocalDate end) {
        Map<LocalDate, BigDecimal> msiScores = repository.listMsiIndex(start, end).stream()
                .filter(row -> row.getTradeDate() != null)
                .collect(Collectors.toMap(MsiIndexDailyBO::getTradeDate, MsiIndexDailyBO::getRiskAppetiteScore, (left, right) -> left));
        List<MarketSentimentRiskAppetiteDTO.HistoryItemDTO> items = repository.listRiskAppetite(start, end).stream()
                .map(row -> riskHistoryItem(row, msiScores.get(row.getTradeDate())))
                .toList();
        return MarketSentimentRiskAppetiteDTO.HistoryResponseDTO.builder()
                .ok(true).start(start).end(end).count(items.size()).items(items).build();
    }

    public MarketSentimentLimitEmotionSummaryDTO getLimitEmotion(LocalDate date) {
        return repository.findLimitEmotion(servingDate(date)).map(this::limitEmotion).orElseGet(() -> emptyLimitEmotion(date));
    }

    public MarketSentimentLimitEmotionSummaryDTO.HistoryResponseDTO getLimitEmotionHistory(LocalDate start, LocalDate end) {
        List<MarketSentimentLimitEmotionSummaryDTO.HistoryItemDTO> items = repository.listLimitEmotion(start, end).stream()
                .map(this::limitEmotionHistoryItem)
                .toList();
        return MarketSentimentLimitEmotionSummaryDTO.HistoryResponseDTO.builder()
                .ok(true).start(start).end(end).count(items.size()).items(items).build();
    }

    public MarketSentimentVolatilityDTO getVolatility(LocalDate date) {
        return repository.findVolatility(servingDate(date)).map(this::volatility).orElseGet(() -> emptyVolatility(date));
    }

    public MarketSentimentVolatilityDTO.HistoryResponseDTO getVolatilityHistory(LocalDate start, LocalDate end) {
        List<MarketSentimentVolatilityDTO.HistoryItemDTO> items = repository.listVolatility(start, end).stream()
                .map(this::volatilityHistoryItem)
                .toList();
        return MarketSentimentVolatilityDTO.HistoryResponseDTO.builder()
                .ok(true).start(start).end(end).count(items.size()).items(items).build();
    }

    public MarketSentimentTurnoverDTO getTurnover(LocalDate date) {
        return repository.findTurnover(servingDate(date)).map(this::turnover).orElseGet(() -> emptyTurnover(date));
    }

    public MarketSentimentTurnoverDTO.HistoryResponseDTO getTurnoverHistory(LocalDate start, LocalDate end) {
        List<MarketSentimentTurnoverDTO.HistoryItemDTO> items = repository.listTurnover(start, end).stream()
                .map(this::turnoverHistoryItem)
                .toList();
        return MarketSentimentTurnoverDTO.HistoryResponseDTO.builder()
                .ok(true).start(start).end(end).count(items.size()).items(items).build();
    }

    public MarketSentimentStyleRiskDTO getStyleRisk(LocalDate date) {
        return repository.findStyleRisk(servingDate(date)).map(this::styleRisk).orElseGet(() -> emptyStyleRisk(date));
    }

    public MarketSentimentStyleRiskDTO.HistoryResponseDTO getStyleRiskHistory(LocalDate start, LocalDate end) {
        List<MarketSentimentStyleRiskDTO.HistoryItemDTO> items = repository.listStyleRisk(start, end).stream()
                .map(this::styleRiskHistoryItem)
                .toList();
        return MarketSentimentStyleRiskDTO.HistoryResponseDTO.builder()
                .ok(true).start(start).end(end).count(items.size()).items(items).build();
    }

    public MarketSentimentProfitEffectDTO getProfitEffect(LocalDate date) {
        return repository.findProfitEffect(servingDate(date)).map(this::profitEffect).orElseGet(() -> emptyProfitEffect(date));
    }

    public MarketSentimentProfitEffectDTO.HistoryResponseDTO getProfitEffectHistory(LocalDate start, LocalDate end) {
        List<MarketSentimentProfitEffectDTO.HistoryItemDTO> items = repository.listProfitEffect(start, end).stream()
                .map(this::profitHistoryItem)
                .toList();
        return MarketSentimentProfitEffectDTO.HistoryResponseDTO.builder()
                .ok(true).start(start).end(end).count(items.size()).items(items).build();
    }

    public MarketSentimentSectorBreadthDTO getSectorBreadth(LocalDate date) {
        return repository.findSectorBreadth(servingDate(date)).map(this::sectorBreadth).orElseGet(() -> emptySectorBreadth(date));
    }

    public MarketSentimentSectorBreadthDTO.HistoryResponseDTO getSectorBreadthHistory(int days, LocalDate end) {
        LocalDate endDate = servingDate(end);
        LocalDate startDate = endDate.minusDays(days * 2L);
        List<MarketSentimentSectorBreadthDTO.HistoryItemDTO> items = repository.listSectorBreadth(startDate, endDate).stream()
                .map(this::sectorBreadthHistoryItem)
                .toList();
        return MarketSentimentSectorBreadthDTO.HistoryResponseDTO.builder()
                .ok(true).start(startDate).end(endDate).days(days).count(items.size()).items(items).build();
    }

    public MarketSentimentMsiIndexDTO getMsiIndex(LocalDate date) {
        return repository.findMsiIndex(servingDate(date)).map(this::msiIndex).orElseGet(() -> emptyMsiIndex(date));
    }

    public MarketSentimentMsiIndexDTO.HistoryResponseDTO getMsiIndexHistory(LocalDate start, LocalDate end) {
        List<MarketSentimentMsiIndexDTO.HistoryItemDTO> items = repository.listMsiIndex(start, end).stream()
                .map(this::msiIndexHistoryItem)
                .toList();
        return MarketSentimentMsiIndexDTO.HistoryResponseDTO.builder()
                .ok(true).start(start).end(end).count(items.size()).items(items).build();
    }

    public MarketPulseOverviewHistoryDTO getOverviewHistory(LocalDate start, LocalDate end) {
        List<MarketPulseOverviewHistoryDTO.ItemDTO> items = repository.listOverview(start, end).stream()
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
        List<IndexDailyBarBO> rows = repository.listIndexDaily(code, start, end);
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
                .name(indexName(code))
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

    private MarketSentimentRiskAppetiteDTO riskAppetite(RiskAppetiteDailyBO row) {
        BigDecimal score = row.getScore() != null
                ? row.getScore()
                : repository.findMsiIndex(row.getTradeDate()).map(MsiIndexDailyBO::getRiskAppetiteScore).orElse(null);
        return MarketSentimentRiskAppetiteDTO.builder()
                .ok(true).tradeDate(row.getTradeDate()).windowDays(row.getWindowDays())
                .hs300(asset(row.getHs300Close(), row.getTradeDate(), row.getHs300BaseClose(), row.getHs300BaseDate(), row.getHs30020dReturn(), null))
                .treasury(MarketSentimentRiskAppetiteDTO.TreasuryDTO.builder()
                        .asset511010(asset(row.getTreasury511010Close(), row.getTradeDate(), row.getTreasury511010BaseClose(), null, row.getTreasury51101020dReturn(), bd("0.5")))
                        .asset511090(asset(row.getTreasury511090Close(), row.getTradeDate(), row.getTreasury511090BaseClose(), null, row.getTreasury51109020dReturn(), bd("0.5")))
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
        BigDecimal score = row.getScore() == null ? msiScore : row.getScore();
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

    private MarketSentimentProfitEffectDTO profitEffect(ProfitEffectDailyBO row) {
        return MarketSentimentProfitEffectDTO.builder()
                .ok(true).tradeDate(row.getTradeDate()).up5dPct(row.getUp5dPct()).newLow60dPct(row.getNewLow60dPct())
                .score(row.getScore()).rawValue(row.getScore()).elapsedMs(row.getElapsedMs()).source(row.getSource()).fromCache(true).build();
    }

    private MarketSentimentProfitEffectDTO.HistoryItemDTO profitHistoryItem(ProfitEffectDailyBO row) {
        return MarketSentimentProfitEffectDTO.HistoryItemDTO.builder()
                .tradeDate(row.getTradeDate()).score(row.getScore()).fromCache(true).build();
    }

    private MarketSentimentSectorBreadthDTO sectorBreadth(SectorBreadthDailyBO row) {
        BigDecimal score = row.getScore() == null && row.getAdvancePct() != null ? row.getAdvancePct().multiply(bd("100")) : row.getScore();
        return MarketSentimentSectorBreadthDTO.builder()
                .ok(true).tradeDate(row.getTradeDate()).advancing(n(row.getAdvancing())).declining(n(row.getDeclining()))
                .flat(n(row.getFlat())).total(n(row.getTotal())).advancePct(row.getAdvancePct()).score(score)
                .source(row.getSource()).elapsedMs(row.getElapsedMs()).fromCache(true).build();
    }

    private MarketSentimentSectorBreadthDTO.HistoryItemDTO sectorBreadthHistoryItem(SectorBreadthDailyBO row) {
        BigDecimal score = row.getScore() == null && row.getAdvancePct() != null ? row.getAdvancePct().multiply(bd("100")) : row.getScore();
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
                .vol(WEIGHT_VOL).turnover(WEIGHT_TURNOVER).priceStrength(WEIGHT_PRICE_STRENGTH)
                .riskAppetite(WEIGHT_RISK_APPETITE).breadth(WEIGHT_BREADTH).limitEmotion(WEIGHT_LIMIT_EMOTION)
                .profitEffect(WEIGHT_PROFIT_EFFECT).sectorBreadth(WEIGHT_SECTOR_BREADTH).styleRisk(WEIGHT_STYLE_RISK).build();
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

    private LocalDate servingDate(LocalDate date) {
        return date == null ? LocalDate.now() : date;
    }

    private int n(Integer value) {
        return value == null ? 0 : value;
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    private String indexName(String code) {
        return switch (code == null ? "" : code.replaceAll("\\D", "")) {
            case "000001" -> "上证指数";
            case "399001" -> "深证成指";
            case "399006" -> "创业板指";
            default -> code;
        };
    }
}
