package cn.cccs7.cynexus.market.domain.service.sentiment.impl;

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
import cn.cccs7.cynexus.market.domain.sentiment.MarketSentimentDomainService;
import cn.cccs7.cynexus.market.domain.service.sentiment.MarketSentimentQueryDomainService;
import cn.cccs7.cynexus.market.domain.valueobject.sentiment.MarketSentimentHistoryResult;
import cn.cccs7.cynexus.market.domain.valueobject.sentiment.MarketSentimentIndexDailyResult;
import cn.cccs7.cynexus.market.domain.valueobject.sentiment.MarketSentimentQueryResult;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/** Pure domain implementation; Spring configuration belongs outside domain. */
public class MarketSentimentQueryDomainServiceImpl implements MarketSentimentQueryDomainService {

    private static final BigDecimal WEIGHT_VOL = new BigDecimal("0.15");
    private static final BigDecimal WEIGHT_TURNOVER = new BigDecimal("0.15");
    private static final BigDecimal WEIGHT_PRICE_STRENGTH = new BigDecimal("0.10");
    private static final BigDecimal WEIGHT_RISK_APPETITE = new BigDecimal("0.10");
    private static final BigDecimal WEIGHT_BREADTH = new BigDecimal("0.15");
    private static final BigDecimal WEIGHT_LIMIT_EMOTION = new BigDecimal("0.15");
    private static final BigDecimal WEIGHT_PROFIT_EFFECT = new BigDecimal("0.10");
    private static final BigDecimal WEIGHT_SECTOR_BREADTH = new BigDecimal("0.05");
    private static final BigDecimal WEIGHT_STYLE_RISK = new BigDecimal("0.05");
    private static final BigDecimal TREASURY_WEIGHT = new BigDecimal("0.5");

    private final MarketSentimentQueryRepository repository;

    public MarketSentimentQueryDomainServiceImpl(MarketSentimentQueryRepository repository) {
        this.repository = repository;
    }

    @Override
    public MarketSentimentQueryResult<MaCountDailyBO> maCount(LocalDate date) {
        LocalDate effectiveDate = resolveServingTradeDate(date);
        return result(date, effectiveDate, repository.findMaCount(effectiveDate)
                .filter(row -> matchesTradeDate(effectiveDate, row.getTradeDate())), ignored -> null);
    }

    @Override
    public MarketSentimentHistoryResult<MaCountDailyBO> maCountHistory(LocalDate start, LocalDate end) {
        return history(start, end, repository.listMaCount(start, end), row -> null, "java-pg");
    }

    @Override
    public MarketSentimentQueryResult<RiskAppetiteDailyBO> riskAppetite(LocalDate date) {
        LocalDate effectiveDate = resolveServingTradeDate(date);
        Optional<RiskAppetiteDailyBO> value = repository.findRiskAppetite(effectiveDate)
                .filter(row -> matchesTradeDate(effectiveDate, row.getTradeDate()));
        BigDecimal fallback = value.flatMap(row -> repository.findMsiIndex(row.getTradeDate()))
                .map(MsiIndexDailyBO::getRiskAppetiteScore).orElse(null);
        return result(date, effectiveDate, value, row -> resolveScore(row.getScore(), fallback));
    }

    @Override
    public MarketSentimentHistoryResult<RiskAppetiteDailyBO> riskAppetiteHistory(LocalDate start, LocalDate end) {
        Map<LocalDate, BigDecimal> scores = msiScores(start, end, MsiIndexDailyBO::getRiskAppetiteScore);
        return history(start, end, repository.listRiskAppetite(start, end),
                row -> resolveScore(row.getScore(), scores.get(row.getTradeDate())), "java-pg");
    }

    @Override
    public MarketSentimentQueryResult<MarketSentimentLimitEmotionDailyBO> limitEmotion(LocalDate date) {
        LocalDate effectiveDate = resolveServingTradeDate(date);
        return result(date, effectiveDate, repository.findLimitEmotion(effectiveDate)
                .filter(row -> matchesTradeDate(effectiveDate, row.getTradeDate())), ignored -> null);
    }

    @Override
    public MarketSentimentHistoryResult<MarketSentimentLimitEmotionDailyBO> limitEmotionHistory(LocalDate start, LocalDate end) {
        return history(start, end, repository.listLimitEmotion(start, end), row -> null, "java-pg");
    }

