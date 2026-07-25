package cn.cccs7.cynexus.core.domain.scheduler;

import cn.cccs7.cynexus.common.core.constant.CommonConstants;
import cn.cccs7.cynexus.core.common.constant.CoreConstants;
import cn.cccs7.cynexus.core.domain.entity.DataJobTriggerResultBO;
import cn.cccs7.cynexus.core.domain.entity.SchedulerDailyCounterBO;
import cn.cccs7.cynexus.core.domain.entity.SchedulerJobCategoryBO;
import cn.cccs7.cynexus.core.domain.entity.SchedulerJobRunHistoryBO;
import cn.cccs7.cynexus.core.domain.entity.SchedulerTodayCountersBO;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.springframework.scheduling.support.CronExpression;

/** Domain decisions used by scheduler management and run reconciliation. */
public final class SchedulerManagementPolicy {

    private static final ZoneId CHINA_ZONE = ZoneId.of(CommonConstants.SHANGHAI_ZONE_ID);

    private SchedulerManagementPolicy() {
    }

    public static String normalizeTriggerStatus(DataJobTriggerResultBO result) {
        if (result == null || result.getStatus() == null || result.getStatus().isBlank()) {
            return CoreConstants.STATUS_FAILED;
        }
        return CoreConstants.STATUS_LOCKED.equalsIgnoreCase(result.getStatus()) ? CoreConstants.STATUS_SKIPPED : result.getStatus();
    }

    public static boolean isTerminalStatus(String status) {
        if (status == null) {
            return false;
        }
        return Set.of(CoreConstants.STATUS_SUCCESS, CoreConstants.STATUS_SKIPPED, CoreConstants.STATUS_FAILED,
                CoreConstants.STATUS_LOCKED, CoreConstants.STATUS_UNKNOWN_JOB, CoreConstants.STATUS_PARTIAL).contains(status.toLowerCase());
    }

    public static boolean isAcceptedSuccess(DataJobTriggerResultBO result) {
        if (result == null || !Boolean.TRUE.equals(result.getAccepted())) {
            return false;
        }
        String status = result.getStatus() == null ? "" : result.getStatus().toLowerCase();
        return CoreConstants.STATUS_SUCCESS.equals(status) || CoreConstants.STATUS_SKIPPED.equals(status);
    }

    public static boolean isAcceptedRunning(DataJobTriggerResultBO result) {
        if (result == null || !Boolean.TRUE.equals(result.getAccepted()) || result.getRunId() == null) {
            return false;
        }
        String status = result.getStatus() == null ? "" : result.getStatus().toLowerCase();
        return CoreConstants.STATUS_RUNNING.equals(status) || CoreConstants.STATUS_PROCESSING.equals(status);
    }

    public static boolean shouldPoll(DataJobTriggerResultBO result) {
        if (result == null || result.getRunId() == null || result.getRunId().isBlank()) {
            return false;
        }
        return !isTerminalStatus(normalizeTriggerStatus(result)) || isAcceptedRunning(result);
    }

    public static String triggerError(DataJobTriggerResultBO result) {
        if (result == null || result.getResult() == null) {
            return "missing data job executor result";
        }
        Object errors = result.getResult().get("errors");
        if (errors instanceof List<?> list && !list.isEmpty()) {
            return list.stream().filter(Objects::nonNull).map(String::valueOf).collect(Collectors.joining("; "));
        }
        if (!isAcceptedSuccess(result)) {
            Object error = firstPresent(result.getResult(), Set.of("error", "message", "skip_reason"));
            return error == null ? result.getStatus() : String.valueOf(error);
        }
        return null;
    }

    public static String allowedHistoryTriggerType(String triggerType) {
        return "auto".equals(triggerType) ? "auto" : "manual";
    }

    public static String categoryCode(String value) {
        String code = value == null ? "" : value.trim().toLowerCase()
                .replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
        return code.isBlank() ? "category" : code;
    }

    public static String uniqueCategoryCode(String requestedCode, List<SchedulerJobCategoryBO> categories) {
        String base = categoryCode(requestedCode);
        Set<String> usedCodes = categories.stream().map(SchedulerJobCategoryBO::getCode)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        if (!usedCodes.contains(base)) {
            return base;
        }
        int suffix = 2;
        while (usedCodes.contains(base + "_" + suffix)) {
            suffix++;
        }
        return base + "_" + suffix;
    }

    public static int nextCategorySortOrder(List<SchedulerJobCategoryBO> categories) {
        return categories.stream().map(SchedulerJobCategoryBO::getSortOrder).filter(Objects::nonNull)
                .max(Integer::compareTo).orElse(0) + 10;
    }

