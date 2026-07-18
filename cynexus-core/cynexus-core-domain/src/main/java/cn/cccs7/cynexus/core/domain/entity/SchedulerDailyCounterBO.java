package cn.cccs7.cynexus.core.domain.entity;

import lombok.Data;

@Data
public class SchedulerDailyCounterBO {
    private int total;
    private int success;
    private int failed;
    private int skipped;
    private int manual;
    private int auto;

    public void accept(String status, String triggerType) {
        total++;
        if ("auto".equalsIgnoreCase(triggerType)) {
            auto++;
        } else {
            manual++;
        }
        String normalized = status == null ? "" : status.toLowerCase();
        if ("success".equals(normalized)) {
            success++;
        } else if ("skipped".equals(normalized)) {
            skipped++;
        } else if ("failed".equals(normalized) || "error".equals(normalized)
                || "partial".equals(normalized) || "partial_failed".equals(normalized)) {
            failed++;
        }
    }
}