    @Override
    public MarketSentimentQueryResult<VolatilityDailyBO> volatility(LocalDate date) {
        LocalDate effectiveDate = resolveServingTradeDate(date);
        return result(date, effectiveDate, repository.findVolatility(effectiveDate)
                .filter(row -> matchesTradeDate(effectiveDate, row.getTradeDate())), ignored -> null);
    }

    @Override
    public MarketSentimentHistoryResult<VolatilityDailyBO> volatilityHistory(LocalDate start, LocalDate end) {
        return history(start, end, repository.listVolatility(start, end), row -> null, "java-pg");
    }

    @Override
    public MarketSentimentQueryResult<TurnoverActivityDailyBO> turnover(LocalDate date) {
        LocalDate effectiveDate = resolveServingTradeDate(date);
        return result(date, effectiveDate, repository.findTurnover(effectiveDate)
                .filter(row -> matchesTradeDate(effectiveDate, row.getTradeDate())), ignored -> null);
    }

    @Override
    public MarketSentimentHistoryResult<TurnoverActivityDailyBO> turnoverHistory(LocalDate start, LocalDate end) {
        return history(start, end, repository.listTurnover(start, end), row -> null, "java-pg");
    }

    @Override
    public MarketSentimentQueryResult<StyleRiskDailyBO> styleRisk(LocalDate date) {
        LocalDate effectiveDate = resolveServingTradeDate(date);
        return result(date, effectiveDate, repository.findStyleRisk(effectiveDate)
                .filter(row -> matchesTradeDate(effectiveDate, row.getTradeDate())), ignored -> null);
    }

    @Override
    public MarketSentimentHistoryResult<StyleRiskDailyBO> styleRiskHistory(LocalDate start, LocalDate end) {
        return history(start, end, repository.listStyleRisk(start, end), row -> null, "java-pg");
    }

    @Override
    public MarketSentimentQueryResult<ProfitEffectDailyBO> profitEffect(LocalDate date) {
        LocalDate effectiveDate = resolveServingTradeDate(date);
        Optional<ProfitEffectDailyBO> value = repository.findProfitEffect(effectiveDate)
                .filter(row -> matchesTradeDate(effectiveDate, row.getTradeDate()));
        BigDecimal resolvedScore = value.flatMap(row -> repository.findMsiIndex(row.getTradeDate()))
                .map(MsiIndexDailyBO::getProfitEffectScore)
                .orElseGet(() -> value.map(ProfitEffectDailyBO::getScore).orElse(null));
        return result(date, effectiveDate, value, ignored -> resolvedScore);
    }

    @Override
    public MarketSentimentHistoryResult<ProfitEffectDailyBO> profitEffectHistory(LocalDate start, LocalDate end) {
        Map<LocalDate, BigDecimal> scores = msiScores(start, end, MsiIndexDailyBO::getProfitEffectScore);
        return history(start, end, repository.listProfitEffect(start, end),
                row -> resolveScore(scores.get(row.getTradeDate()), row.getScore()), "java-pg");
    }

    @Override
    public MarketSentimentQueryResult<SectorBreadthDailyBO> sectorBreadth(LocalDate date) {
        LocalDate effectiveDate = resolveServingTradeDate(date);
        Optional<SectorBreadthDailyBO> value = repository.findSectorBreadth(effectiveDate)
                .filter(row -> matchesTradeDate(effectiveDate, row.getTradeDate()));
        return result(date, effectiveDate, value,
                row -> deriveSectorBreadthScore(row.getScore(), row.getAdvancePct()));
    }

    @Override
    public MarketSentimentHistoryResult<SectorBreadthDailyBO> sectorBreadthHistory(int days, LocalDate end) {
        LocalDate endDate = resolveServingTradeDate(end);
        LocalDate startDate = endDate.minusDays(days * 2L);
        return history(startDate, endDate, repository.listSectorBreadth(startDate, endDate),
                row -> deriveSectorBreadthScore(row.getScore(), row.getAdvancePct()), "java-pg");
    }

    @Override
    public MarketSentimentQueryResult<MsiIndexDailyBO> msiIndex(LocalDate date) {
        LocalDate effectiveDate = resolveServingTradeDate(date);
        return result(date, effectiveDate, repository.findMsiIndex(effectiveDate)
                .filter(row -> matchesTradeDate(effectiveDate, row.getTradeDate())), ignored -> null);
    }

