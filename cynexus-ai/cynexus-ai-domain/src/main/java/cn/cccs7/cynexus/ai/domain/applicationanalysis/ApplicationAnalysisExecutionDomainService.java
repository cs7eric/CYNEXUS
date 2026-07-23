package cn.cccs7.cynexus.ai.domain.applicationanalysis;

import cn.cccs7.cynexus.ai.domain.entity.ApplicationAnalysisPageBO;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.LocalDate;
import java.util.Map;

public interface ApplicationAnalysisExecutionDomainService {

    ApplicationAnalysisPageBO.Target requireTarget(String targetId);

    ExecutionResult execute(ExecutionCommand command);

    record ExecutionCommand(ApplicationAnalysisPageBO.Target target,
                             LocalDate requestedDate,
                             Map<String, Object> analysisInput) {
    }

    record ExecutionResult(boolean ok,
                           String targetKey,
                           LocalDate date,
                           String snapshotPath,
                           boolean updated,
                           String skipReason,
                           ObjectNode payload) {
    }
}
