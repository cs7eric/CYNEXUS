package cn.cccs7.cynexus.core.domain.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataSourceHealthCheckRunBO {

    private Long id;
    private String runId;
    private String triggerType;
    private String status;
    private OffsetDateTime startedAt;
    private OffsetDateTime finishedAt;
    private Integer durationMs;
    private Boolean includeSlow;
    private Integer totalCount;
    private Integer healthyCount;
    private Integer degradedCount;
    private Integer failedCount;
    private Integer skippedCount;
    private BigDecimal successRate;
    private BigDecimal avgLatencyMs;
    private String errorMessage;
    private String rawPayload;
    private String extra;
    private String remark;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime deletedAt;
}
