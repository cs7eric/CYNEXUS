package cn.cccs7.cynexus.market.domain.sentiment.impl;

import cn.cccs7.cynexus.market.domain.entity.MsiIndexDailyBO;
import cn.cccs7.cynexus.market.domain.repository.MarketSentimentQueryRepository;
import cn.cccs7.cynexus.market.domain.sentiment.MarketSentimentDomainService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class MarketSentimentDomainServiceImpl implements MarketSentimentDomainService {

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

    public MarketSentimentDomainServiceImpl(MarketSentimentQueryRepository repository) {
        this.repository = repository;
    }

    @Override
    public LocalDate resolveServingTradeDate(LocalDate requestedDate) {
        LocalDate requested = requestedDate == null ? LocalDate.now() : requestedDate;
        return repository.findMsiIndex(requested)
                .map(MsiIndexDailyBO::getTradeDate)
                .orElse(requested);
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
        return weights;
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
}
