package cn.cccs7.cynexus.ai.domain.applicationanalysis.impl;

import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.entity.AiProviderBO;
import cn.cccs7.cynexus.ai.domain.entity.AiScenarioBO;
import cn.cccs7.cynexus.ai.domain.entity.AiUsageBindingBO;
import cn.cccs7.cynexus.ai.domain.policy.provider.AiProviderModelPricingPolicy;
import cn.cccs7.cynexus.ai.domain.repository.AiModelRepository;
import cn.cccs7.cynexus.ai.domain.repository.AiProviderRepository;
import cn.cccs7.cynexus.ai.domain.repository.AiUsageBindingRepository;
import cn.cccs7.cynexus.ai.domain.applicationanalysis.ApplicationAnalysisRoutingDomainService;
import cn.cccs7.cynexus.ai.domain.routing.ModelRoutingDomainService;
import cn.cccs7.cynexus.ai.domain.valueobject.ModelCapability;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class ApplicationAnalysisRoutingDomainServiceImpl implements ApplicationAnalysisRoutingDomainService {

    private final AiModelRepository modelRepository;
    private final AiUsageBindingRepository bindingRepository;
    private final AiProviderRepository providerRepository;
    private final ModelRoutingDomainService routingService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ApplicationAnalysisRoutingDomainServiceImpl(AiModelRepository modelRepository,
                                                       AiUsageBindingRepository bindingRepository,
                                                       AiProviderRepository providerRepository,
                                                       ModelRoutingDomainService routingService) {
        this.modelRepository = modelRepository;
        this.bindingRepository = bindingRepository;
        this.providerRepository = providerRepository;
        this.routingService = routingService;
    }

    @Override
    public AiUsageBindingBO findBinding(String capability) {
        return bindingRepository.listAll().stream()
                .filter(item -> capability.equals(item.getCapability()))
                .filter(item -> Boolean.TRUE.equals(item.getEnabled()))
                .findFirst()
                .orElse(null);
    }

    @Override
    public List<AiModelBO> routeModels(String capability, String preferredModelCode) {
        List<AiModelBO> models = routeByBinding(findBinding(capability), preferredModelCode);
        if (!models.isEmpty()) {
            return models;
        }
        AiScenarioBO scenario = AiScenarioBO.builder()
                .scenarioCode(capability).scenarioName("Application Analysis " + capability)
                .defaultModelCode(null).maxToolCalls(0).toolCallRequired(false)
                .jsonOutputRequired(true).enabled(true).build();
        return List.of(routingService.route(scenario, modelRepository.findEnabledModels(), preferredModelCode));
    }

    @Override
    public String promptVersion(AiUsageBindingBO binding) {
        JsonNode value = readJson(binding == null ? null : binding.getParams()).get("prompt_version");
        return value == null || value.isNull() ? "" : normalize(value.asText(""));
    }

    private List<AiModelBO> routeByBinding(AiUsageBindingBO binding, String preferredModelCode) {
        if (binding == null || binding.getProviderId() == null) {
            return List.of();
        }
        AiProviderBO provider = providerRepository.findById(binding.getProviderId());
        if (provider == null || !Boolean.TRUE.equals(provider.getEnabled())) {
            return List.of();
        }
        Set<String> names = new LinkedHashSet<>();
        add(names, preferredModelCode);
        add(names, binding.getModelOverride());
        addArray(names, binding.getParams(), "fallback_chain");
        addArray(names, binding.getParams(), "fallback_models");
        add(names, provider.getDefaultModel());
        List<AiModelBO> models = new ArrayList<>();
        names.forEach(name -> models.add(providerModel(provider, name)));
        return models;
    }

    private AiModelBO providerModel(AiProviderBO provider, String modelName) {
        String providerCode = normalize(provider.getProviderType()).isBlank()
                ? normalize(provider.getCode()) : normalize(provider.getProviderType());
        AiProviderModelPricingPolicy.ModelPricing pricing = AiProviderModelPricingPolicy.find(provider, modelName).orElse(null);
        return AiModelBO.builder().modelCode(modelName).providerCode(providerCode).modelName(modelName)
                .baseUrl(provider.getBaseUrl()).apiKey(provider.getApiKey()).apiKeyEnv(provider.getApiKeyEnv())
                .extra(provider.getExtra()).inputPricePerMillion(pricing == null ? provider.getInputPricePerMillion() : pricing.inputPricePerMillion())
                .outputPricePerMillion(pricing == null ? provider.getOutputPricePerMillion() : pricing.outputPricePerMillion()).pricingCurrency(provider.getPricingCurrency())
                .capability(ModelCapability.builder().structuredOutputSupported(true).build()).enabled(true).build();
    }

    private void addArray(Set<String> values, String rawJson, String field) {
        JsonNode array = readJson(rawJson).get(field);
        if (array != null && array.isArray()) {
            array.forEach(item -> add(values, item.asText("")));
        }
    }

    private void add(Set<String> values, String value) {
        String normalized = normalize(value);
        if (!normalized.isBlank()) {
            values.add(normalized);
        }
    }

    private JsonNode readJson(String rawJson) {
        try {
            return rawJson == null || rawJson.isBlank() ? objectMapper.createObjectNode() : objectMapper.readTree(rawJson);
        } catch (Exception ignored) {
            return objectMapper.createObjectNode();
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
