package cn.cccs7.cynexus.core.domain.workout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class WorkoutPolicyTest {
    @Test
    void acceptsUiMetricNamesAndNormalizesStableCodes() {
        assertThat(WorkoutPolicy.metric("shoulderWidth")).isEqualTo("shoulder_width");
        assertThat(WorkoutPolicy.metric("underChest")).isEqualTo("under_chest");
        assertThat(WorkoutPolicy.split("Chest + Triceps", "strength")).isEqualTo("chest_triceps");
    }

    @Test
    void enforcesCategorySplitAndPairedMeasurementRules() {
        assertThatThrownBy(() -> WorkoutPolicy.split("Shoulders", "cardio"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> WorkoutPolicy.side(null, "upper_arm"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> WorkoutPolicy.side("left", "waist"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(WorkoutPolicy.unit("weight")).isEqualTo("kg");
        assertThat(WorkoutPolicy.unit("waist")).isEqualTo("cm");
        assertThat(WorkoutPolicy.value(30.0, "shoulders")).isEqualByComparingTo("30.00");
        assertThatThrownBy(() -> WorkoutPolicy.value(0.9, "weight"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> WorkoutPolicy.value(4.9, "upper_arm"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
