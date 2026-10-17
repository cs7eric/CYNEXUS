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
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class ApplicationAnalysisRoutingDomainServiceImpl implements ApplicationAnalysisRoutingDomainService {
    private static final Set<String> STRUCTURED_CAPABILITIES = Set.of(
            "media_transcription_enrich", "post_metadata", "mp4_qa",
            "application_analysis", "application_recent30", "stock_selection_ai_intent_planning",
            "stock_selection_ai_screening", "market_event_factor_analysis", "market_factor_study_synthesis",
            "accounting_receipt_parse", "portfolio_trade_screenshot_parse", "portfolio_trade_screenshot_review",
            "wechat_image_route", "auction_analysis", "market_information_event_analysis",
            "market_information_daily_brief", "market_futures_position_analysis",
            "market_indicator_replay_explanation", "knowledge_base_document_generate", "knowledge_base_rag_answer");

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
        names.forEach(name -> models.add(providerModel(provider, name, binding)));
        fallbackProviders(binding, provider).forEach(fallback -> {
            Set<String> fallbackNames = new LinkedHashSet<>();
            add(fallbackNames, fallbackModel(binding, fallback));
            add(fallbackNames, fallback.getDefaultModel());
            fallbackNames.forEach(name -> models.add(providerModel(fallback, name, binding)));
        });
        return models;
    }

    /**
     * A capability can declare one distinct Provider fallback for outages such
     * as an upstream gateway timeout. Model fallbacks remain on the primary
     * Provider; this branch only runs after all of them have failed.
     */
    private List<AiProviderBO> fallbackProviders(AiUsageBindingBO binding, AiProviderBO primary) {
        Set<String> codes = new LinkedHashSet<>();
        addArray(codes, binding == null ? null : binding.getParams(), "fallback_provider_codes");
        if (codes.isEmpty()) add(codes, jsonText(binding == null ? null : binding.getParams(), "fallback_provider_code"));
        if (codes.isEmpty()) return List.of();
        List<AiProviderBO> providers = providerRepository.listAll();
        List<AiProviderBO> fallbacks = new ArrayList<>();
        for (String code : codes) {
            providers.stream()
                    .filter(item -> item != null && Boolean.TRUE.equals(item.getEnabled()))
                    .filter(item -> code.equalsIgnoreCase(normalize(item.getCode())))
                    .filter(item -> primary.getId() == null || !primary.getId().equals(item.getId()))
                    .findFirst()
                    .ifPresent(fallbacks::add);
        }
        return fallbacks;
    }

    private String fallbackModel(AiUsageBindingBO binding, AiProviderBO provider) {
        JsonNode values = readJson(binding == null ? null : binding.getParams()).path("fallback_provider_models");
        if (values.isObject()) {
            JsonNode value = values.get(normalize(provider.getCode()));
            if (value == null) {
                var fields = values.fields();
                while (fields.hasNext()) {
                    var entry = fields.next();
                    if (normalize(provider.getCode()).equalsIgnoreCase(normalize(entry.getKey()))) {
                        value = entry.getValue();
                        break;
                    }
                }
            }
            if (value != null && value.isValueNode()) return normalize(value.asText(""));
        }
        return jsonText(binding == null ? null : binding.getParams(), "fallback_model");
    }

    private AiModelBO providerModel(AiProviderBO provider, String modelName, AiUsageBindingBO binding) {
        String providerCode = normalize(provider.getProviderType()).isBlank()
                ? normalize(provider.getCode()) : normalize(provider.getProviderType());
        AiProviderModelPricingPolicy.ModelPricing pricing = AiProviderModelPricingPolicy.find(provider, modelName).orElse(null);
        return AiModelBO.builder().modelCode(modelName).providerCode(providerCode).modelName(modelName)
                .baseUrl(provider.getBaseUrl()).apiKey(provider.getApiKey()).apiKeyEnv(provider.getApiKeyEnv())
                .extra(mergeRuntimeExtra(provider.getExtra(), binding.getParams()))
                .inputPricePerMillion(pricing == null ? provider.getInputPricePerMillion() : pricing.inputPricePerMillion())
                .outputPricePerMillion(pricing == null ? provider.getOutputPricePerMillion() : pricing.outputPricePerMillion()).pricingCurrency(provider.getPricingCurrency())
                .capability(ModelCapability.builder()
                        .structuredOutputSupported(STRUCTURED_CAPABILITIES.contains(capabilityOf(binding)))
                        .visionSupported(Set.of("accounting_receipt_parse", "portfolio_trade_screenshot_parse",
                                "portfolio_trade_screenshot_review", "wechat_image_route").contains(capabilityOf(binding)))
                        .build()).enabled(true).build();
    }

    private String capabilityOf(AiUsageBindingBO binding) {
        return binding == null || binding.getCapability() == null ? "" : normalize(binding.getCapability());
    }

    /** Binding parameters are capability-scoped overrides of provider runtime options. */
    private String mergeRuntimeExtra(String providerExtra, String bindingParams) {
        ObjectNode merged = objectMapper.createObjectNode();
        mergeJsonObject(merged, providerExtra);
        mergeJsonObject(merged, bindingParams);
        return merged.toString();
    }

    private void mergeJsonObject(ObjectNode target, String rawJson) {
        JsonNode source = readJson(rawJson);
        if (!source.isObject()) return;
        source.fields().forEachRemaining(entry -> target.set(entry.getKey(), entry.getValue()));
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

    private String jsonText(String rawJson, String field) {
        JsonNode value = readJson(rawJson).get(field);
        return value == null || value.isNull() ? "" : normalize(value.asText(""));
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
