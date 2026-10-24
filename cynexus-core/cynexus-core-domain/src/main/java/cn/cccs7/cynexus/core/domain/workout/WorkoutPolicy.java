package cn.cccs7.cynexus.core.domain.workout;

import cn.cccs7.cynexus.core.domain.workout.WorkoutModels.*;
import cn.cccs7.cynexus.core.domain.common.RequestFingerprint;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

public final class WorkoutPolicy {
    private WorkoutPolicy() {}
    private static final Set<String> SPLITS = Set.of("chest_triceps", "back_biceps", "shoulders", "rest");
    private static final Set<String> METRICS = Set.of("weight", "height", "head", "neck", "shoulders", "shoulder_width",
            "chest", "under_chest", "waist", "abdomen", "hips", "upper_arm", "forearm", "wrist", "thigh", "calf", "ankle");
    private static final Set<String> PAIRED = Set.of("upper_arm", "forearm", "wrist", "thigh", "calf", "ankle");

    public static void require(boolean condition, String message) {
        if (!condition) throw new IllegalArgumentException(message);
    }
    public static String text(String value, int max, String field) {
        require(value != null && !value.isBlank() && value.trim().length() <= max, "Invalid " + field);
        return value.trim();
    }
    public static String optionalText(String value, int max, String field) {
        if (value == null || value.isBlank()) return null;
        return text(value, max, field);
    }
    public static LocalDate date(LocalDate value, String field) {
        require(value != null && value.getYear() >= 1900 && value.getYear() <= 2200, "Invalid " + field);
        return value;
    }
    public static LocalDate pastOrToday(LocalDate value, String field) {
        date(value, field);
        require(!value.isAfter(LocalDate.now()), field + " cannot be in the future");
        return value;
    }
    public static String category(String value) {
        String category = text(value, 20, "category").toLowerCase(java.util.Locale.ROOT);
        require(Set.of("strength", "cardio").contains(category), "Invalid category");
        return category;
    }
    public static String split(String value, String category) {
        if (value == null || value.isBlank()) return null;
        String split = value.trim().toLowerCase(java.util.Locale.ROOT).replace(' ', '_').replace('+', '_');
        split = split.replaceAll("_+", "_");
        require(SPLITS.contains(split) && !"rest".equals(split), "Invalid split");
        require(!"cardio".equals(category), "Cardio sessions cannot have a split");
        return split;
    }
    public static int duration(Integer value) {
        require(value != null && value >= 1 && value <= 1440, "Duration must be between 1 and 1,440 minutes");
        return value;
    }
    public static String metric(String value) {
        String metric = text(value, 32, "metric").toLowerCase(java.util.Locale.ROOT);
        metric = value.trim().replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase(java.util.Locale.ROOT)
                .replace('-', '_').replace(' ', '_');
        require(METRICS.contains(metric), "Invalid measurement metric");
        return metric;
    }
    public static String side(String value, String metric) {
        boolean paired = PAIRED.contains(metric);
        if (value == null || value.isBlank()) {
            require(!paired, "A left or right side is required for this metric");
            return null;
        }
        String side = value.trim().toLowerCase(java.util.Locale.ROOT);
        require(paired && Set.of("left", "right").contains(side), paired ? "Invalid measurement side" : "This metric does not have sides");
        return side;
    }
    public static BigDecimal value(Double input, String metric) {
        require(input != null && Double.isFinite(input), "Invalid measurement value");
        BigDecimal value = BigDecimal.valueOf(input);
        require(value.signum() > 0 && value.compareTo(new BigDecimal("1000000")) < 0 && value.scale() <= 6, "Invalid measurement value");
        BigDecimal min = switch (metric) {
            case "weight" -> new BigDecimal("1");
            case "height" -> new BigDecimal("40");
            case "head" -> new BigDecimal("20");
            case "neck" -> new BigDecimal("10");
            case "shoulders", "chest", "under_chest", "waist", "abdomen", "hips" -> new BigDecimal("30");
            case "shoulder_width" -> new BigDecimal("10");
            case "upper_arm", "forearm", "wrist", "calf", "ankle" -> new BigDecimal("5");
            case "thigh" -> new BigDecimal("10");
            default -> new BigDecimal("1");
        };
        BigDecimal max = switch (metric) {
            case "weight" -> new BigDecimal("600");
            case "height" -> new BigDecimal("260");
            case "head", "neck" -> new BigDecimal("90");
            case "shoulders" -> new BigDecimal("220");
            case "shoulder_width" -> new BigDecimal("100");
            case "upper_arm" -> new BigDecimal("100");
            case "forearm" -> new BigDecimal("80");
            case "wrist" -> new BigDecimal("50");
            case "thigh" -> new BigDecimal("150");
            case "calf" -> new BigDecimal("100");
            case "ankle" -> new BigDecimal("60");
            default -> new BigDecimal("250");
        };
        require(value.compareTo(min) >= 0 && value.compareTo(max) <= 0, "Measurement is outside the supported range");
        return value.setScale(2, java.math.RoundingMode.HALF_UP);
    }
    public static String unit(String metric) { return "weight".equals(metric) ? "kg" : "cm"; }
    public static String key(String key, String fallback) {
        return key == null || key.isBlank() ? fallback : text(key, 128, "idempotencyKey");
    }
    public static String fingerprint(Object... values) {
        return RequestFingerprint.of(values);
    }
}
