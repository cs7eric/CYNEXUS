package cn.cccs7.cynexus.ai.domain.port;

import cn.cccs7.cynexus.ai.domain.execution.AiExecutionStatus;

public interface AiExecutionTracePort {

    void record(String executionNo, AiExecutionStatus status);
}
