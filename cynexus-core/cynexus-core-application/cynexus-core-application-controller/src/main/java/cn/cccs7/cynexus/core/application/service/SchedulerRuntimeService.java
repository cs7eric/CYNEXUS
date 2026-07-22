package cn.cccs7.cynexus.core.application.service;

import cn.cccs7.cynexus.core.application.dto.SchedulerManagementJobActionResponseDTO;
import cn.cccs7.cynexus.core.domain.entity.SchedulerDueJobBO;
import cn.cccs7.cynexus.core.domain.service.scheduler.SchedulerExecutionDomainService;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "cynexus.scheduler.runtime", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SchedulerRuntimeService {

    private static final ZoneId CHINA_ZONE = ZoneId.of("Asia/Shanghai");

    private final SchedulerAppService schedulerAppService;
    private final SchedulerExecutionDomainService schedulerExecutionDomainService;

    public SchedulerRuntimeService(SchedulerExecutionDomainService schedulerExecutionDomainService,
                                   SchedulerAppService schedulerAppService) {
        this.schedulerExecutionDomainService = schedulerExecutionDomainService;
        this.schedulerAppService = schedulerAppService;
    }

    @Scheduled(
            initialDelayString = "${cynexus.scheduler.runtime.initial-delay-ms:15000}",
            fixedDelayString = "${cynexus.scheduler.runtime.tick-ms:30000}"
    )
    public void runDueJobs() {
        schedulerAppService.reconcileRunningDataJobRuns();
        OffsetDateTime now = OffsetDateTime.now(CHINA_ZONE);
        for (SchedulerDueJobBO dueJob : schedulerExecutionDomainService.findDueJobs(now)) {
            if (!schedulerExecutionDomainService.acquire(dueJob)) {
                continue;
            }
            boolean backgroundProcessing = false;
            Integer marketTargets = null;
            RuntimeException marketFailure = null;
            try {
                schedulerExecutionDomainService.markRunning(dueJob, now);
                if (schedulerExecutionDomainService.isMarketAutomation(dueJob)) {
                    marketTargets = schedulerExecutionDomainService.refreshMarketAutomation(dueJob);
                } else {
                    SchedulerManagementJobActionResponseDTO response = schedulerAppService.triggerScheduledJob(
                            dueJob.getJob().getCode());
                    backgroundProcessing = response != null && "processing".equalsIgnoreCase(response.getStatus());
                }
            } catch (RuntimeException exception) {
                if (schedulerExecutionDomainService.isMarketAutomation(dueJob)) {
                    marketFailure = exception;
                }
            } finally {
                if (!backgroundProcessing) {
                    if (schedulerExecutionDomainService.isMarketAutomation(dueJob)) {
                        schedulerExecutionDomainService.recordMarketAutomationResult(
                                dueJob, now, marketTargets, marketFailure);
                    }
                    schedulerExecutionDomainService.markStopped(dueJob, OffsetDateTime.now(CHINA_ZONE));
                }
                schedulerExecutionDomainService.release(dueJob);
            }
        }
    }

}
