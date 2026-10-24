package cn.cccs7.cynexus.ai.domain.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FoodNutritionLabelOutputPolicyTest {

    @Test
    void canonicalizesCommonProviderAliasesAndNumericStrings() {
        String normalized = FoodNutritionLabelOutputPolicy.normalize("""
                {"name":"蛋黄派","brandName":"盼盼","basis":"per serving","servingSize":"20g",
                 "nutrition":{"energy":"411","carbohydrates":"11.8","protein":"1.9","fat":"4.8"},
                 "confidence":0.92,"extra":"discard"}
                """);

        assertThat(normalized).contains("\"foodName\":\"蛋黄派\"")
                .contains("\"brand\":\"盼盼\"")
                .contains("\"basis\":\"per_serving\"")
                .contains("\"caloriesKcal\":411")
                .contains("\"confidence\":\"high\"")
                .doesNotContain("extra");
        assertTrue(VisionOutputDomainPolicy.isValid("food_nutrition_label_parse", normalized));
    }

    @Test
    void fillsNullableFieldsWhenTheModelOnlyReturnsARecognizedName() {
        String normalized = FoodNutritionLabelOutputPolicy.normalize("""
                {"recognized":true,"foodName":"蛋黄派","brand":null,"basis":"unknown","nutrition":{},"confidence":"medium"}
                """);

        assertTrue(VisionOutputDomainPolicy.isValid("food_nutrition_label_parse", normalized));
        assertThat(normalized).contains("\"caloriesKcal\":null").contains("\"fatG\":null");
    }
}
