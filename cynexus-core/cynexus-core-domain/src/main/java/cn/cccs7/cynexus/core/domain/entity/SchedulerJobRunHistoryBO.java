package cn.cccs7.cynexus.core.domain.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record SchedulerJobRunHistoryBO(
        Long id,
        Long jobId,
        OffsetDateTime startedAt,
        OffsetDateTime endedAt,
        String triggerType,
        String status,
        String errorMessage,
        BigDecimal durationSeconds,
        String remark
) {
}
