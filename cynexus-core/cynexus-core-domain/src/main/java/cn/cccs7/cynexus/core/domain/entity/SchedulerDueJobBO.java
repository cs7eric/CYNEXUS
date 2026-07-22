package cn.cccs7.cynexus.core.domain.entity;

import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Value;

/** A scheduler job that is eligible for execution at a specific tick. */
@Value
@Builder
public class SchedulerDueJobBO {

    SchedulerJobBO job;
    SchedulerJobStatusBO status;
    OffsetDateTime evaluatedAt;

    public SchedulerDueJobBO(SchedulerJobBO job, SchedulerJobStatusBO status, OffsetDateTime evaluatedAt) {
        this.job = job;
        this.status = status;
        this.evaluatedAt = evaluatedAt;
    }
}
