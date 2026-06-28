package cn.cccs7.cynexus.core.infra.client;

import cn.cccs7.cynexus.core.domain.entity.DataJobTriggerRequestBO;
import cn.cccs7.cynexus.core.domain.entity.DataJobTriggerResultBO;
import java.util.Map;

public final class DataJobExecutorE2eTestMain {

    private DataJobExecutorE2eTestMain() {
    }

    public static void main(String[] args) {
        String baseUrl = arg(args, 0, "http://127.0.0.1:18083");
        String token = arg(args, 1, "");
        HttpDataJobExecutorClient client = new HttpDataJobExecutorClient(baseUrl, token, 10_000);

        Map<String, Object> health = client.health();
        assertTrue(Boolean.TRUE.equals(health.get("ok")), "health.ok must be true");

        Map<String, Object> jobs = client.listJobs();
        assertTrue(String.valueOf(jobs).contains("trigger_probe"), "trigger_probe must be listed");

        DataJobTriggerResultBO result = client.triggerJob("trigger_probe", DataJobTriggerRequestBO.builder()
                .triggerRequestId("java-e2e-trigger-probe")
                .triggeredBy("cynexus-core-e2e")
                .dryRun(true)
                .params(Map.of("hello", "core", "answer", 42))
                .build());
        assertTrue(Boolean.TRUE.equals(result.getAccepted()), "trigger_probe must be accepted");
        assertTrue("success".equals(result.getStatus()), "trigger_probe status must be success");
        assertTrue("trigger_probe".equals(result.getJobKey()), "trigger_probe job key mismatch");
        assertTrue(result.getResult() != null, "trigger_probe result must not be null");
        System.out.println("JAVA_TO_DATA_JOB_EXECUTOR_E2E_OK runId=" + result.getRunId());
    }

    private static String arg(String[] args, int index, String defaultValue) {
        if (args == null || args.length <= index || args[index] == null || args[index].isBlank()) {
            return defaultValue;
        }
        return args[index];
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
