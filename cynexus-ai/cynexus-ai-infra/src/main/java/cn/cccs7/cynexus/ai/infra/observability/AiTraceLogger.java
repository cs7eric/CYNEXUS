package cn.cccs7.cynexus.ai.infra.observability;

import cn.cccs7.cynexus.ai.domain.execution.AiExecutionStatus;
import cn.cccs7.cynexus.ai.domain.port.AiExecutionTracePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AiTraceLogger implements AiExecutionTracePort {

    private static final Logger LOGGER = LoggerFactory.getLogger(AiTraceLogger.class);

    @Override
    public void record(String executionNo, AiExecutionStatus status) {
        LOGGER.info("AI execution completed, executionNo={}, status={}", executionNo, status);
    }
}
