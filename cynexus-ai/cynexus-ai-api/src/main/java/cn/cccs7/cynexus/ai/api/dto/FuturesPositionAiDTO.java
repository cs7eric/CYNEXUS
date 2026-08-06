package cn.cccs7.cynexus.ai.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRawValue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public final class FuturesPositionAiDTO {

    private FuturesPositionAiDTO() {
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnalysisResponse {
        private boolean ok;
        @JsonProperty("variety_code")
        private String varietyCode;
        @JsonProperty("instrument_code")
        private String instrumentCode;
        @JsonProperty("trade_date")
        private String tradeDate;
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
