package cn.cccs7.cynexus.core.domain.scheduler;

import cn.cccs7.cynexus.common.core.constant.CommonConstants;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;

@Service
public class SchedulerTimingDomainService {

    private static final ZoneId CHINA_ZONE = ZoneId.of(CommonConstants.SHANGHAI_ZONE_ID);

    public boolean isDue(Map<String, Object> schedule, OffsetDateTime lastRunAt, OffsetDateTime now) {
        if (schedule == null || schedule.isEmpty() || now == null) {
            return false;
        }
        if (Boolean.TRUE.equals(asBoolean(schedule.get("workday_only"))) && isWeekend(now)) {
            return false;
        }
        String cron = asString(schedule.get("cron"));
        if (hasText(cron)) {
            return isCronDue(cron, lastRunAt, now);
        }
        Integer everySeconds = asInteger(schedule.get("every_seconds"));
        if (everySeconds != null && everySeconds > 0) {
            return isIntervalDue(schedule, lastRunAt, now, everySeconds, true);
        }
        Integer everyMinutes = asInteger(schedule.get("every_minutes"));
        if (everyMinutes != null && everyMinutes > 0) {
            return isIntervalDue(schedule, lastRunAt, now, everyMinutes, false);
        }
        String runTime = asString(schedule.get("run_time"));
        return hasText(runTime) && isRunTimeDue(runTime, schedule, lastRunAt, now);
    }

    private boolean isCronDue(String cronValue, OffsetDateTime lastRunAt, OffsetDateTime now) {
        try {
            CronExpression cron = CronExpression.parse(normalizeCron(cronValue));
            OffsetDateTime base = lastRunAt == null ? now.minusDays(1) : lastRunAt;
            OffsetDateTime next = cron.next(base);
            return next != null && !next.isAfter(now);
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private boolean isIntervalDue(Map<String, Object> schedule, OffsetDateTime lastRunAt,
                                  OffsetDateTime now, int interval, boolean seconds) {
        return isWithinWindows(schedule, now.toLocalTime())
                && (lastRunAt == null || (seconds
                ? Duration.between(lastRunAt, now).getSeconds() >= interval
                : Duration.between(lastRunAt, now).toMinutes() >= interval));
    }

    private boolean isRunTimeDue(String runTimeValue, Map<String, Object> schedule,
                                 OffsetDateTime lastRunAt, OffsetDateTime now) {
        LocalTime runTime = parseLocalTime(runTimeValue);
        if (runTime == null || now.toLocalTime().isBefore(runTime)) {
            return false;
        }
        if (Boolean.TRUE.equals(asBoolean(schedule.get("run_once_per_week")))) {
            return !hasRunThisWeek(lastRunAt, now);
        }
        return !hasRunOnDate(lastRunAt, now.toLocalDate());
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

    private static String normalizeCron(String cron) {
        String trimmed = cron == null ? "" : cron.trim();
        return trimmed.split("\\s+").length == 5 ? "0 " + trimmed : trimmed;
    }

    private static boolean isWeekend(OffsetDateTime now) {
        DayOfWeek day = now.getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }

    private static boolean hasRunOnDate(OffsetDateTime lastRunAt, LocalDate date) {
        return lastRunAt != null && Objects.equals(lastRunAt.atZoneSameInstant(CHINA_ZONE).toLocalDate(), date);
    }

    private static boolean hasRunThisWeek(OffsetDateTime lastRunAt, OffsetDateTime now) {
        if (lastRunAt == null) {
            return false;
        }
        WeekFields fields = WeekFields.of(Locale.ROOT);
        LocalDate lastDate = lastRunAt.atZoneSameInstant(CHINA_ZONE).toLocalDate();
        LocalDate nowDate = now.toLocalDate();
        return lastDate.get(fields.weekBasedYear()) == nowDate.get(fields.weekBasedYear())
                && lastDate.get(fields.weekOfWeekBasedYear()) == nowDate.get(fields.weekOfWeekBasedYear());
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
        try {
            return value == null ? null : Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static Boolean asBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        return value == null ? null : Boolean.parseBoolean(String.valueOf(value));
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
