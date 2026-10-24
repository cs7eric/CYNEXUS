package cn.cccs7.cynexus.core.domain.food;

import java.math.BigDecimal;

/** Domain values for owner-scoped nutrition configuration. */
public final class NutritionConfigModels {
    private NutritionConfigModels() {}

    public record Config(
            String sex,
            BigDecimal bodyFatPct,
            Integer ageYears,
            BigDecimal activityFactor,
            BigDecimal calorieDeficit,
            BigDecimal carbsMultiplier,
            BigDecimal proteinMultiplier,
            BigDecimal fatMultiplier) {
        public static Config defaults() {
            return new Config(null, null, null, null,
                    BigDecimal.valueOf(500), BigDecimal.valueOf(3),
                    BigDecimal.valueOf(1.7), BigDecimal.valueOf(0.9));
        }
    }
}
