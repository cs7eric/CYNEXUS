package cn.cccs7.cynexus.ai.domain.service.scenario;

import static org.assertj.core.api.Assertions.assertThat;

import cn.cccs7.cynexus.ai.domain.applicationanalysis.ApplicationAnalysisRoutingDomainService;
import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.entity.AiPromptBO;
import cn.cccs7.cynexus.ai.domain.entity.AiUsageBindingBO;
import cn.cccs7.cynexus.ai.domain.port.RepositoryAiPromptRuntimeResolver;
import cn.cccs7.cynexus.ai.domain.repository.AiPromptRepository;
import cn.cccs7.cynexus.ai.domain.service.routing.ModelRouteDomainService;
import cn.cccs7.cynexus.ai.domain.validation.OutputValidationDomainService;
import cn.cccs7.cynexus.ai.domain.validation.OutputValidationDomainServiceImpl;
import cn.cccs7.cynexus.ai.domain.valueobject.ModelCapability;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ScenarioExecutionDomainServiceTest {

    @Test
    void selectsHighestActivePromptVersionWhenBindingIsUnavailable() {
        AiPromptBO v1 = AiPromptBO.builder().promptKey("food_fridge_inventory").promptVersion("v1")
                .status("active").systemPrompt("old").userPromptTemplate("{{user_input}}").build();
        AiPromptBO v2 = AiPromptBO.builder().promptKey("food_fridge_inventory").promptVersion("v2")
                .status("active").systemPrompt("current").userPromptTemplate("{{user_input}}").build();
        AiPromptRepository prompts = new AiPromptRepository() {
            @Override public List<AiPromptBO> list(String keyword, String scenarioCode, String status) { return List.of(v1, v2); }
            @Override public AiPromptBO findById(Long id) { return null; }
            @Override public Long create(AiPromptBO value) { return null; }
            @Override public boolean update(AiPromptBO value) { return false; }
            @Override public boolean delete(Long id) { return false; }
        };
        var resolver = new RepositoryAiPromptRuntimeResolver(new Routing(null, (AiModelBO) null), prompts);

        var resolved = resolver.resolve("food_fridge_inventory", "food_fridge_inventory", "fallback", "{{user_input}}");

        assertThat(resolved.promptVersion()).isEqualTo("v2");
        assertThat(resolved.systemPrompt()).isEqualTo("current");
    }

    @Test
    void routesTheFirstEnabledBindingAndResolvesItsModel() {
        AiUsageBindingBO binding = AiUsageBindingBO.builder()
                .capability("stock_information_analysis").enabled(true).providerId(1L).build();
        AiModelBO model = AiModelBO.builder()
                .modelCode("gpt").enabled(true)
                .capability(ModelCapability.builder().structuredOutputSupported(true).build())
                .build();
        ScenarioExecutionDomainService service = new ScenarioExecutionDomainServiceImpl(
                new Routing(binding, model), emptyPromptRepository(), unusedModelRoute(), new OutputValidationDomainServiceImpl());

        assertThat(service.routeInformationModel(
                null, List.of("missing", "stock_information_analysis"), false)).isSameAs(model);
    }

    @Test
    void prefersActiveBoundPromptAndSubstitutesCandidateContext() {
        AiUsageBindingBO binding = AiUsageBindingBO.builder()
                .capability("accounting_receipt_parse").enabled(true).promptId(9L).build();
        AiPromptBO prompt = AiPromptBO.builder()
                .status("active").systemPrompt("system")
                .userPromptTemplate("candidate={{candidate_context}}")
                .build();
        ScenarioExecutionDomainService service = new ScenarioExecutionDomainServiceImpl(
                new Routing(binding, (AiModelBO) null), promptRepository(prompt), unusedModelRoute(), new OutputValidationDomainServiceImpl());

        assertThat(service.resolveVisionPrompt("accounting_receipt_parse", "receipt candidates"))
                .isEqualTo("system" + System.lineSeparator() + "candidate=receipt candidates");
    }

    @Test
    void preparesPortfolioScreenshotScenarioWithDedicatedCapability() {
        AiModelBO model = AiModelBO.builder()
                .modelCode("vision-model").enabled(true)
                .capability(ModelCapability.builder().visionSupported(true).structuredOutputSupported(true).build())
                .build();
        ScenarioExecutionDomainService service = new ScenarioExecutionDomainServiceImpl(
                new Routing(AiUsageBindingBO.builder().capability("portfolio_trade_screenshot_parse").enabled(true).build(), model),
                emptyPromptRepository(), unusedModelRoute(), new OutputValidationDomainServiceImpl());

        var prepared = service.prepareVisionExecution("portfolio_trade_screenshot_parse", null, "image metadata");

        assertThat(prepared.getScenario().getScenarioCode()).isEqualTo("portfolio_trade_screenshot_parse");
        assertThat(prepared.getScenario().getScenarioName()).contains("Portfolio");
        assertThat(prepared.getCapability()).isEqualTo("portfolio_trade_screenshot_parse");
        assertThat(prepared.getModel()).isSameAs(model);
        assertThat(prepared.getExecutionNo()).startsWith("AI-");
        var result = service.finalizeVisionExecution(prepared, "{\"isTradeScreenshot\":false,\"candidates\":[]}");
        assertThat(result.getExecutionNo()).isEqualTo(prepared.getExecutionNo());
    }

    @Test
    void retainsFallbackModelsForVisionExecution() {
        AiModelBO primary = AiModelBO.builder().modelCode("primary").enabled(true).build();
        AiModelBO fallback = AiModelBO.builder().modelCode("fallback").enabled(true).build();
        ScenarioExecutionDomainService service = new ScenarioExecutionDomainServiceImpl(
                new Routing(AiUsageBindingBO.builder().capability("portfolio_trade_screenshot_parse").enabled(true).build(),
                        List.of(primary, fallback)),
                emptyPromptRepository(), unusedModelRoute(), new OutputValidationDomainServiceImpl());

        var prepared = service.prepareVisionExecution("portfolio_trade_screenshot_parse", null, "image metadata");

        assertThat(prepared.candidateModels()).containsExactly(primary, fallback);
    }

    @Test
    void usesPortfolioFallbackPromptWhenBoundPromptIsUnavailable() {
        ScenarioExecutionDomainService service = new ScenarioExecutionDomainServiceImpl(
                new Routing(null, (AiModelBO) null), emptyPromptRepository(), unusedModelRoute(), new OutputValidationDomainServiceImpl());

        String prompt = service.resolveVisionPrompt("portfolio_trade_screenshot_parse", "candidate context");

        assertThat(prompt).contains("个人持仓交易截图解析器");
        assertThat(prompt).contains("isTradeScreenshot");
        assertThat(prompt).contains("BUY").contains("SELL");
        assertThat(prompt).contains("ETF").contains("历史成交");
        assertThat(prompt).contains("instrumentType").contains("STOCK");
        assertThat(prompt).contains("共享日期").contains("HH:mm:ss");
        assertThat(prompt).contains("candidate context");
        assertThat(prompt).doesNotContain("transactionType");
    }

    @Test
    void preparesWechatImageRouteScenarioWithDedicatedCapability() {
        AiModelBO model = AiModelBO.builder()
                .modelCode("vision-model").enabled(true)
                .capability(ModelCapability.builder().visionSupported(true).structuredOutputSupported(true).build())
                .build();
        ScenarioExecutionDomainService service = new ScenarioExecutionDomainServiceImpl(
                new Routing(AiUsageBindingBO.builder().capability("wechat_image_route").enabled(true).build(), model),
                emptyPromptRepository(), unusedModelRoute(), new OutputValidationDomainServiceImpl());

        var prepared = service.prepareVisionExecution("wechat_image_route", null, "image metadata");

        assertThat(prepared.getScenario().getScenarioCode()).isEqualTo("wechat_image_route");
        assertThat(prepared.getScenario().getScenarioName()).contains("WeChat");
        assertThat(prepared.getCapability()).isEqualTo("wechat_image_route");
        assertThat(prepared.getModel()).isSameAs(model);
    }

    @Test
    void preparesFridgeInventoryThroughItsBoundCapability() {
        AiModelBO model = AiModelBO.builder()
                .modelCode("gpt-6.1-sol").enabled(true)
                .capability(ModelCapability.builder().structuredOutputSupported(true).build())
                .build();
        ScenarioExecutionDomainService service = new ScenarioExecutionDomainServiceImpl(
                new Routing(AiUsageBindingBO.builder().capability("food_fridge_inventory").enabled(true).build(), model),
                emptyPromptRepository(), unusedModelRoute(), new OutputValidationDomainServiceImpl());

        var prepared = service.prepareTextExecution("food_fridge_inventory", null, "two bottles of milk", Map.of(), List.of());

        assertThat(prepared.getModel()).isSameAs(model);
        assertThat(prepared.getPrompt()).contains("fridge inventory extraction assistant");
        assertThat(prepared.getPromptKey()).isEqualTo("food_fridge_inventory");
        assertThat(prepared.getToolCodes()).isEmpty();
    }

    @Test
    void preparesFridgeNutritionMatchThroughItsBoundPromptAndCandidateInput() {
        AiModelBO model = AiModelBO.builder()
                .modelCode("gpt-6.1-sol").enabled(true)
                .capability(ModelCapability.builder().structuredOutputSupported(true).build())
                .build();
        String outputSchema = "{\"type\":\"object\",\"required\":[\"matches\"]}";
        AiUsageBindingBO binding = AiUsageBindingBO.builder()
                .capability("food_fridge_nutrition_match").enabled(true).promptId(42L).build();
        AiPromptBO prompt = AiPromptBO.builder()
                .promptKey("food_fridge_nutrition_match").promptVersion("v1").status("active")
                .systemPrompt("Rank supplied nutrition candidates only.")
                .userPromptTemplate("Food items and bounded nutrition candidates:\n{{candidate_input}}")
                .variables("[\"candidate_input\"]").outputSchema(outputSchema).build();
        ScenarioExecutionDomainService service = new ScenarioExecutionDomainServiceImpl(
                new Routing(binding, model), promptRepository(prompt), unusedModelRoute(), new OutputValidationDomainServiceImpl());
        String candidateInput = "[{\"name\":\"chicken breast\",\"candidates\":[{\"id\":\"food-123\"}]}]";

        var prepared = service.prepareTextExecution("food_fridge_nutrition_match", null, "",
                Map.of("candidate_input", candidateInput), List.of("unrelated_tool"));

        assertThat(prepared.getModel()).isSameAs(model);
        assertThat(prepared.getScenario().getScenarioCode()).isEqualTo("food_fridge_nutrition_match");
        assertThat(prepared.getPrompt()).isEqualTo("Rank supplied nutrition candidates only."
                + System.lineSeparator() + "Food items and bounded nutrition candidates:\n" + candidateInput);
        assertThat(prepared.getPromptKey()).isEqualTo("food_fridge_nutrition_match");
        assertThat(prepared.getPromptVersion()).isEqualTo("v1");
        assertThat(prepared.getOutputSchema()).isEqualTo(outputSchema);
        assertThat(prepared.getToolCodes()).isEmpty();
        assertThat(service.finalizeExecution(prepared, "{\"matches\":[]}").getStatus().name()).isEqualTo("SUCCEEDED");
    }

    @Test
    void fridgeNutritionMatchUsesBoundedFallbackPromptAndSchemaWhenPromptRowIsMissing() {
        AiModelBO model = AiModelBO.builder()
                .modelCode("gpt-6.1-sol").enabled(true)
                .capability(ModelCapability.builder().structuredOutputSupported(true).build())
                .build();
        AiUsageBindingBO binding = AiUsageBindingBO.builder()
                .capability("food_fridge_nutrition_match").enabled(true).promptId(42L).build();
        ScenarioExecutionDomainService service = new ScenarioExecutionDomainServiceImpl(
                new Routing(binding, model), emptyPromptRepository(), unusedModelRoute(), new OutputValidationDomainServiceImpl());
        String candidateInput = "{\"name\":\"oats\",\"candidates\":[]}";

        var prepared = service.prepareTextExecution("food_fridge_nutrition_match", null, "",
                Map.of("candidate_input", candidateInput), List.of());

        assertThat(prepared.getPrompt()).contains("Rank each food name against only the supplied nutrition candidates")
                .contains("return its exact itemIndex")
                .contains("Never invent candidate IDs, names, brands, or nutrient values")
                .contains("Food items and bounded nutrition candidates:")
                .contains(candidateInput);
        assertThat(prepared.getPromptKey()).isEqualTo("food_fridge_nutrition_match");
        assertThat(prepared.getOutputSchema()).contains("\"required\":[\"matches\"]")
                .contains("\"itemIndex\"").contains("\"selectedId\"").contains("\"rankedIds\"");
        assertThat(service.finalizeExecution(prepared, "{\"matches\":[]}").getStatus().name()).isEqualTo("SUCCEEDED");
        assertThat(service.finalizeExecution(prepared, "{\"matches\":[{}]}").getStatus().name())
                .isEqualTo("FAILED");
    }

    @Test
    void usesWechatRouteFallbackPromptWhenBoundPromptIsUnavailable() {
        ScenarioExecutionDomainService service = new ScenarioExecutionDomainServiceImpl(
                new Routing(null, (AiModelBO) null), emptyPromptRepository(), unusedModelRoute(), new OutputValidationDomainServiceImpl());

        String prompt = service.resolveVisionPrompt("wechat_image_route", "stored image metadata");

        assertThat(prompt).contains("微信图片路由分类器");
        assertThat(prompt).contains("ACCOUNTING_RECEIPT").contains("PORTFOLIO_TRADE").contains("UNKNOWN");
        assertThat(prompt).contains("stored image metadata");
        assertThat(prompt).doesNotContain("transactionType");
    }

    private ModelRouteDomainService unusedModelRoute() {
        return (scenario, preferredModelCode) -> null;
    }

    private AiPromptRepository promptRepository(AiPromptBO prompt) {
        return new AiPromptRepository() {
            @Override public List<AiPromptBO> list(String keyword, String scenarioCode, String status) { return List.of(); }
            @Override public AiPromptBO findById(Long id) { return prompt; }
            @Override public Long create(AiPromptBO value) { return null; }
            @Override public boolean update(AiPromptBO value) { return false; }
            @Override public boolean delete(Long id) { return false; }
        };
    }

    private AiPromptRepository emptyPromptRepository() {
        return promptRepository(null);
    }

    private static final class Routing implements ApplicationAnalysisRoutingDomainService {
        private final AiUsageBindingBO binding;
        private final List<AiModelBO> models;

        private Routing(AiUsageBindingBO binding, AiModelBO model) {
            this(binding, model == null ? List.of() : List.of(model));
        }

        private Routing(AiUsageBindingBO binding, List<AiModelBO> models) {
            this.binding = binding;
            this.models = models;
        }

        @Override
        public AiUsageBindingBO findBinding(String capability) {
            return binding != null && capability.equals(binding.getCapability()) ? binding : null;
        }

        @Override
        public List<AiModelBO> routeModels(String capability, String preferredModelCode) {
            return models;
        }

        @Override
        public String promptVersion(AiUsageBindingBO value) {
            return "";
        }
    }
}
