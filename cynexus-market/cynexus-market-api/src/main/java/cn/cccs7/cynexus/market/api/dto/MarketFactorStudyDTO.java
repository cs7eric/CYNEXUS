package cn.cccs7.cynexus.market.api.dto;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class MarketFactorStudyDTO {
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CreateRequest {
        private String eventType;
        private Integer cohortDefinitionVersion;
        private String startDate;
        private String endDate;
        private List<Integer> outcomeHorizons;
        private Integer controlsPerEvent;
        private Integer dailyPreWindowSessions;
        private Integer minutePreWindowMinutes;
        private List<String> discoveryMethods;
        private Map<String, Object> eventParams;
        private String dataCutoffAt;
        private String featureRegistryVersion;
        private String idempotencyKey;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Response {
        private String studyId;
        private String status;
        private String eventType;
        private String startDate;
        private String endDate;
        private String dataCutoffAt;
        private String featureRegistryVersion;
        private Boolean leakageCheckPassed;
        private Integer eventSampleCount;
        private Integer controlSampleCount;
        private Map<String, Object> progress;
        private List<String> warnings;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SampleResponse {
        private String code;
        private String market;
        private String eventTradeDate;
        private String anchorAt;
        private String eventType;
        private String sampleType;
        private Map<String, Object> matchingMetadata;
        private Map<String, Object> source;
        private Map<String, Object> coverage;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SequencePointResponse {
        private String code;
        private String market;
        private String eventTradeDate;
        private String sampleType;
        private String frequency;
        private String sequenceCode;
        private Integer offset;
        private String observedAt;
        private String availableAt;
        private java.math.BigDecimal rawValue;
        private java.math.BigDecimal normalizedValue;
        private Map<String, Object> source;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class FactorResultResponse {
        private String discoveryCode;
        private Integer recipeVersion;
        private String method;
        private String scope;
        private Map<String, Object> recipe;
        private Map<String, Object> learnedBounds;
        private Map<String, Object> statistics;
        private Map<String, Object> validation;
        private Map<String, Object> evidence;
        private String verdict;
        private Integer rank;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class OutcomeResponse {
        private String code;
        private String market;
        private String eventTradeDate;
        private String sampleType;
        private List<Map<String, Object>> outcomes;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ClusterResponse {
        private String clusterCode;
        private String method;
        private Integer memberCount;
        private List<String> discoveryCodes;
    }
}
