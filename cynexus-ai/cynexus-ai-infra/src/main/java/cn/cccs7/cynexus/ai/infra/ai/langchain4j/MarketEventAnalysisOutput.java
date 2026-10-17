package cn.cccs7.cynexus.ai.infra.ai.langchain4j;

import java.util.List;
import com.fasterxml.jackson.databind.JsonNode;

/** Typed transport shape; domain policy still validates evidence references and enums. */
public record MarketEventAnalysisOutput(JsonNode factor_analysis) {
}
