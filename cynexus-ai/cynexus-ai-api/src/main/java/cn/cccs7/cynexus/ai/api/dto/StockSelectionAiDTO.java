package cn.cccs7.cynexus.ai.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRawValue;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public final class StockSelectionAiDTO {

    private StockSelectionAiDTO() {
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnalysisResponse {
        private boolean ok;
        @JsonProperty("trade_date")
        private String tradeDate;
        private String source;
        @JsonProperty("universe_count")
        private int universeCount;
        @JsonProperty("candidate_count")
        private int candidateCount;
        private List<String> warnings;
        @JsonProperty("analysis_result")
        @JsonRawValue
        private String analysisResultJson;
        @JsonProperty("provider_code")
        private String providerCode;
        @JsonProperty("model_code")
        private String modelCode;
        @JsonProperty("prompt_version")
        private String promptVersion;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IntentPlanResponse {
        private String normalizedIntent;
        private Map<String, Object> criteria;
        private List<ProposedCondition> proposedConditions;
        private List<String> appliedRules;
        private List<String> assumptions;
        private List<String> unsupportedPhrases;
        private List<String> warnings;
        private String providerCode;
        private String modelCode;
        private String promptVersion;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProposedCondition {
        private String category;
        private String sourcePhrase;
        private String label;
        private String definition;
        private String executionStatus;
        private List<String> dataRequirements;
    }
}
