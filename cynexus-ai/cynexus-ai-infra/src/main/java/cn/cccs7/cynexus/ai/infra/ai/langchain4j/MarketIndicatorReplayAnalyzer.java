package cn.cccs7.cynexus.ai.infra.ai.langchain4j;

import dev.langchain4j.service.UserMessage;
import com.fasterxml.jackson.databind.JsonNode;

/** Typed structured boundary for market-indicator replay explanations. */
public interface MarketIndicatorReplayAnalyzer {
    @UserMessage("{{text}}")
    MarketIndicatorReplayOutput analyze(String text);

    record MarketIndicatorReplayOutput(JsonNode summary, JsonNode drivers, JsonNode counterEvidence,
                                       JsonNode failureConditions, JsonNode uncertainties,
                                       JsonNode evidenceRefs, JsonNode disclaimer) { }
}
