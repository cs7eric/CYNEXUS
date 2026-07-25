package cn.cccs7.cynexus.core.domain.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import cn.cccs7.cynexus.core.domain.entity.DataJobTriggerResultBO;
import cn.cccs7.cynexus.core.domain.entity.SchedulerJobBO;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SchedulerTriggerPolicyTest {

    @Test
    void resolvesConfiguredDataJobAndAddsDateAliases() {
        SchedulerJobBO job = SchedulerJobBO.builder()
                .code("custom_scheduler_job")
                .extra("{\"data_job_key\":\"sync_custom\",\"default_params\":{\"limit\":20}}")
                .build();

        SchedulerDataJobKeyResolver.ResolvedDataJob dataJob = SchedulerTriggerPolicy.resolveDataJob(job).orElseThrow();
        Map<String, Object> params = SchedulerTriggerPolicy.triggerParams(dataJob, "2026-07-25");

        assertThat(dataJob.jobKey()).isEqualTo("sync_custom");
        assertThat(params).containsEntry("limit", 20)
                .containsEntry("target_date", "2026-07-25")
                .containsEntry("trade_date", "2026-07-25")
                .containsEntry("snapshot_date", "2026-07-25");
    }

    @Test
    void buildsResponseFactsFromExecutorItems() {
        DataJobTriggerResultBO result = DataJobTriggerResultBO.builder()
                .accepted(true)
                .runId("run-1")
                .jobKey("sync_custom")
                .status("success")
                .result(Map.of("items", List.of(
                        Map.of("jobId", "a", "ok", true, "lastStatus", "success"),
                        Map.of("jobId", "b", "ok", false, "lastStatus", "failed", "last_error", "timeout"))))
                .build();

        var outcome = SchedulerTriggerPolicy.completed("custom_scheduler_job", result);

        assertThat(outcome.ok()).isTrue();
        assertThat(outcome.count()).isEqualTo(2);
        assertThat(outcome.failedCount()).isEqualTo(1);
        assertThat(outcome.items()).extracting(item -> item.jobId()).containsExactly("a", "b");
        assertThat(outcome.items().get(1).lastRunError()).isEqualTo("timeout");
    }
}
