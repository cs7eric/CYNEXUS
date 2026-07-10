package cn.cccs7.cynexus.ai.application.service;

import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.entity.AiProviderBO;
import cn.cccs7.cynexus.ai.domain.entity.AiScenarioBO;
import cn.cccs7.cynexus.ai.domain.entity.AiUsageBindingBO;
import cn.cccs7.cynexus.ai.domain.repository.AiProviderRepository;
import cn.cccs7.cynexus.ai.domain.repository.AiUsageBindingRepository;
import cn.cccs7.cynexus.ai.domain.valueobject.ModelCapability;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class ApplicationAnalysisModelRouter {

    private static final String FALLBACK_MODEL = "mock-model";

    private final ModelRouteAppService modelRouteAppService;
    private final AiUsageBindingRepository aiUsageBindingRepository;
    private final AiProviderRepository aiProviderRepository;
    private final ObjectMapper objectMapper;

    public ApplicationAnalysisModelRouter(ModelRouteAppService modelRouteAppService,
                                          AiUsageBindingRepository aiUsageBindingRepository,
                                          AiProviderRepository aiProviderRepository) {
        this.modelRouteAppService = modelRouteAppService;
        this.aiUsageBindingRepository = aiUsageBindingRepository;
        this.aiProviderRepository = aiProviderRepository;
        this.objectMapper = new ObjectMapper();
    }

    public AiUsageBindingBO findBinding(String capability) {
        return aiUsageBindingRepository.listAll().stream()
                .filter(item -> capability.equals(item.getCapability()))
                .filter(item -> Boolean.TRUE.equals(item.getEnabled()))
                .findFirst()
                .orElse(null);
    }

    public List<AiModelBO> routeModels(String capability, String preferredModelCode) {
        AiUsageBindingBO binding = findBinding(capability);
        List<AiModelBO> models = routeByUsageBinding(binding, preferredModelCode);
        if (!models.isEmpty()) {
            return models;
        }
        try {
            return List.of(modelRouteAppService.route(buildScenario(capability), preferredModelCode));
        } catch (Exception ignored) {
            return List.of(mockModel());
        }
    }

    public String promptVersion(AiUsageBindingBO binding) {
        JsonNode params = readJson(binding == null ? null : binding.getParams());
        JsonNode value = params.get("prompt_version");
        return value == null || value.isNull() ? "" : normalize(value.asText(""));
    }

    private List<AiModelBO> routeByUsageBinding(AiUsageBindingBO binding, String preferredModelCode) {
        if (binding == null || binding.getProviderId() == null) {
            return List.of();
        }
        AiProviderBO provider = aiProviderRepository.findById(binding.getProviderId());
        if (provider == null || !Boolean.TRUE.equals(provider.getEnabled())) {
            return List.of();
        }
        Set<String> modelNames = new LinkedHashSet<>();
        addIfPresent(modelNames, preferredModelCode);
        addIfPresent(modelNames, binding.getModelOverride());
        addStringArray(modelNames, binding.getParams(), "fallback_chain");
        addStringArray(modelNames, binding.getParams(), "fallback_models");
        addIfPresent(modelNames, provider.getDefaultModel());

        List<AiModelBO> models = new ArrayList<>();
        for (String modelName : modelNames) {
            models.add(buildProviderModel(provider, modelName));
        }
        return models;
    }

    private AiScenarioBO buildScenario(String capability) {
        return AiScenarioBO.builder()
                .scenarioCode(capability)
                .scenarioName("Application Analysis " + capability)
                .defaultModelCode(FALLBACK_MODEL)
                .maxToolCalls(0)
                .toolCallRequired(false)
                .jsonOutputRequired(true)
                .enabled(true)
                .build();
    }

    private AiModelBO buildProviderModel(AiProviderBO provider, String modelName) {
        String providerCode = normalize(provider.getProviderType()).isBlank()
                ? normalize(provider.getCode())
                : normalize(provider.getProviderType());
        return AiModelBO.builder()
                .modelCode(modelName)
                .providerCode(providerCode)
                .modelName(modelName)
                .baseUrl(provider.getBaseUrl())
                .apiKey(provider.getApiKey())
                .apiKeyEnv(provider.getApiKeyEnv())
                .extra(provider.getExtra())
                .capability(ModelCapability.builder().structuredOutputSupported(true).build())
                .enabled(true)
                .build();
    }

    private AiModelBO mockModel() {
        return AiModelBO.builder()
                .modelCode(FALLBACK_MODEL)
                .providerCode("mock")
                .modelName("Mock Model")
                .capability(ModelCapability.builder().structuredOutputSupported(true).build())
                .enabled(true)
                .build();
    }

    private void addStringArray(Set<String> values, String rawJson, String field) {
        JsonNode array = readJson(rawJson).get(field);
        if (array == null || !array.isArray()) {
            return;
        }
        for (JsonNode item : array) {
            addIfPresent(values, item.asText(""));
        }
    }

    private void addIfPresent(Set<String> values, String value) {
        String normalized = normalize(value);
        if (!normalized.isBlank()) {
            values.add(normalized);
        }
    }

    private JsonNode readJson(String rawJson) {
        try {
            return rawJson == null || rawJson.isBlank()
                    ? objectMapper.createObjectNode()
                    : objectMapper.readTree(rawJson);
        } catch (Exception ignored) {
            return objectMapper.createObjectNode();
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
