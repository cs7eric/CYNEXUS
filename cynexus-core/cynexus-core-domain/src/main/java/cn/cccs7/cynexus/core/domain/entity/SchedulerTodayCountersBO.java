package cn.cccs7.cynexus.core.domain.entity;

import java.time.OffsetDateTime;
import lombok.Data;

@Data
public class SchedulerTodayCountersBO {

    private int actual;
    private int success;
    private int failed;
    private int skipped;
    private int manual;
    private int auto;
    private OffsetDateTime failedAt;
    private String failedError;
    private String failedMessage;

    public void accept(SchedulerJobRunHistoryBO item) {
        if (item == null) {
            return;
        }
        actual++;
        if ("auto".equalsIgnoreCase(item.getTriggerType())) {
            auto++;
        } else {
            manual++;
        }
        String normalized = item.getStatus() == null ? "" : item.getStatus().toLowerCase();
        if ("success".equals(normalized)) {
            success++;
        } else if ("skipped".equals(normalized)) {
            skipped++;
        } else if ("failed".equals(normalized) || "error".equals(normalized)
                || "partial".equals(normalized) || "partial_failed".equals(normalized)) {
            failed++;
            OffsetDateTime startedAt = item.getStartedAt();
            if (failedAt == null || (startedAt != null && startedAt.isAfter(failedAt))) {
                failedAt = startedAt;
                failedError = item.getErrorMessage();
                failedMessage = item.getRemark();
            }
        }
    }
}
