package cn.cccs7.cynexus.ai.domain.futuresposition;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Map;

public interface FuturesPositionAiDomainService {

    String CAPABILITY = "market.futures_position_analysis";
    String PROMPT_KEY = "market_futures_position_analysis";

    AnalysisResult analyze(AnalysisCommand command);

    record AnalysisCommand(String varietyCode,
                           String instrumentCode,
                           String instrumentName,
                           String exchangeCode,
                           String tradeDate,
                           Integer top,
                           Map<String, Object> current,
                           List<Map<String, Object>> history,
                           List<Map<String, Object>> members) {
    }

    record AnalysisResult(String varietyCode,
                          String instrumentCode,
                          String tradeDate,
                          ObjectNode analysis,
                          String providerCode,
                          String modelCode,
                          String promptVersion) {
    }
}
