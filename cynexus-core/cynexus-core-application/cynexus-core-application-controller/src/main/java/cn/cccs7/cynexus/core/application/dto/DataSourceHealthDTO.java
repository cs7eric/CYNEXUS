package cn.cccs7.cynexus.core.application.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataSourceHealthDTO {

    private Boolean ok;
    private OffsetDateTime generatedAt;
    private Integer durationMs;
    private Boolean includeSlow;
    private Summary summary;
    private Run run;
    private List<Item> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Summary {
        private Integer total;
        private Integer healthy;
        private Integer degraded;
        private Integer failed;
        private Integer skipped;
        private BigDecimal successRate;
        private BigDecimal avgLatencyMs;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Run {
        private String runId;
        private String triggerType;
        private String status;
        private OffsetDateTime startedAt;
        private OffsetDateTime finishedAt;
        private Integer durationMs;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        private String sourceCode;
        private String sourceName;
        private String sourceType;
        private String category;
        private String provider;
        private String description;
        private String endpoint;
        private String status;
        private Boolean connectivity;
        private String authenticityStatus;
        private Integer latencyMs;
        private Integer sampleCount;
        private BigDecimal successRate24h;
        private OffsetDateTime checkedAt;
        private String errorMessage;
        private String checks;
        private String rawPayload;
    }
}
