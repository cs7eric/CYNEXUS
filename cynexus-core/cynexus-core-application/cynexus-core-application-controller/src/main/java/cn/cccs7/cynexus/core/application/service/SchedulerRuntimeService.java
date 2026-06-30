package cn.cccs7.cynexus.core.application.service;

import cn.cccs7.cynexus.core.domain.entity.SchedulerJobBO;
import cn.cccs7.cynexus.core.domain.entity.SchedulerJobStatusBO;
import cn.cccs7.cynexus.core.domain.service.SchedulerDomainService;
import cn.cccs7.cynexus.core.application.dto.SchedulerManagementJobActionResponseDTO;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.WeekFields;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
@ConditionalOnProperty(prefix = "cynexus.scheduler.runtime", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SchedulerRuntimeService {

    private static final Logger log = LoggerFactory.getLogger(SchedulerRuntimeService.class);
    private static final ZoneId CHINA_ZONE = ZoneId.of("Asia/Shanghai");

    private final SchedulerDomainService schedulerDomainService;
    private final SchedulerAppService schedulerAppService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Set<Long> inFlightJobIds = ConcurrentHashMap.newKeySet();

    public SchedulerRuntimeService(SchedulerDomainService schedulerDomainService,
                                   SchedulerAppService schedulerAppService) {
        this.schedulerDomainService = schedulerDomainService;
        this.schedulerAppService = schedulerAppService;
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
            if (!isRunnableStatus(status) || !isDue(job, status, now)) {
                continue;
            }
            if (!inFlightJobIds.add(job.getId())) {
                continue;
            }
            boolean backgroundProcessing = false;
            try {
                markRunning(job, status, now);
                SchedulerManagementJobActionResponseDTO response = schedulerAppService.triggerScheduledJob(job.getCode());
                backgroundProcessing = response != null && "processing".equalsIgnoreCase(response.getStatus());
            } catch (RuntimeException exception) {
                log.warn("Scheduled job trigger failed: jobCode={}", job.getCode(), exception);
            } finally {
                if (!backgroundProcessing) {
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
                && SchedulerDataJobKeyResolver.resolve(job.getCode()).isPresent();
    }

    private boolean isRunnableStatus(SchedulerJobStatusBO status) {
        return status != null
                && status.getDeletedAt() == null
                && Boolean.TRUE.equals(status.getEnabled())
                && !readSchedule(status).isEmpty();
    }

    private boolean isDue(SchedulerJobBO job, SchedulerJobStatusBO status, OffsetDateTime now) {
        Map<String, Object> schedule = readSchedule(status);
        if (isWorkdayOnly(schedule) && isWeekend(now)) {
            return false;
        }
        if (hasText(asString(schedule.get("cron"))) && isCronDue(schedule, status, now)) {
            return true;
        }
        Integer everyMinutes = asInteger(schedule.get("every_minutes"));
        if (everyMinutes != null && everyMinutes > 0) {
            return isIntervalDue(schedule, status, now, everyMinutes);
        }
        if (hasText(asString(schedule.get("run_time")))) {
            return isRunTimeDue(schedule, status, now);
        }
        log.debug("Scheduler job has a schedule but no supported timing key: jobCode={}", job.getCode());
        return false;
    }

    private boolean isCronDue(Map<String, Object> schedule, SchedulerJobStatusBO status, OffsetDateTime now) {
        try {
            String expression = normalizeCron(asString(schedule.get("cron")));
            CronExpression cron = CronExpression.parse(expression);
            OffsetDateTime base = status.getLastRunAt() == null ? now.minusDays(1) : status.getLastRunAt();
            OffsetDateTime next = cron.next(base);
            return next != null && !next.isAfter(now);
        } catch (RuntimeException exception) {
            log.warn("Invalid scheduler cron expression: cron={}", schedule.get("cron"), exception);
            return false;
        }
    }

    private boolean isIntervalDue(Map<String, Object> schedule,
                                  SchedulerJobStatusBO status,
                                  OffsetDateTime now,
                                  int everyMinutes) {
        if (!isWithinWindows(schedule, now.toLocalTime())) {
            return false;
        }
        OffsetDateTime lastRunAt = status.getLastRunAt();
        return lastRunAt == null || Duration.between(lastRunAt, now).toMinutes() >= everyMinutes;
    }

    private boolean isRunTimeDue(Map<String, Object> schedule,
                                 SchedulerJobStatusBO status,
                                 OffsetDateTime now) {
        LocalTime runTime = parseLocalTime(asString(schedule.get("run_time")));
        if (runTime == null || now.toLocalTime().isBefore(runTime)) {
            return false;
        }
        if (Boolean.TRUE.equals(asBoolean(schedule.get("run_once_per_week")))) {
            return !hasRunThisWeek(status.getLastRunAt(), now);
        }
        return !hasRunOnDate(status.getLastRunAt(), now.toLocalDate());
    }

    private boolean isWithinWindows(Map<String, Object> schedule, LocalTime now) {
        Object windows = schedule.get("intraday_windows");
        if (!(windows instanceof List<?> list) || list.isEmpty()) {
            return true;
        }
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> window)) {
                continue;
            }
            LocalTime start = parseLocalTime(asString(window.get("start")));
            LocalTime end = parseLocalTime(asString(window.get("end")));
            if (start != null && end != null && !now.isBefore(start) && !now.isAfter(end)) {
                return true;
            }
        }
        return false;
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

    private static String normalizeCron(String cron) {
        String trimmed = cron == null ? "" : cron.trim();
        String[] fields = trimmed.split("\\s+");
        if (fields.length == 5) {
            return "0 " + trimmed;
        }
        return trimmed;
    }

    private static boolean isWorkdayOnly(Map<String, Object> schedule) {
        return Boolean.TRUE.equals(asBoolean(schedule.get("workday_only")));
    }

    private static boolean isWeekend(OffsetDateTime now) {
        DayOfWeek dayOfWeek = now.getDayOfWeek();
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }

    private static boolean hasRunOnDate(OffsetDateTime lastRunAt, LocalDate date) {
        return lastRunAt != null && Objects.equals(lastRunAt.atZoneSameInstant(CHINA_ZONE).toLocalDate(), date);
    }

    private static boolean hasRunThisWeek(OffsetDateTime lastRunAt, OffsetDateTime now) {
        if (lastRunAt == null) {
            return false;
        }
        WeekFields weekFields = WeekFields.of(Locale.ROOT);
        LocalDate lastDate = lastRunAt.atZoneSameInstant(CHINA_ZONE).toLocalDate();
        LocalDate nowDate = now.toLocalDate();
        return lastDate.get(weekFields.weekBasedYear()) == nowDate.get(weekFields.weekBasedYear())
                && lastDate.get(weekFields.weekOfWeekBasedYear()) == nowDate.get(weekFields.weekOfWeekBasedYear());
    }

    private static LocalTime parseLocalTime(String value) {
        if (!hasText(value)) {
            return null;
        }
        try {
            return LocalTime.parse(value.trim());
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static Integer asInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static Boolean asBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value == null) {
            return null;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
