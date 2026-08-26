package cn.cccs7.cynexus.core.domain.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import cn.cccs7.cynexus.core.domain.entity.SchedulerJobBO;
import org.junit.jupiter.api.Test;

class SchedulerExecutionCapabilityPolicyTest {

    @Test
    void supportsJobConfiguredWithDataJobKeyInExtra() {
        SchedulerJobBO job = SchedulerJobBO.builder()
                .code("trade_review_timeshare_close")
                .extra("{\"data_job_key\":\"sync_trade_review_timeshare\"}")
                .build();

        assertThat(SchedulerExecutionCapabilityPolicy.supportsDataJob(job)).isTrue();
    }
}
