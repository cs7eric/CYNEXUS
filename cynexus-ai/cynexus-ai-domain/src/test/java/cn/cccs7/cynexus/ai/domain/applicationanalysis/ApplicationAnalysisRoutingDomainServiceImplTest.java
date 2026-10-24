package cn.cccs7.cynexus.ai.domain.applicationanalysis;

import static org.assertj.core.api.Assertions.assertThat;

import cn.cccs7.cynexus.ai.domain.applicationanalysis.impl.ApplicationAnalysisRoutingDomainServiceImpl;
import cn.cccs7.cynexus.ai.domain.entity.AiProviderBO;
import cn.cccs7.cynexus.ai.domain.entity.AiUsageBindingBO;
import cn.cccs7.cynexus.ai.domain.repository.AiModelRepository;
import cn.cccs7.cynexus.ai.domain.repository.AiProviderRepository;
import cn.cccs7.cynexus.ai.domain.repository.AiUsageBindingRepository;
import cn.cccs7.cynexus.ai.domain.routing.ModelRoutingDomainService;
import cn.cccs7.cynexus.ai.domain.entity.AiScenarioBO;
import cn.cccs7.cynexus.common.core.exception.BizException;
import java.util.List;
import org.junit.jupiter.api.Test;

class ApplicationAnalysisRoutingDomainServiceImplTest {

    @Test
    void mergesCapabilityRuntimeOptionsOverProviderOptions() {
        AiUsageBindingBO binding = AiUsageBindingBO.builder().capability("stock_selection_ai_screening")
                .providerId(21L).enabled(true).modelOverride("gpt-5.4-mini")
                .params("{\"request_timeout_seconds\":180,\"response_format\":\"json_object\"}").build();
        AiProviderBO provider = AiProviderBO.builder().id(21L).code("xhhai").providerType("openai_compatible")
                .defaultModel("gpt-5.4-mini").enabled(true)
                .extra("{\"request_timeout_seconds\":90,\"wire_api\":\"chat_completions\"}").build();
        var service = new ApplicationAnalysisRoutingDomainServiceImpl(new ModelRepository(),
                new BindingRepository(binding), new ProviderRepository(provider), new ModelRouter());

        var model = service.routeModels("stock_selection_ai_screening", null).getFirst();

        assertThat(model.getExtra()).contains("\"request_timeout_seconds\":180")
                .contains("\"response_format\":\"json_object\"")
                .contains("\"wire_api\":\"chat_completions\"");
    }

    @Test
    void appendsConfiguredFallbackProviderAfterPrimaryProviderModels() {
        AiUsageBindingBO binding = AiUsageBindingBO.builder().capability("stock_selection_ai_screening")
                .providerId(21L).enabled(true).modelOverride("primary-model")
                .params("{\"fallback_models\":[\"primary-default\"],\"fallback_provider_code\":\"deepseek\",\"fallback_model\":\"deepseek-v4-flash\"}")
                .build();
        AiProviderBO primary = AiProviderBO.builder().id(21L).code("xhhai").providerType("openai_compatible")
                .defaultModel("primary-default").enabled(true).build();
        AiProviderBO fallback = AiProviderBO.builder().id(22L).code("deepseek").providerType("deepseek")
                .defaultModel("deepseek-v4-flash").enabled(true).build();
        var service = new ApplicationAnalysisRoutingDomainServiceImpl(new ModelRepository(),
                new BindingRepository(binding), new ProviderRepository(List.of(primary, fallback)), new ModelRouter());

        var models = service.routeModels("stock_selection_ai_screening", null);

        assertThat(models).extracting(item -> item.getProviderCode() + "/" + item.getModelCode())
                .containsExactly("openai_compatible/primary-model", "openai_compatible/primary-default", "deepseek/deepseek-v4-flash");
    }

    @Test
    void selectsTheNextIndependentFallbackWhenTheUserChoosesAConfiguredFallbackProvider() {
        AiUsageBindingBO binding = AiUsageBindingBO.builder().capability("stock_selection_ai_screening")
                .providerId(21L).enabled(true).modelOverride("gpt-5.4-mini")
                .params("{\"fallback_provider_codes\":[\"custom\",\"xhhai\"],\"fallback_provider_models\":{\"custom\":\"gpt-5.4\",\"xhhai\":\"gpt-5.4-mini\"}}")
                .build();
        AiProviderBO primary = AiProviderBO.builder().id(21L).code("xhhai").providerType("openai_compatible")
                .defaultModel("gpt-5.4-mini").enabled(true).build();
        AiProviderBO fallback = AiProviderBO.builder().id(22L).code("custom").providerType("custom")
                .defaultModel("gpt-5.4-mini").enabled(true).build();
        var service = new ApplicationAnalysisRoutingDomainServiceImpl(new ModelRepository(),
                new BindingRepository(binding), new ProviderRepository(List.of(primary, fallback)), new ModelRouter());

        var models = service.routeModels("stock_selection_ai_screening", null);

        assertThat(models).extracting(item -> item.getProviderCode() + "/" + item.getModelCode())
                .containsExactly("openai_compatible/gpt-5.4-mini", "custom/gpt-5.4", "custom/gpt-5.4-mini");
    }