    public static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public static Map<Long, SchedulerTodayCountersBO> todayCounters(List<SchedulerJobRunHistoryBO> history) {
        Map<Long, SchedulerTodayCountersBO> result = new LinkedHashMap<>();
        for (SchedulerJobRunHistoryBO item : history == null ? List.<SchedulerJobRunHistoryBO>of() : history) {
            if (item.getJobId() == null || item.getDeletedAt() != null) {
                continue;
            }
            result.computeIfAbsent(item.getJobId(), key -> new SchedulerTodayCountersBO()).accept(item);
        }
        return result;
    }

    public static Map<LocalDate, SchedulerDailyCounterBO> dailyCounters(
            List<SchedulerJobRunHistoryBO> history, LocalDate start, int days) {
        Map<LocalDate, SchedulerDailyCounterBO> result = new TreeMap<>();
        for (int i = 0; i < days; i++) {
            result.put(start.plusDays(i), new SchedulerDailyCounterBO());
        }
        for (SchedulerJobRunHistoryBO item : history == null ? List.<SchedulerJobRunHistoryBO>of() : history) {
            if (item.getStartedAt() == null) {
                continue;
            }
            LocalDate date = item.getStartedAt().atZoneSameInstant(CHINA_ZONE).toLocalDate();
            SchedulerDailyCounterBO counter = result.get(date);
            if (counter != null) {
                counter.accept(item.getStatus(), item.getTriggerType());
            }
        }
        return result;
    }

    public static int boundedDays(int days, int max) {
        return Math.max(1, Math.min(days, max));
    }

    public static int boundedLimit(int limit, int max) {
        return Math.max(1, Math.min(limit, max));
    }

    public static Integer expectedCount(Map<String, Object> schedule, LocalDate today) {
        if (schedule == null || schedule.isEmpty()) {
            return null;
        }
        if (Boolean.TRUE.equals(asBoolean(schedule.get("workday_only"))) && isWeekend(today)) {
            return 0;
        }
        Integer everyMinutes = asInteger(schedule.get("every_minutes"));
        if (everyMinutes != null && everyMinutes > 0) {
            Object windows = schedule.get("intraday_windows");
            if (!(windows instanceof List<?> list) || list.isEmpty()) {
                return null;
            }
            int total = 0;
            for (Object item : list) {
                if (!(item instanceof Map<?, ?> window)) {
                    continue;
                }
                LocalTime start = parseLocalTime(asString(window.get("start")));
                LocalTime end = parseLocalTime(asString(window.get("end")));
                if (start != null && end != null && end.isAfter(start)) {
                    total += (int) (java.time.Duration.between(start, end).toMinutes() / everyMinutes);
                }
            }
            return total;
        }
        if (asString(schedule.get("run_time")) != null) {
            return 1;
        }
        String cronValue = asString(schedule.get("cron"));
        return cronValue == null || cronValue.isBlank() ? null : countCronFires(cronValue, today);
    }

    public static Integer metric(String text, String prefix) {
        int start = text == null ? -1 : text.indexOf(prefix);
        if (start < 0) {
            return null;
        }
        int index = start + prefix.length();
        StringBuilder digits = new StringBuilder();
        while (index < text.length() && Character.isDigit(text.charAt(index))) {
            digits.append(text.charAt(index++));
        }
        try {
            return digits.isEmpty() ? null : Integer.parseInt(digits.toString());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static Integer countCronFires(String expression, LocalDate today) {
        try {
            String normalized = expression.trim();
            if (normalized.split("\\s+").length == 5) {
                normalized = "0 " + normalized;
            }
            CronExpression cron = CronExpression.parse(normalized);
            OffsetDateTime cursor = today.atStartOfDay(CHINA_ZONE).toOffsetDateTime().minusSeconds(1);
            OffsetDateTime end = today.plusDays(1).atStartOfDay(CHINA_ZONE).toOffsetDateTime();
            int count = 0;
            while (count < 1500) {
                OffsetDateTime next = cron.next(cursor);
                if (next == null || !next.isBefore(end)) {
                    break;
                }
                count++;
                cursor = next;
            }
            return count;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static boolean isWeekend(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }

    private static Object firstPresent(Map<String, Object> map, Set<String> keys) {
        for (String key : keys) {
            if (map.containsKey(key) && map.get(key) != null) {
                return map.get(key);
            }
        }
        return null;
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
        return value instanceof Boolean bool ? bool : value == null ? null : Boolean.parseBoolean(String.valueOf(value));
    }

    private static LocalTime parseLocalTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalTime.parse(value.trim());
        } catch (RuntimeException ignored) {
            return null;
        }
    }
}
