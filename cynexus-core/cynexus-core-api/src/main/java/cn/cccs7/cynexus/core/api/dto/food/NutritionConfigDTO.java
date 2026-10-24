package cn.cccs7.cynexus.core.api.dto.food;

import java.math.BigDecimal;

/** Owner-scoped nutrition configuration and the rules used to calculate daily targets. */
public final class NutritionConfigDTO {
    private NutritionConfigDTO() {}

    public record Config(
            String sex,
            BigDecimal bodyFatPct,
            Integer ageYears,
            BigDecimal activityFactor,
            BigDecimal calorieDeficit,
            BigDecimal carbsMultiplier,
            BigDecimal proteinMultiplier,
            BigDecimal fatMultiplier) {}

    public record SaveRequest(
            String sex,
            BigDecimal bodyFatPct,
            Integer ageYears,
            BigDecimal activityFactor,
            BigDecimal calorieDeficit,
            BigDecimal carbsMultiplier,
            BigDecimal proteinMultiplier,
            BigDecimal fatMultiplier) {}
}
