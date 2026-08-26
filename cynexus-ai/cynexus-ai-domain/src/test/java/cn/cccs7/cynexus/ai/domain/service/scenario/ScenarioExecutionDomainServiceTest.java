package cn.cccs7.cynexus.ai.domain.service.scenario;

import static org.assertj.core.api.Assertions.assertThat;

import cn.cccs7.cynexus.ai.domain.applicationanalysis.ApplicationAnalysisRoutingDomainService;
import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.entity.AiPromptBO;
import cn.cccs7.cynexus.ai.domain.entity.AiUsageBindingBO;
import cn.cccs7.cynexus.ai.domain.repository.AiPromptRepository;
import cn.cccs7.cynexus.ai.domain.service.routing.ModelRouteDomainService;
import cn.cccs7.cynexus.ai.domain.validation.OutputValidationDomainService;
import cn.cccs7.cynexus.ai.domain.validation.OutputValidationDomainServiceImpl;
import cn.cccs7.cynexus.ai.domain.valueobject.ModelCapability;
import java.util.List;
import org.junit.jupiter.api.Test;

class ScenarioExecutionDomainServiceTest {

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
