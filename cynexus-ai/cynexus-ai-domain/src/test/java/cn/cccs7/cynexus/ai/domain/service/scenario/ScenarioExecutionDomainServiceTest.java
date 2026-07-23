package cn.cccs7.cynexus.ai.domain.service.scenario;

import static org.assertj.core.api.Assertions.assertThat;

import cn.cccs7.cynexus.ai.domain.applicationanalysis.ApplicationAnalysisRoutingDomainService;
import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.entity.AiPromptBO;
import cn.cccs7.cynexus.ai.domain.entity.AiUsageBindingBO;
import cn.cccs7.cynexus.ai.domain.repository.AiPromptRepository;
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
                new Routing(binding, model), emptyPromptRepository());

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
                new Routing(binding, null), promptRepository(prompt));

        assertThat(service.resolveVisionPrompt("accounting_receipt_parse", "receipt candidates"))
                .isEqualTo("system" + System.lineSeparator() + "candidate=receipt candidates");
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
        private final AiModelBO model;

        private Routing(AiUsageBindingBO binding, AiModelBO model) {
            this.binding = binding;
            this.model = model;
        }

        @Override
        public AiUsageBindingBO findBinding(String capability) {
            return binding != null && capability.equals(binding.getCapability()) ? binding : null;
        }

        @Override
        public List<AiModelBO> routeModels(String capability, String preferredModelCode) {
            return model == null ? List.of() : List.of(model);
        }

        @Override
        public String promptVersion(AiUsageBindingBO value) {
            return "";
        }
    }
}
