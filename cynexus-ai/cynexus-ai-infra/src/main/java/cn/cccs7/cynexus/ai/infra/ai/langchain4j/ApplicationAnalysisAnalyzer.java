package cn.cccs7.cynexus.ai.infra.ai.langchain4j;

import dev.langchain4j.service.UserMessage;
import com.fasterxml.jackson.databind.JsonNode;

/** Typed structured boundary for the application-analysis snapshot capability. */
public interface ApplicationAnalysisAnalyzer {
    @UserMessage("{{text}}")
    ApplicationAnalysisOutput analyze(String text);

    record ApplicationAnalysisOutput(JsonNode analysis_result) { }
}
