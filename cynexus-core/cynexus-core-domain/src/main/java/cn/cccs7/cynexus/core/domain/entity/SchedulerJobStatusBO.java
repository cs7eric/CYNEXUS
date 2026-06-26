package cn.cccs7.cynexus.core.domain.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record SchedulerJobStatusBO(
        Long id,
        Long jobId,
        Boolean enabled,
        Boolean running,
        String schedule,
        Integer timezoneOffsetHours,
        Integer tickSeconds,
        OffsetDateTime lastRunAt,
        Boolean lastRunOk,
        String lastRunStatus,
        String lastRunErrorMessage,
        BigDecimal lastDurationSeconds,
        Integer lastTargetsProcessed,
        Integer totalRuns,
        Integer totalFailures,
        OffsetDateTime schedulerStartedAt,
        OffsetDateTime stoppedAt,
        String extra,
        String remark
) {
}
