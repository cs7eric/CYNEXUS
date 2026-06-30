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
public class SchedulerJobRunHistoryBO {

    private Long id;
    private Long jobId;
    private OffsetDateTime startedAt;
    private OffsetDateTime endedAt;
    private String triggerType;
    private String status;
    private String errorMessage;
    private BigDecimal durationSeconds;
    private String remark;
    private String resultSummary;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime deletedAt;
}
