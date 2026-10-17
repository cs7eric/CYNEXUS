package cn.cccs7.cynexus.ai.domain.marketfactor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cn.cccs7.cynexus.ai.domain.applicationanalysis.ApplicationAnalysisRoutingDomainService;
import cn.cccs7.cynexus.ai.domain.entity.AiCompletionResultBO;
import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.marketfactor.impl.MarketFactorStudySynthesisDomainServiceImpl;
import cn.cccs7.cynexus.ai.domain.port.AiPromptRuntimeResolver;
import cn.cccs7.cynexus.ai.domain.repository.AiExecutionLogRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MarketFactorStudySynthesisDomainServiceImplTest {

    @Test
    void keepsOnlyKnownEvidenceReferencesFromStructuredModelOutput() {
        var completion = new Completion("""
                {"study_synthesis":{"summary":"已完成研究摘要","findings":[
                  {"label":"候选","evidence_refs":["factor:1","invented:1"],
                   "counter_evidence_refs":["factor:2","invented:2"],"interpretation":"仅基于持久化统计"}],
                  "caveats":["非因果结论"]}}
                """);
        var service = service("PASSED", completion);

        var result = service.synthesize(command());

        assertThat(result.synthesis().path("findings")).hasSize(1);
        assertThat(result.synthesis().path("findings").get(0).path("evidence_refs"))
                .extracting(value -> value.asText()).containsExactly("factor:1");
        assertThat(result.synthesis().path("findings").get(0).path("counter_evidence_refs"))
                .extracting(value -> value.asText()).containsExactly("factor:2");
        assertThat(completion.calls).isEqualTo(1);
    }

    @Test
    void rejectsAStudyWhoseLeakageCheckDidNotPassBeforeCallingTheModel() {
        var completion = new Completion("{}");
        var service = service("FAILED", completion);

        assertThatThrownBy(() -> service.synthesize(command()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("leakage check");
        assertThat(completion.calls).isZero();
    }

    @Test
    void rejectsMalformedModelOutputRatherThanReturningUngovernedText() {
        var completion = new Completion("not-json");
        var service = service("PASSED", completion);

        assertThatThrownBy(() -> service.synthesize(command()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AI study synthesis failed");
    }

    private MarketFactorStudySynthesisDomainService service(String leakageStatus, Completion completion) {
        ApplicationAnalysisRoutingDomainService routing = new ApplicationAnalysisRoutingDomainService() {
            @Override public cn.cccs7.cynexus.ai.domain.entity.AiUsageBindingBO findBinding(String capability) { return null; }
            @Override public List<AiModelBO> routeModels(String capability, String preferredModelCode) {
                return List.of(AiModelBO.builder().providerCode("test-provider").modelCode("test-model").build());
            }
            @Override public String promptVersion(cn.cccs7.cynexus.ai.domain.entity.AiUsageBindingBO binding) { return "v1"; }
        };
        AiExecutionLogRepository logs = log -> 1L;
        MarketFactorStudyEvidencePort evidence = (studyId, principal, tenant, signature) -> new MarketFactorStudyEvidencePort.EvidenceSnapshot(
                studyId, "SUCCEEDED", "2026-08-14T15:00:00+08:00", leakageStatus, List.of(),
                Map.of("factor:1", "candidate statistics", "factor:2", "counter evidence"),
                Map.of("studyId", studyId, "statistics", Map.of("lift", 1.6)));
        AiPromptRuntimeResolver prompts = (capability, promptKey, system, template) ->
                new AiPromptRuntimeResolver.AiPromptRuntime(promptKey, "v1", system, template);
        return new MarketFactorStudySynthesisDomainServiceImpl(routing, logs, evidence, completion, prompts);
    }

    private MarketFactorStudySynthesisDomainService.Command command() {
        return new MarketFactorStudySynthesisDomainService.Command("123", "owner-a", "tenant-a", "signed");
    }

    private static final class Completion implements MarketEventFactorAnalysisCompletionPort {
        private final String response;
        private int calls;

        private Completion(String response) { this.response = response; }

        @Override public AiCompletionResultBO complete(AiModelBO model, String prompt, String capability) {
            calls++;
            return AiCompletionResultBO.builder().content(response).providerCode("test-provider").modelCode("test-model").build();
        }
    }
}
