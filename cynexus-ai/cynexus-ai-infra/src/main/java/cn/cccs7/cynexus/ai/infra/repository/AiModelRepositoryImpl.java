package cn.cccs7.cynexus.ai.infra.repository;

import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.repository.AiModelRepository;
import cn.cccs7.cynexus.ai.domain.entity.AiProviderBO;
import cn.cccs7.cynexus.ai.domain.policy.provider.AiProviderModelPricingPolicy;
import cn.cccs7.cynexus.ai.domain.valueobject.ModelCapability;
import cn.cccs7.cynexus.ai.domain.valueobject.ModelRuntimeOptions;
import cn.cccs7.cynexus.ai.infra.basic.service.AiProviderBasicService;
import cn.cccs7.cynexus.ai.infra.convert.AiProviderPOConverter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Repository;

@Repository
public class AiModelRepositoryImpl implements AiModelRepository {

    private final AiProviderBasicService aiProviderBasicService;

    public AiModelRepositoryImpl(AiProviderBasicService aiProviderBasicService) {
        this.aiProviderBasicService = aiProviderBasicService;
    }

    @Override
    public List<AiModelBO> findEnabledModels() {
        List<AiModelBO> models = new ArrayList<>();
        for (var providerPo : aiProviderBasicService.listAll()) {
            if (providerPo == null || !Boolean.TRUE.equals(providerPo.getEnabled()) || providerPo.getDeletedAt() != null) {
                continue;
            }
            AiProviderBO provider = AiProviderPOConverter.INSTANCE.toTarget(providerPo);
            Set<String> names = new LinkedHashSet<>();
            AiProviderModelPricingPolicy.configuredModels(provider).forEach(pricing -> names.add(pricing.model()));
            if (provider.getDefaultModel() != null && !provider.getDefaultModel().isBlank()) {
                names.add(provider.getDefaultModel().trim());
            }
            String providerCode = provider.getProviderType() == null || provider.getProviderType().isBlank()
                    ? provider.getCode() : provider.getProviderType();
            for (String modelName : names) {
                var pricing = AiProviderModelPricingPolicy.find(provider, modelName).orElse(null);
                models.add(AiModelBO.builder()
                        .modelCode(modelName)
                        .providerCode(providerCode)
                        .providerIdentity(provider.getCode())
                        .modelName(modelName)
                        .baseUrl(provider.getBaseUrl())
                        .apiKey(provider.getApiKey())
                        .apiKeyEnv(provider.getApiKeyEnv())
                        .extra(provider.getExtra())
                        .inputPricePerMillion(pricing == null ? provider.getInputPricePerMillion() : pricing.inputPricePerMillion())
                        .outputPricePerMillion(pricing == null ? provider.getOutputPricePerMillion() : pricing.outputPricePerMillion())
                        .pricingCurrency(provider.getPricingCurrency())
                        .capability(new ModelCapability(true, true, false))
                        .runtimeOptions(new ModelRuntimeOptions(null, null, null))
                        .enabled(true)
                        .build());
            }
        }
        return models;
    }
}
