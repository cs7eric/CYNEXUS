package cn.cccs7.cynexus.ai.infra.ai.langchain4j;

import dev.langchain4j.service.UserMessage;
import com.fasterxml.jackson.databind.JsonNode;

/** Capability-specific typed boundary for event and daily-brief JSON. */
public interface MarketInformationAnalyzer {
    @UserMessage("{{text}}")
    MarketInformationOutput analyze(String text);

    record MarketInformationOutput(JsonNode events, JsonNode sourceCoverage, JsonNode warnings,
                                   JsonNode summary, JsonNode dailyBrief, JsonNode metadata) { }
}