    @Test
    void bindingModelOverrideWinsOverRequestPreference() {
        AiUsageBindingBO binding = AiUsageBindingBO.builder().capability("food_fridge_inventory")
                .providerId(21L).enabled(true).modelOverride("gpt-6.1-sol").build();
        AiProviderBO provider = AiProviderBO.builder().id(21L).code("xtyoo").providerType("xtyoo")
                .defaultModel("gpt-6.1-sol").enabled(true).build();
        var service = new ApplicationAnalysisRoutingDomainServiceImpl(new ModelRepository(),
                new BindingRepository(binding), new ProviderRepository(provider), new ModelRouter());

        var models = service.routeModels("food_fridge_inventory", "another-model");

        assertThat(models).extracting(item -> item.getProviderCode() + "/" + item.getModelCode())
                .startsWith("xtyoo/gpt-6.1-sol");
    }

    @Test
    void fridgeInventoryDoesNotSilentlyFallBackToAnUnboundProvider() {
        var service = new ApplicationAnalysisRoutingDomainServiceImpl(new ModelRepository(),
                new BindingRepository(null), new ProviderRepository(List.of()), new ModelRouter());

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.routeModels("food_fridge_inventory", null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("food fridge inventory");
    }

    @Test
    void fridgeNutritionMatchingUsesItsStructuredBindingAndDoesNotUseGenericFallback() {
        AiUsageBindingBO binding = AiUsageBindingBO.builder().capability("food_fridge_nutrition_match")
                .providerId(31L).enabled(true).modelOverride("gpt-6.1-sol").build();
        AiProviderBO provider = AiProviderBO.builder().id(31L).code("xtyoo").providerType("xtyoo")
                .defaultModel("gpt-6.1-sol").enabled(true).build();
        var service = new ApplicationAnalysisRoutingDomainServiceImpl(new ModelRepository(),
                new BindingRepository(binding), new ProviderRepository(provider), new ModelRouter());

        var model = service.routeModels("food_fridge_nutrition_match", "other-model").getFirst();

        assertThat(model.getProviderCode()).isEqualTo("xtyoo");
        assertThat(model.getModelCode()).isEqualTo("gpt-6.1-sol");
        assertThat(model.getCapability().isStructuredOutputSupported()).isTrue();
        var unbound = new ApplicationAnalysisRoutingDomainServiceImpl(new ModelRepository(),
                new BindingRepository(null), new ProviderRepository(List.of()), new ModelRouter());
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> unbound.routeModels("food_fridge_nutrition_match", null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("food fridge nutrition matching");
    }

    @Test
    void foodNutritionLabelUsesItsBoundVisionModel() {
        AiUsageBindingBO binding = AiUsageBindingBO.builder().capability("food_nutrition_label_parse")
                .providerId(41L).enabled(true).modelOverride("gpt-6.1-sol").build();
        AiProviderBO provider = AiProviderBO.builder().id(41L).code("xtyoo").providerType("xtyoo")
                .defaultModel("gpt-6.1-sol").enabled(true).build();
        var service = new ApplicationAnalysisRoutingDomainServiceImpl(new ModelRepository(),
                new BindingRepository(binding), new ProviderRepository(provider), new ModelRouter());

        var model = service.routeModels("food_nutrition_label_parse", null).getFirst();

        assertThat(model.getProviderCode()).isEqualTo("xtyoo");
        assertThat(model.getModelCode()).isEqualTo("gpt-6.1-sol");
        assertThat(model.getCapability().isVisionSupported()).isTrue();
        assertThat(model.getCapability().isStructuredOutputSupported()).isTrue();
    }

    @Test
    void foodNutritionLabelDoesNotSilentlyUseAnUnboundProvider() {
        var service = new ApplicationAnalysisRoutingDomainServiceImpl(new ModelRepository(),
                new BindingRepository(null), new ProviderRepository(List.of()), new ModelRouter());

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.routeModels("food_nutrition_label_parse", null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("food nutrition label recognition");
    }

    private static final class ModelRepository implements AiModelRepository {
        @Override public List<cn.cccs7.cynexus.ai.domain.entity.AiModelBO> findEnabledModels() { return List.of(); }
    }

    private static final class BindingRepository implements AiUsageBindingRepository {
        private final AiUsageBindingBO binding;
        private BindingRepository(AiUsageBindingBO binding) { this.binding = binding; }
        @Override public List<AiUsageBindingBO> listAll() { return binding == null ? List.of() : List.of(binding); }
        @Override public AiUsageBindingBO findById(Long id) { return binding; }
        @Override public Long create(AiUsageBindingBO value) { return 1L; }
        @Override public boolean update(AiUsageBindingBO value) { return true; }
        @Override public boolean delete(Long id) { return true; }
    }

    private static final class ProviderRepository implements AiProviderRepository {
        private final List<AiProviderBO> providers;
        private ProviderRepository(AiProviderBO provider) { this(List.of(provider)); }
        private ProviderRepository(List<AiProviderBO> providers) { this.providers = providers; }
        @Override public List<AiProviderBO> listAll() { return providers; }
        @Override public AiProviderBO findById(Long id) {
            return providers.stream().filter(item -> item.getId().equals(id)).findFirst().orElse(null);
        }
        @Override public Long create(AiProviderBO value) { return 1L; }
        @Override public boolean update(AiProviderBO value) { return true; }
        @Override public boolean delete(Long id) { return true; }
    }

    private static final class ModelRouter implements ModelRoutingDomainService {
        @Override
        public cn.cccs7.cynexus.ai.domain.entity.AiModelBO route(AiScenarioBO scenario,
                                                                   List<cn.cccs7.cynexus.ai.domain.entity.AiModelBO> candidates,
                                                                   String preferredModelCode) {
            throw new BizException(cn.cccs7.cynexus.common.core.exception.ErrorCode.BAD_REQUEST, "not used");
        }
    }
}
