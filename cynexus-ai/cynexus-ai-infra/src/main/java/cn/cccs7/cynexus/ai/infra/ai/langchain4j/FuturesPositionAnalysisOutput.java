package cn.cccs7.cynexus.ai.infra.ai.langchain4j;

import java.util.List;

/** Typed transport shape for futures position analysis. */
public record FuturesPositionAnalysisOutput(AnalysisResult analysis_result) {
    public record AnalysisResult(String headline, String structure, List<String> evidence, String trend,
                                 String risk, List<String> watch, String data_quality, String disclaimer) {
    }
}
