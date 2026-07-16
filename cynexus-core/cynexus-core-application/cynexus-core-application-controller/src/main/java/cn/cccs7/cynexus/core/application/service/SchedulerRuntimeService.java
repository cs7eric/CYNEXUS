package cn.cccs7.cynexus.core.application.service;

import cn.cccs7.cynexus.core.domain.entity.SchedulerJobBO;
import cn.cccs7.cynexus.core.domain.client.MarketAutomationClient;
import cn.cccs7.cynexus.core.domain.entity.SchedulerJobStatusBO;
import cn.cccs7.cynexus.core.domain.scheduler.SchedulerDomainService;
import cn.cccs7.cynexus.core.domain.scheduler.SchedulerTimingDomainService;
import cn.cccs7.cynexus.core.application.dto.SchedulerManagementJobActionResponseDTO;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
@ConditionalOnProperty(prefix = "cynexus.scheduler.runtime", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SchedulerRuntimeService {

    private static final Logger log = LoggerFactory.getLogger(SchedulerRuntimeService.class);
    private static final ZoneId CHINA_ZONE = ZoneId.of("Asia/Shanghai");

    private final SchedulerDomainService schedulerDomainService;
    private final SchedulerTimingDomainService schedulerTimingDomainService;
    private final SchedulerAppService schedulerAppService;
    private final MarketAutomationClient marketAutomationClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Set<Long> inFlightJobIds = ConcurrentHashMap.newKeySet();

    public SchedulerRuntimeService(SchedulerDomainService schedulerDomainService,
                                   SchedulerTimingDomainService schedulerTimingDomainService,
                                   SchedulerAppService schedulerAppService,
                                   MarketAutomationClient marketAutomationClient) {
        this.schedulerDomainService = schedulerDomainService;
        this.schedulerTimingDomainService = schedulerTimingDomainService;
        this.schedulerAppService = schedulerAppService;
        this.marketAutomationClient = marketAutomationClient;
    }

    @Scheduled(
            initialDelayString = "${cynexus.scheduler.runtime.initial-delay-ms:15000}",
            fixedDelayString = "${cynexus.scheduler.runtime.tick-ms:30000}"
    )
    public void runDueJobs() {
        schedulerAppService.reconcileRunningDataJobRuns();
        OffsetDateTime now = OffsetDateTime.now(CHINA_ZONE);
        Map<Long, SchedulerJobStatusBO> statusByJobId = schedulerDomainService.listStatuses().stream()
                .filter(status -> status.getDeletedAt() == null)
                .collect(Collectors.toMap(SchedulerJobStatusBO::getJobId, Function.identity(), SchedulerRuntimeService::pickLatestStatus));

        for (SchedulerJobBO job : schedulerDomainService.listJobs()) {
            if (!isRunnableJob(job)) {
                continue;
            }
            SchedulerJobStatusBO status = statusByJobId.get(job.getId());
            if (!isRunnableStatus(status) || !schedulerTimingDomainService.isDue(
                    readSchedule(status), status.getLastRunAt(), now)) {
                continue;
            }
            if (!inFlightJobIds.add(job.getId())) {
                continue;
            }
            boolean backgroundProcessing = false;
            Integer marketTargets = null;
            RuntimeException marketFailure = null;
            try {
                markRunning(job, status, now);
                if (isMarketAutomationJob(job)) {
                    marketTargets = marketAutomationClient.refreshInformationPipeline();
                } else {
                    SchedulerManagementJobActionResponseDTO response = schedulerAppService.triggerScheduledJob(job.getCode());
                    backgroundProcessing = response != null && "processing".equalsIgnoreCase(response.getStatus());
                }
            } catch (RuntimeException exception) {
                if (isMarketAutomationJob(job)) {
                    marketFailure = exception;
                }
                log.warn("Scheduled job trigger failed: jobCode={}", job.getCode(), exception);
            } finally {
                if (!backgroundProcessing) {
                    if (isMarketAutomationJob(job)) {
                        markAutomationResult(job, now, marketTargets, marketFailure);
                    }
                    markStopped(job);
                }
                inFlightJobIds.remove(job.getId());
            }
        }
    }

    private boolean isRunnableJob(SchedulerJobBO job) {
        return job != null
                && job.getDeletedAt() == null
                && Boolean.TRUE.equals(job.getEnabled())
                && (SchedulerDataJobKeyResolver.resolve(job.getCode()).isPresent() || isMarketAutomationJob(job));
    }

    private boolean isMarketAutomationJob(SchedulerJobBO job) {
        return job != null && "market_information_pipeline_refresh".equals(job.getCode());
    }

    private boolean isRunnableStatus(SchedulerJobStatusBO status) {
        return status != null
                && status.getDeletedAt() == null
                && Boolean.TRUE.equals(status.getEnabled())
                && !readSchedule(status).isEmpty();
    }

    private void markRunning(SchedulerJobBO job, SchedulerJobStatusBO status, OffsetDateTime now) {
        SchedulerJobStatusBO resolved = status == null ? createStatus(job) : status;
        schedulerDomainService.updateStatusRunning(resolved.getId(), true, now, null);
    }

    private void markStopped(SchedulerJobBO job) {
        SchedulerJobStatusBO current = schedulerDomainService.findStatusByJobId(job.getId());
        if (current == null) {
            return;
        }
        schedulerDomainService.updateStatusRunning(
                current.getId(),
                false,
                current.getSchedulerStartedAt(),
                OffsetDateTime.now(CHINA_ZONE));
    }

    private void markAutomationResult(SchedulerJobBO job, OffsetDateTime startedAt,
                                      Integer targetsProcessed, RuntimeException failure) {
        SchedulerJobStatusBO current = schedulerDomainService.findStatusByJobId(job.getId());
        if (current == null) return;
        OffsetDateTime finishedAt = OffsetDateTime.now(CHINA_ZONE);
        BigDecimal durationSeconds = BigDecimal.valueOf(
                Math.max(0, Duration.between(startedAt, finishedAt).toMillis()) / 1000.0);
        schedulerDomainService.updateStatusRunResult(
                current.getId(), finishedAt, failure == null,
                failure == null ? "success" : "failed",
                failure == null ? null : failure.getMessage(),
                durationSeconds, targetsProcessed, current.getProgress(), current.getExtra(), current.getRemark());
    }

    private SchedulerJobStatusBO createStatus(SchedulerJobBO job) {
        SchedulerJobStatusBO created = SchedulerJobStatusBO.builder()
                .jobId(job.getId())
                .enabled(Boolean.TRUE.equals(job.getEnabled()))
                .running(false)
                .build();
        Long id = schedulerDomainService.createStatus(created);
        created.setId(id);
        return created;
    }

    private Map<String, Object> readSchedule(SchedulerJobStatusBO status) {
        String json = status == null ? null : status.getSchedule();
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return asStringObjectMap(objectMapper.readValue(json, Map.class));
        } catch (Exception exception) {
            log.warn("Invalid scheduler schedule JSON: statusId={}", status.getId(), exception);
            return Map.of();
        }
    }

    private static SchedulerJobStatusBO pickLatestStatus(SchedulerJobStatusBO left, SchedulerJobStatusBO right) {
        OffsetDateTime leftTime = left.getUpdatedAt() == null ? left.getCreatedAt() : left.getUpdatedAt();
        OffsetDateTime rightTime = right.getUpdatedAt() == null ? right.getCreatedAt() : right.getUpdatedAt();
        if (leftTime == null) {
            return right;
        }
        if (rightTime == null) {
            return left;
        }
        return rightTime.isAfter(leftTime) ? right : left;
    }

    private static Map<String, Object> asStringObjectMap(Map<?, ?> map) {
        if (map == null || map.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        map.forEach((key, value) -> result.put(String.valueOf(key), value));
        return result;
    }

    private static String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

}
