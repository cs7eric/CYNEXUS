package cn.cccs7.cynexus.ai.application.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/** Scheduler adapter for the persisted vision execution queue. */
@Service
public class AiVisionTaskWorker {
    private final AiVisionTaskAppService taskAppService;
    private final int staleTimeoutMinutes;

    public AiVisionTaskWorker(AiVisionTaskAppService taskAppService,
                              @Value("${cynexus.ai.vision-task.stale-timeout-minutes:15}") int staleTimeoutMinutes) {
        this.taskAppService = taskAppService;
        this.staleTimeoutMinutes = Math.max(1, staleTimeoutMinutes);
    }

    @Scheduled(initialDelayString = "${cynexus.ai.vision-task.recovery-initial-delay-ms:10000}",
            fixedDelayString = "${cynexus.ai.vision-task.recovery-delay-ms:60000}")
    public void recoverStaleTasks() {
        taskAppService.recoverStaleTasks(staleTimeoutMinutes);
    }

    @Scheduled(initialDelayString = "${cynexus.ai.vision-task.cleanup-initial-delay-ms:15000}",
            fixedDelayString = "${cynexus.ai.vision-task.cleanup-delay-ms:3600000}")
    public void purgeTerminalInputs() {
        taskAppService.purgeTerminalInputs();
    }

    @Scheduled(initialDelayString = "${cynexus.ai.vision-task.worker-initial-delay-ms:2000}",
            fixedDelayString = "${cynexus.ai.vision-task.worker-delay-ms:1000}")
    public void processNext() {
        taskAppService.dispatchNext();
    }
}
