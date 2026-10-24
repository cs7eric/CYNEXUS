package cn.cccs7.cynexus.core.domain.food;

import cn.cccs7.cynexus.core.domain.food.NutritionConfigModels.Config;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;

/** Validates nutrition rules and delegates persistence to the domain port. */
@Service
public class NutritionConfigDomainService {
    private static final BigDecimal MIN_ACTIVITY = BigDecimal.valueOf(1.1);
    private static final BigDecimal MAX_ACTIVITY = BigDecimal.valueOf(2.5);
    private static final BigDecimal MAX_MULTIPLIER = BigDecimal.valueOf(20);
    private final NutritionConfigRepository repository;

    public NutritionConfigDomainService(NutritionConfigRepository repository) {
        this.repository = repository;
    }

    public Config get(String owner) {
        owner(owner);
        Config stored = repository.find(owner);
        return stored == null ? Config.defaults() : withDefaults(stored);
    }

    public Config save(String owner, Config requested) {
        owner(owner);
        if (requested == null) throw new IllegalArgumentException("Nutrition configuration is required");
        Config normalized = normalize(requested);
        repository.lockOwner(owner);
        return repository.upsert(owner, normalized);
    }

    public void delete(String owner) {
        owner(owner);
        repository.lockOwner(owner);
        repository.delete(owner);
    }

    private Config normalize(Config value) {
        String sex = value.sex();
        if (sex != null) {
            sex = sex.trim().toLowerCase(java.util.Locale.ROOT);
            if (!sex.equals("male") && !sex.equals("female")) {
                throw new IllegalArgumentException("sex must be male or female");
            }
        }
        BigDecimal bodyFat = bounded(value.bodyFatPct(), BigDecimal.ZERO, BigDecimal.valueOf(100), "bodyFatPct");
        Integer age = value.ageYears();
        if (age != null && (age < 1 || age > 120)) throw new IllegalArgumentException("ageYears must be between 1 and 120");
        BigDecimal activity = bounded(value.activityFactor(), MIN_ACTIVITY, MAX_ACTIVITY, "activityFactor");
        BigDecimal deficit = bounded(value.calorieDeficit(), BigDecimal.ZERO, BigDecimal.valueOf(5000), "calorieDeficit");
        Config defaults = Config.defaults();
        BigDecimal carbs = multiplier(value.carbsMultiplier() == null ? defaults.carbsMultiplier() : value.carbsMultiplier(), "carbsMultiplier");
        BigDecimal protein = multiplier(value.proteinMultiplier() == null ? defaults.proteinMultiplier() : value.proteinMultiplier(), "proteinMultiplier");
        BigDecimal fat = multiplier(value.fatMultiplier() == null ? defaults.fatMultiplier() : value.fatMultiplier(), "fatMultiplier");
        return new Config(sex, bodyFat, age, activity,
                value.calorieDeficit() == null ? defaults.calorieDeficit() : deficit,
                carbs, protein, fat);
    }

    private Config withDefaults(Config value) {
        Config defaults = Config.defaults();
        return new Config(value.sex(), value.bodyFatPct(), value.ageYears(), value.activityFactor(),
                value.calorieDeficit() == null ? defaults.calorieDeficit() : value.calorieDeficit(),
                value.carbsMultiplier() == null ? defaults.carbsMultiplier() : value.carbsMultiplier(),
                value.proteinMultiplier() == null ? defaults.proteinMultiplier() : value.proteinMultiplier(),
                value.fatMultiplier() == null ? defaults.fatMultiplier() : value.fatMultiplier());
    }

    private static BigDecimal bounded(BigDecimal value, BigDecimal min, BigDecimal max, String name) {
        if (value == null) return null;
        if (value.compareTo(min) < 0 || value.compareTo(max) > 0) throw new IllegalArgumentException(name + " is out of range");
        return value.stripTrailingZeros();
    }

    private static BigDecimal multiplier(BigDecimal value, String name) {
        if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(MAX_MULTIPLIER) > 0) throw new IllegalArgumentException(name + " must be between 0 and 20");
        return value.stripTrailingZeros();
    }

    private static void owner(String owner) {
        if (owner == null || owner.isBlank() || owner.length() > 512) throw new IllegalArgumentException("owner is required");
    }
}
