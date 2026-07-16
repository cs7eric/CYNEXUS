package cn.cccs7.cynexus.market.domain.sentiment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public interface MarketSentimentDomainService {

    LocalDate resolveServingTradeDate(LocalDate requestedDate);

    boolean matchesTradeDate(LocalDate expectedDate, LocalDate actualDate);

    BigDecimal resolveScore(BigDecimal directScore, BigDecimal fallbackScore);

    BigDecimal deriveSectorBreadthScore(BigDecimal score, BigDecimal advancePct);

    Map<String, BigDecimal> msiWeights();

    BigDecimal treasuryWeight();

    int count(Integer value);

    String indexName(String code);
}
