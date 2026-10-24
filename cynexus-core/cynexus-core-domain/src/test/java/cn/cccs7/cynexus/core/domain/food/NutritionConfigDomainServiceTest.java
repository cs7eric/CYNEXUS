package cn.cccs7.cynexus.core.domain.food;

import cn.cccs7.cynexus.core.domain.food.NutritionConfigModels.Config;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NutritionConfigDomainServiceTest {
    @Test
    void returnsDefaultsWhenOwnerHasNoConfiguration() {
        NutritionConfigRepository repository = repository(null);
        Config value = new NutritionConfigDomainService(repository).get("owner-a");

        assertThat(value.calorieDeficit()).isEqualByComparingTo("500");
        assertThat(value.carbsMultiplier()).isEqualByComparingTo("3");
        assertThat(value.proteinMultiplier()).isEqualByComparingTo("1.7");
        assertThat(value.fatMultiplier()).isEqualByComparingTo("0.9");
    }

    @Test
    void validatesInputsAndPersistsNormalizedValues() {
        AtomicReference<Config> saved = new AtomicReference<>();
        NutritionConfigRepository repository = repository(saved);
        Config result = new NutritionConfigDomainService(repository).save("owner-a",
                new Config(" MALE ", new BigDecimal("18.0"), 30, new BigDecimal("1.55"),
                        new BigDecimal("500"), new BigDecimal("3.0"), new BigDecimal("1.7"), new BigDecimal("0.9")));

        assertThat(saved.get()).isEqualTo(result);
        assertThat(result.sex()).isEqualTo("male");
        assertThat(result.bodyFatPct()).isEqualByComparingTo("18");
    }

    @Test
    void rejectsOutOfRangeActivity() {
        NutritionConfigRepository repository = repository(null);
        assertThatThrownBy(() -> new NutritionConfigDomainService(repository).save("owner-a",
                new Config("male", null, null, new BigDecimal("3"), null, null, null, null)))
                .hasMessageContaining("activityFactor");
    }

    private static NutritionConfigRepository repository(AtomicReference<Config> saved) {
        return new NutritionConfigRepository() {
            @Override public void lockOwner(String owner) {}
            @Override public Config find(String owner) { return saved == null ? null : saved.get(); }
            @Override public Config upsert(String owner, Config config) { if (saved != null) saved.set(config); return config; }
            @Override public void delete(String owner) {}
        };
    }
}
