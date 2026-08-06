package cn.cccs7.cynexus.core.domain.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import cn.cccs7.cynexus.core.domain.entity.DataJobTriggerResultBO;
import cn.cccs7.cynexus.core.domain.entity.SchedulerJobBO;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SchedulerTriggerPolicyTest {

    @Test
    void resolvesMarketIndicatorRefreshForRecurringRuntime() {
        SchedulerDataJobKeyResolver.ResolvedDataJob dataJob = SchedulerDataJobKeyResolver
                .resolve("market_indicator_refresh")
                .orElseThrow();

        assertThat(dataJob.jobKey()).isEqualTo("market_indicator_refresh");
        assertThat(dataJob.defaultParams())
                .containsEntry("limit_history_days", 365)
                .containsEntry("batch_size", 80)
                .containsEntry("pool_size", 8);
    }

    @Test
    void resolvesDailyMarginSyncForRecurringRuntime() {
        SchedulerDataJobKeyResolver.ResolvedDataJob dataJob = SchedulerDataJobKeyResolver
                .resolve("margin_sync_daily")
                .orElseThrow();

        assertThat(dataJob.jobKey()).isEqualTo("sync_margin");
        assertThat(dataJob.defaultParams())
                .containsEntry("create_table", true)
                .containsEntry("providers", List.of("exchange", "akshare"))
                .containsEntry("provider_priority", List.of("exchange", "akshare"))
                .containsEntry("datasets", List.of("summary", "detail", "account"))
                .containsEntry("lock_scope", "margin");
    }

    @Test
    void resolvesMarginHistoryBackfillForWholeActiveUniverse() {
        SchedulerDataJobKeyResolver.ResolvedDataJob dataJob = SchedulerDataJobKeyResolver
                .resolve("margin_stock_history_backfill")
                .orElseThrow();

        assertThat(dataJob.jobKey()).isEqualTo("sync_margin");
        assertThat(dataJob.defaultParams())
                .containsEntry("mode", "stock_history")
                .containsEntry("code_source", "all")
                .containsEntry("limit_codes", 0)
                .containsEntry("history_days", 120)
                .containsEntry("page_size", 120)
                .containsEntry("workers", 8)
                .containsEntry("request_interval", 0.35);
    }

    @Test
    void doesNotForceScheduledDateForMarginSync() {
        SchedulerDataJobKeyResolver.ResolvedDataJob dataJob = SchedulerDataJobKeyResolver
                .resolve("margin_sync_daily")
                .orElseThrow();

        Map<String, Object> params = SchedulerTriggerPolicy.triggerParams(dataJob, "2026-08-04");

        assertThat(params).doesNotContainKeys("target_date", "trade_date", "snapshot_date");
    }

    @Test
    void doesNotForceScheduledDateForFuturesPositionSync() {
        SchedulerDataJobKeyResolver.ResolvedDataJob dataJob =
                new SchedulerDataJobKeyResolver.ResolvedDataJob(
                        "sync_futures_position", Map.of("latest_source_data", true));

        Map<String, Object> params = SchedulerTriggerPolicy.triggerParams(dataJob, "2026-08-04");

        assertThat(params).containsEntry("latest_source_data", true)
                .doesNotContainKeys("target_date", "trade_date", "snapshot_date");
    }

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
