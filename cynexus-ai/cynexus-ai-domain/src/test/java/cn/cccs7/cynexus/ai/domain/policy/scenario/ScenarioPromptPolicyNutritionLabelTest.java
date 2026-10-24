package cn.cccs7.cynexus.ai.domain.policy.scenario;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class ScenarioPromptPolicyNutritionLabelTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void nutritionLabelFallbackDefinesTheStructuredImageOutput() throws Exception {
        String prompt = ScenarioPromptPolicy.systemPrompt("food_nutrition_label_parse");
        var schema = MAPPER.readTree(ScenarioPromptPolicy.fallbackOutputSchema("food_nutrition_label_parse"));

        assertThat(prompt).contains("image index 0", "image index 1", "food or package photo", "nutrition label", "untrusted data", "servingSizeGrams");
        assertThat(schema.path("additionalProperties").asBoolean()).isFalse();
        assertThat(schema.path("required").toString()).contains("recognized", "nutrition", "confidence");
        assertThat(schema.path("properties").path("basis").path("enum").toString())
                .contains("per_100g", "per_serving", "unknown");
    }
}
