package cn.cccs7.cynexus.ai.api.request;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public final class StockSelectionAiRequest {

    private StockSelectionAiRequest() {
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnalyzeRequest {
        private String researchIntent;
        private Integer maxCandidates;
        private Map<String, Object> criteria;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IntentPlanRequest {
        private String researchIntent;
        private Map<String, Object> criteria;
    }
}
