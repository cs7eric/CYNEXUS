package cn.cccs7.cynexus.ai.domain.marketfactor;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface MarketEventFactorAnalysisDomainService {
    String CAPABILITY = "market_event_factor_analysis";
    String PROMPT_KEY = "market_event_factor_analysis";

    AnalysisResult analyze(AnalysisCommand command);

    record AnalysisCommand(String tradeDate, String cohort, BigDecimal minLargeMovePct, Integer maxEvents, String symbol) {
    }

    record AnalysisResult(String tradeDate,
                          String cohort,
                          BigDecimal minLargeMovePct,
                          String source,
                          int populationCount,
                          int sampledCount,
                          String dataQuality,
                          List<String> warnings,
                          Map<String, String> evidenceRefs,
                          ObjectNode analysis,
                          String providerCode,
                          String modelCode,
                          String promptVersion) {
    }
}
