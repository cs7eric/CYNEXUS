package cn.cccs7.cynexus.ai.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRawValue;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

public final class MarketFactorStudySynthesisDTO {
    private MarketFactorStudySynthesisDTO() { }

    @Data @Builder
    public static class Response {
        private boolean ok;
        private String studyId;
        private String studyStatus;
        private String dataCutoffAt;
        private String leakageCheckStatus;
        private List<String> warnings;
        @JsonProperty("evidence_refs") private Map<String, String> evidenceRefs;
        @JsonProperty("synthesis_result") @JsonRawValue private String synthesisResultJson;
        private String providerCode;
        private String modelCode;
        private String promptVersion;
    }
}
