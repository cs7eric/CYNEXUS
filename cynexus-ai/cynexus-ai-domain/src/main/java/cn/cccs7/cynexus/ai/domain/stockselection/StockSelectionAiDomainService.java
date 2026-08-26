package cn.cccs7.cynexus.ai.domain.stockselection;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Map;

public interface StockSelectionAiDomainService {

    String CAPABILITY = "stock_selection_ai_screening";
    String PROMPT_KEY = "stock_selection_ai_screening";
    String INTENT_PLANNING_CAPABILITY = "stock_selection_ai_intent_planning";
    String INTENT_PLANNING_PROMPT_KEY = "stock_selection_ai_intent_planning";

    AnalysisResult analyze(AnalysisCommand command);

    IntentPlan parseIntent(IntentCommand command);

    record AnalysisCommand(String researchIntent, Integer maxCandidates, Map<String, Object> criteria) {
    }

    record AnalysisResult(String tradeDate,
                          String source,
                          int universeCount,
                          int candidateCount,
                          List<String> warnings,
                          ObjectNode analysis,
                          String providerCode,
                          String modelCode,
                          String promptVersion) {
    }

    record IntentCommand(String researchIntent, Map<String, Object> baseCriteria) {
    }

    record IntentPlan(String normalizedIntent,
                      Map<String, Object> criteria,
                      List<ProposedCondition> proposedConditions,
                      List<String> appliedRules,
                      List<String> assumptions,
                      List<String> unsupportedPhrases,
                      List<String> warnings,
                      String providerCode,
                      String modelCode,
                      String promptVersion) {
    }

    /**
     * Preserves a valid screening idea even when the current screener cannot
     * execute it yet. Only {@code criteria} is sent to the trusted executor.
     */
    record ProposedCondition(String category,
                             String sourcePhrase,
                             String label,
                             String definition,
                             String executionStatus,
                             List<String> dataRequirements) {
    }
}