    @Override
    public MarketSentimentHistoryResult<MsiIndexDailyBO> msiIndexHistory(LocalDate start, LocalDate end) {
        return history(start, end, repository.listMsiIndex(start, end), row -> null, "java-pg");
    }

    @Override
    public MarketSentimentHistoryResult<MarketOverviewBO> overviewHistory(LocalDate start, LocalDate end) {
        return history(start, end, repository.listOverview(start, end), row -> null, "java-pg");
    }

    @Override
    public MarketSentimentIndexDailyResult indexDaily(String code, LocalDate start, LocalDate end) {
        return new MarketSentimentIndexDailyResult(code, indexName(code), start, end,
                repository.listIndexDaily(code, start, end));
    }

    @Override
    public LocalDate resolveServingTradeDate(LocalDate requestedDate) {
        LocalDate requested = requestedDate == null ? LocalDate.now() : requestedDate;
        return repository.findMsiIndex(requested).map(MsiIndexDailyBO::getTradeDate).orElse(requested);
    }

    @Override
    public boolean matchesTradeDate(LocalDate expectedDate, LocalDate actualDate) {
        return expectedDate != null && expectedDate.equals(actualDate);
    }

    @Override
    public BigDecimal resolveScore(BigDecimal directScore, BigDecimal fallbackScore) {
        return directScore == null ? fallbackScore : directScore;
    }

    @Override
    public BigDecimal deriveSectorBreadthScore(BigDecimal score, BigDecimal advancePct) {
        return score == null && advancePct != null ? advancePct.multiply(BigDecimal.TEN.pow(2)) : score;
    }

    @Override
    public Map<String, BigDecimal> msiWeights() {
        Map<String, BigDecimal> weights = new LinkedHashMap<>();
        weights.put("vol", WEIGHT_VOL);
        weights.put("turnover", WEIGHT_TURNOVER);
        weights.put("priceStrength", WEIGHT_PRICE_STRENGTH);
        weights.put("riskAppetite", WEIGHT_RISK_APPETITE);
        weights.put("breadth", WEIGHT_BREADTH);
        weights.put("limitEmotion", WEIGHT_LIMIT_EMOTION);
        weights.put("profitEffect", WEIGHT_PROFIT_EFFECT);
        weights.put("sectorBreadth", WEIGHT_SECTOR_BREADTH);
        weights.put("styleRisk", WEIGHT_STYLE_RISK);
        return Map.copyOf(weights);
    }

    @Override
    public BigDecimal treasuryWeight() {
        return TREASURY_WEIGHT;
    }

    @Override
    public int count(Integer value) {
        return value == null ? 0 : value;
    }

    @Override
    public String indexName(String code) {
        return switch (code == null ? "" : code.replaceAll("\\D", "")) {
            case "000001" -> "上证指数";
            case "399001" -> "深证成指";
            case "399006" -> "创业板指";
            default -> code;
        };
    }

    private <T> MarketSentimentQueryResult<T> result(LocalDate requestedDate, LocalDate effectiveDate,
                                                      Optional<T> value, Function<T, BigDecimal> score) {
        return new MarketSentimentQueryResult<>(requestedDate, effectiveDate, value.orElse(null),
                value.map(score).orElse(null));
    }

    private <T> MarketSentimentHistoryResult<T> history(LocalDate start, LocalDate end, List<T> rows,
                                                        Function<T, BigDecimal> score, String source) {
        List<MarketSentimentHistoryResult.Item<T>> items = (rows == null ? List.<T>of() : rows).stream()
                .map(row -> new MarketSentimentHistoryResult.Item<>(row, score.apply(row)))
                .toList();
        return new MarketSentimentHistoryResult<>(start, end, items, source);
    }

    private Map<LocalDate, BigDecimal> msiScores(LocalDate start, LocalDate end,
                                                   Function<MsiIndexDailyBO, BigDecimal> selector) {
        Map<LocalDate, BigDecimal> scores = new LinkedHashMap<>();
        for (MsiIndexDailyBO row : repository.listMsiIndex(start, end)) {
            if (row.getTradeDate() != null) {
                scores.putIfAbsent(row.getTradeDate(), selector.apply(row));
            }
        }
        return scores;
    }
}
