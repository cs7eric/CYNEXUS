package cn.cccs7.cynexus.ai.domain.marketfactor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Bounded factual evidence supplied by the Market module before any model call. */
public interface MarketEventFactorEvidencePort {
    EvidenceSnapshot load(String tradeDate, String cohort, BigDecimal minLargeMovePct, int maxEvents, String symbol);

    record EvidenceSnapshot(String requestedTradeDate,
                            String tradeDate,
                            String cohort,
                            BigDecimal minLargeMovePct,
                            int populationCount,
                            int sampledCount,
                            String source,
                            Map<String, String> evidenceRefs,
                            Map<String, Object> modelInput,
                            List<String> warnings,
                            String dataQuality) {
    }
}
