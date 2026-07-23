package cn.cccs7.cynexus.ai.application.service;

import cn.cccs7.cynexus.ai.domain.applicationanalysis.ApplicationAnalysisExecutionDomainService;
import cn.cccs7.cynexus.ai.domain.entity.ApplicationAnalysisPageBO;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Application boundary for the recent-30 use case. */
@Service
public class ApplicationAnalysisRecent30Executor {

    private final ApplicationAnalysisExecutionDomainService executionDomainService;
    private final ApplicationAnalysisKlineInputBuilder inputBuilder;

    public ApplicationAnalysisRecent30Executor(
            ApplicationAnalysisExecutionDomainService executionDomainService,
            ApplicationAnalysisKlineInputBuilder inputBuilder) {
        this.executionDomainService = executionDomainService;
        this.inputBuilder = inputBuilder;
    }

    @Transactional
    public ExecutionResult execute(String targetId, LocalDate date) {
        ApplicationAnalysisPageBO.Target target = executionDomainService.requireTarget(targetId);
        ApplicationAnalysisExecutionDomainService.ExecutionResult result = executionDomainService.execute(
                new ApplicationAnalysisExecutionDomainService.ExecutionCommand(
                        target, date, inputBuilder.buildRecent30Input(target)));
        return new ExecutionResult(result.ok(), result.targetKey(), result.date(), result.snapshotPath(),
                result.updated(), result.skipReason(), result.payload());
    }

    public record ExecutionResult(boolean ok,
                                   String targetKey,
                                   LocalDate date,
                                   String snapshotPath,
                                   boolean updated,
                                   String skipReason,
                                   com.fasterxml.jackson.databind.node.ObjectNode payload) {
    }
}
