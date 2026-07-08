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
public class DataSourceHealthCheckItemBO {

    private Long id;
    private String runId;
    private String sourceCode;
    private String sourceName;
    private String sourceType;
    private String category;
    private String provider;
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
    private String extra;
    private String remark;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime deletedAt;
}
