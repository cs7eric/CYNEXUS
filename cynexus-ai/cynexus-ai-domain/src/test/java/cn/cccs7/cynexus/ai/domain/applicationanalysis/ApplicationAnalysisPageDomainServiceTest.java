package cn.cccs7.cynexus.ai.domain.applicationanalysis;

import static org.assertj.core.api.Assertions.assertThat;

import cn.cccs7.cynexus.ai.domain.applicationanalysis.impl.ApplicationAnalysisPageDomainServiceImpl;
import cn.cccs7.cynexus.ai.domain.entity.ApplicationAnalysisPageBO;
import cn.cccs7.cynexus.ai.domain.repository.ApplicationAnalysisPageRepository;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ApplicationAnalysisPageDomainServiceTest {

    @Test
    void saveTargetsNormalizesBusinessTargetBeforeRepositoryAccess() {
        PageRepository repository = new PageRepository();
        ApplicationAnalysisPageDomainService service = service(repository,
                new StubExecutionService(), target -> java.util.Map.of());

        service.saveTargets(new ApplicationAnalysisPageDomainService.SaveTargetsCommand(
                null, null, null, null,
                List.of(new ApplicationAnalysisPageDomainService.TargetInput(
                        "legacy", null, " 600000 ", null, null, null, null, null, List.of("tag")))));

        assertThat(repository.saved.getFirst().getTargetKey()).isEqualTo("stock-600000");
        assertThat(repository.saved.getFirst().getEnabled()).isTrue();
        assertThat(service.getTargets().targets()).hasSize(1);
    }

    @Test
    void runCreatesTargetAndDelegatesMarketInputAndExecutionToDomainCollaborators() {
        PageRepository repository = new PageRepository();
        StubExecutionService execution = new StubExecutionService();
        List<ApplicationAnalysisPageBO.Target> inputs = new ArrayList<>();
        ApplicationAnalysisPageDomainService service = service(repository, execution, target -> {
            inputs.add(target);
            return java.util.Map.of("bars", List.of());
        });

        ApplicationAnalysisPageDomainService.ResultView result = service.run(
                new ApplicationAnalysisPageDomainService.RunCommand("stock", "600519", "Moutai", "qfq"));

        assertThat(inputs).extracting(ApplicationAnalysisPageBO.Target::getTargetKey).containsExactly("stock-600519");
        assertThat(execution.executed).isTrue();
        assertThat(result.targetKey()).isEqualTo("stock-600519");
    }

    private ApplicationAnalysisPageDomainService service(ApplicationAnalysisPageRepository repository,
                                                          ApplicationAnalysisExecutionDomainService execution,
                                                          ApplicationAnalysisKlineInputPort inputPort) {
        return new ApplicationAnalysisPageDomainServiceImpl(repository, execution, inputPort);
    }

    private static final class StubExecutionService implements ApplicationAnalysisExecutionDomainService {
        private boolean executed;
        private ApplicationAnalysisPageBO.Target target;

        @Override
        public ApplicationAnalysisPageBO.Target requireTarget(String targetId) { return target; }

        @Override
        public ExecutionResult execute(ExecutionCommand command) {
            executed = true;
            target = command.target();
            var payload = JsonNodeFactory.instance.objectNode();
            payload.putObject("analysis_result").put("target", "stock-600519");
            return new ExecutionResult(true, command.target().getTargetKey(), LocalDate.of(2026, 7, 23),
                    "postgres://snapshot", false, null, payload);
        }
    }

    private static final class PageRepository implements ApplicationAnalysisPageRepository {
        private final List<ApplicationAnalysisPageBO.Target> saved = new ArrayList<>();

        @Override public ApplicationAnalysisPageBO.Config getDefaultConfig() { return null; }
        @Override public List<ApplicationAnalysisPageBO.Target> listTargets() { return saved; }
        @Override public ApplicationAnalysisPageBO.Target findTarget(String key) { return saved.stream().filter(t -> key.equals(t.getTargetKey())).findFirst().orElse(null); }
        @Override public ApplicationAnalysisPageBO.Config saveTargets(ApplicationAnalysisPageBO.SaveTargetsCommand command) { saved.addAll(command.getTargets()); return null; }
        @Override public ApplicationAnalysisPageBO.CurrentResult findCurrentResult(String key) { return ApplicationAnalysisPageBO.CurrentResult.builder().targetKey(key).build(); }
        @Override public List<ApplicationAnalysisPageBO.HistoryResult> listHistory(String key, int limit) { return List.of(); }
        @Override public List<ApplicationAnalysisPageBO.DailySnapshot> listDailySnapshots(String key, int limit) { return List.of(); }
        @Override public ApplicationAnalysisPageBO.DailySnapshot findDailySnapshot(String key, LocalDate date) { return null; }
        @Override public ApplicationAnalysisPageBO.CurrentResult upsertCurrentResult(ApplicationAnalysisPageBO.CurrentResultUpsertCommand command) { return null; }
        @Override public ApplicationAnalysisPageBO.HistoryResult createHistoryResult(ApplicationAnalysisPageBO.HistoryResultCreateCommand command) { return null; }
        @Override public ApplicationAnalysisPageBO.DailySnapshot upsertDailySnapshot(ApplicationAnalysisPageBO.DailySnapshotUpsertCommand command) { return null; }
    }
}
