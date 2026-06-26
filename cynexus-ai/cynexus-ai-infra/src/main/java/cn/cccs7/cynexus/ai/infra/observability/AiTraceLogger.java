package cn.cccs7.cynexus.ai.infra.observability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AiTraceLogger {

    private static final Logger LOGGER = LoggerFactory.getLogger(AiTraceLogger.class);

    public void record(String executionNo, String status) {
        LOGGER.info("AI execution completed, executionNo={}, status={}", executionNo, status);
    }
}
