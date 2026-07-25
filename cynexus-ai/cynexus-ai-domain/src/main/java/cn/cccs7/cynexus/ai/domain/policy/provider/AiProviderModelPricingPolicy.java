package cn.cccs7.cynexus.ai.domain.policy.provider;

import cn.cccs7.cynexus.ai.domain.entity.AiProviderBO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Resolves model-specific pricing while preserving legacy provider-level prices. */
public final class AiProviderModelPricingPolicy {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private AiProviderModelPricingPolicy() {
    }

    public static List<ModelPricing> configuredModels(AiProviderBO provider) {
        if (provider == null) return List.of();
        String raw = provider.getModelConfigs();
        if (raw == null || raw.isBlank()) raw = provider.getModels();
        if (raw == null || raw.isBlank()) return List.of();
        try {
            JsonNode root = OBJECT_MAPPER.readTree(raw);
            if (!root.isArray()) return List.of();
            List<ModelPricing> result = new ArrayList<>();
            for (JsonNode item : root) {
                String model = item.isTextual() ? item.asText("").trim() : item.path("model").asText("").trim();
                if (model.isBlank()) continue;
                BigDecimal input = item.isObject()
                        ? decimal(item, "input_price_per_million", "inputPricePerMillion", provider.getInputPricePerMillion())
                        : provider.getInputPricePerMillion();
                BigDecimal output = item.isObject()
                        ? decimal(item, "output_price_per_million", "outputPricePerMillion", provider.getOutputPricePerMillion())
                        : provider.getOutputPricePerMillion();
                result.add(new ModelPricing(model, input, output));
            }
            return result;
        } catch (Exception ignored) {
            return List.of();
        }
    }

    public static Optional<ModelPricing> find(AiProviderBO provider, String model) {
        if (model == null || model.isBlank()) return Optional.empty();
        return configuredModels(provider).stream()
                .filter(item -> item.model().equalsIgnoreCase(model.trim()))
                .findFirst();
    }

    private static BigDecimal decimal(JsonNode item, String primary, String fallback, BigDecimal defaultValue) {
        JsonNode value = item.get(primary);
        if (value == null || value.isNull()) value = item.get(fallback);
        if (value == null || value.isNull()) return defaultValue;
        try {
            return new BigDecimal(value.asText());
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    public static final class ModelPricing {
        private final String model;
        private final BigDecimal inputPricePerMillion;
        private final BigDecimal outputPricePerMillion;

        public ModelPricing(String model, BigDecimal inputPricePerMillion, BigDecimal outputPricePerMillion) {
            this.model = model;
            this.inputPricePerMillion = inputPricePerMillion;
            this.outputPricePerMillion = outputPricePerMillion;
        }

        public String model() { return model; }
        public BigDecimal inputPricePerMillion() { return inputPricePerMillion; }
        public BigDecimal outputPricePerMillion() { return outputPricePerMillion; }
    }
}
