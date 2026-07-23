package cn.cccs7.cynexus.ai.domain.applicationanalysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cn.cccs7.cynexus.ai.domain.applicationanalysis.impl.ApplicationAnalysisExecutionDomainServiceImpl;
import cn.cccs7.cynexus.ai.domain.entity.AiCompletionResultBO;
import cn.cccs7.cynexus.ai.domain.entity.AiExecutionLogBO;
import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.entity.AiPromptBO;
import cn.cccs7.cynexus.ai.domain.entity.AiUsageBindingBO;
import cn.cccs7.cynexus.ai.domain.entity.ApplicationAnalysisPageBO;
import cn.cccs7.cynexus.ai.domain.repository.AiExecutionLogRepository;
import cn.cccs7.cynexus.ai.domain.repository.AiPromptRepository;
import cn.cccs7.cynexus.ai.domain.repository.ApplicationAnalysisPageRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ApplicationAnalysisExecutionDomainServiceTest {

    @Test
    void fallsBackToTheNextModelAndPersistsTheSuccessfulState() {
        PageRepository repository = new PageRepository();
        CompletionPort completion = new CompletionPort();
        completion.responses.add(new RuntimeException("provider unavailable"));
        completion.responses.add(AiCompletionResultBO.builder()
                .content("{\"analysis_result\":{\"short_term_trend\":{\"direction\":\"up\"},"
                        + "\"current_situation\":{\"summary\":\"stable\"}}}")
                .providerCode("provider-b").modelCode("model-b").rawResponse("raw")
                .inputTokens(10).outputTokens(20).totalTokens(30).build());

        ApplicationAnalysisExecutionDomainService service = service(repository, completion,
                List.of(AiModelBO.builder().modelCode("model-a").providerCode("provider-a").build(),
                        AiModelBO.builder().modelCode("model-b").providerCode("provider-b").build()));
        ApplicationAnalysisExecutionDomainService.ExecutionResult result = service.execute(
                new ApplicationAnalysisExecutionDomainService.ExecutionCommand(
                        repository.target, LocalDate.of(2026, 7, 23),
                        Map.of("bars", Map.of("daily", Map.of("items", List.of())))));

        assertThat(result.ok()).isTrue();
        assertThat(result.updated()).isTrue();
        assertThat(completion.calls).isEqualTo(2);
        assertThat(repository.currentWrites).isEqualTo(1);
        assertThat(repository.historyWrites).isEqualTo(1);
        assertThat(repository.snapshotWrites).isEqualTo(1);
        assertThat(repository.logs).isNotEmpty();
        assertThat(repository.logs).anyMatch(log -> "FAILED".equals(log.getStatus()));
    }

    @Test
    void rejectsAProviderResponseWithoutAnAnalysisResult() {
        PageRepository repository = new PageRepository();
        CompletionPort completion = new CompletionPort();
        completion.responses.add(AiCompletionResultBO.builder().content("{\"unexpected\":true}").build());

        ApplicationAnalysisExecutionDomainService service = service(repository, completion,
                List.of(AiModelBO.builder().modelCode("model-a").providerCode("provider-a").build()));

        assertThatThrownBy(() -> service.execute(new ApplicationAnalysisExecutionDomainService.ExecutionCommand(
                repository.target, null, Map.of())))
                .hasMessageContaining("does not contain analysis_result");
        assertThat(repository.currentWrites).isZero();
        assertThat(repository.logs).singleElement().extracting(AiExecutionLogBO::getStatus)
                .isEqualTo("FAILED");
    }

    private ApplicationAnalysisExecutionDomainService service(PageRepository repository,
                                                               CompletionPort completion,
                                                               List<AiModelBO> models) {
        return new ApplicationAnalysisExecutionDomainServiceImpl(
                repository,
                new RoutingService(models),
                completion,
                raw -> raw,
                new PromptRepository(),
                repository);
    }

    private static final class RoutingService implements ApplicationAnalysisRoutingDomainService {
        private final List<AiModelBO> models;

        private RoutingService(List<AiModelBO> models) { this.models = models; }

        @Override
        public AiUsageBindingBO findBinding(String capability) { return null; }

        @Override
        public List<AiModelBO> routeModels(String capability, String preferredModelCode) { return models; }

        @Override
        public String promptVersion(AiUsageBindingBO binding) { return ""; }
    }

    private static final class CompletionPort implements ApplicationAnalysisCompletionPort {
        private final List<Object> responses = new ArrayList<>();
        private int calls;

        @Override
        public AiCompletionResultBO complete(AiModelBO model, String prompt, String capability) {
            Object response = responses.get(calls++);
            if (response instanceof RuntimeException exception) throw exception;
            return (AiCompletionResultBO) response;
        }
    }

    private static final class PromptRepository implements AiPromptRepository {
        @Override public List<AiPromptBO> list(String keyword, String scenarioCode, String status) { return List.of(); }
        @Override public AiPromptBO findById(Long id) { return null; }
        @Override public Long create(AiPromptBO prompt) { return 1L; }
        @Override public boolean update(AiPromptBO prompt) { return true; }
        @Override public boolean delete(Long id) { return true; }
    }

    private static final class PageRepository implements ApplicationAnalysisPageRepository, AiExecutionLogRepository {
        private final ApplicationAnalysisPageBO.Target target = ApplicationAnalysisPageBO.Target.builder()
                .id(1L).targetKey("stock-600000").targetType("stock").symbol("600000")
                .name("Test").adjust("qfq").enabled(true).build();
        private final List<AiExecutionLogBO> logs = new ArrayList<>();
        private int currentWrites;
        private int historyWrites;
        private int snapshotWrites;

        @Override public ApplicationAnalysisPageBO.Config getDefaultConfig() { return null; }
        @Override public List<ApplicationAnalysisPageBO.Target> listTargets() { return List.of(target); }
        @Override public ApplicationAnalysisPageBO.Target findTarget(String targetKey) { return target; }
        @Override public ApplicationAnalysisPageBO.Config saveTargets(ApplicationAnalysisPageBO.SaveTargetsCommand command) { return null; }
        @Override public ApplicationAnalysisPageBO.CurrentResult findCurrentResult(String targetKey) { return null; }
        @Override public List<ApplicationAnalysisPageBO.HistoryResult> listHistory(String targetKey, int limit) { return List.of(); }
        @Override public List<ApplicationAnalysisPageBO.DailySnapshot> listDailySnapshots(String targetKey, int limit) { return List.of(); }
        @Override public ApplicationAnalysisPageBO.DailySnapshot findDailySnapshot(String targetKey, LocalDate tradeDate) { return null; }

        @Override
        public ApplicationAnalysisPageBO.CurrentResult upsertCurrentResult(ApplicationAnalysisPageBO.CurrentResultUpsertCommand command) {
            currentWrites++;
            return ApplicationAnalysisPageBO.CurrentResult.builder().targetKey(command.getTargetKey()).build();
        }

        @Override
        public ApplicationAnalysisPageBO.HistoryResult createHistoryResult(ApplicationAnalysisPageBO.HistoryResultCreateCommand command) {
            historyWrites++;
            return ApplicationAnalysisPageBO.HistoryResult.builder().historyKey(command.getHistoryKey()).build();
        }

        @Override
        public ApplicationAnalysisPageBO.DailySnapshot upsertDailySnapshot(ApplicationAnalysisPageBO.DailySnapshotUpsertCommand command) {
            snapshotWrites++;
            return ApplicationAnalysisPageBO.DailySnapshot.builder().targetKey(command.getTargetKey())
                    .tradeDate(command.getTradeDate()).payloadJson(command.getPayloadJson()).build();
        }

        @Override public Long create(AiExecutionLogBO log) { logs.add(log); return (long) logs.size(); }
    }
}
