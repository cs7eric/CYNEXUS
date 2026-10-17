package cn.cccs7.cynexus.ai.infra.ai.langchain4j;

import dev.langchain4j.service.UserMessage;
import com.fasterxml.jackson.databind.JsonNode;

/** Typed structured boundary for screening and intent-planning responses. */
public interface StockSelectionAnalyzer {
    @UserMessage("{{text}}")
    StockSelectionOutput analyze(String text);

    record StockSelectionOutput(JsonNode screening_result) { }
}
