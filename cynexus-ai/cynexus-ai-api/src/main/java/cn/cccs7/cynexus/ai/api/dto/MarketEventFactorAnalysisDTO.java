package cn.cccs7.cynexus.ai.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRawValue;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public final class MarketEventFactorAnalysisDTO {
    private MarketEventFactorAnalysisDTO() {
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnalysisResponse {
        private boolean ok;
        @JsonProperty("trade_date")
        private String tradeDate;
        private String cohort;
        @JsonProperty("min_large_move_pct")
        private BigDecimal minLargeMovePct;
        private String source;
        @JsonProperty("population_count")
        private int populationCount;
        @JsonProperty("sampled_count")
        private int sampledCount;
        @JsonProperty("data_quality")
        private String dataQuality;
        private List<String> warnings;
        @JsonProperty("evidence_refs")
        private Map<String, String> evidenceRefs;
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
}
