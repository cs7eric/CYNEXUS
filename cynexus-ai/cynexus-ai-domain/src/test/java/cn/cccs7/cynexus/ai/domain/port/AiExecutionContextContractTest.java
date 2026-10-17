package cn.cccs7.cynexus.ai.domain.port;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class AiExecutionContextContractTest {
    @Test void preservesAllCorrelationFields() {
        var context = new AiExecutionContext("exec", "cap", "prompt", "v1", "provider", "model",
                "tenant", "principal", "request", "trace");
        assertEquals("exec", context.executionNo());
        assertEquals("cap", context.capability());
        assertEquals("prompt", context.promptKey());
        assertEquals("v1", context.promptVersion());
        assertEquals("tenant", context.tenantKey());
        assertEquals("principal", context.principalKey());
        assertEquals("request", context.requestId());
        assertEquals("trace", context.traceId());
    }

    @Test void repairAttemptKeepsIdentityAndSeparatesCorrelation() {
        var context = new AiExecutionContext("exec", "cap", "prompt", "v1", "provider", "model",
                "tenant", "principal", "request", "trace");

        var repair = context.repairAttempt();

        assertEquals("exec-repair", repair.executionNo());
        assertEquals("request-repair", repair.requestId());
        assertEquals("cap", repair.capability());
        assertEquals("prompt", repair.promptKey());
        assertEquals("v1", repair.promptVersion());
        assertEquals("tenant", repair.tenantKey());
        assertEquals("principal", repair.principalKey());
        assertEquals("trace", repair.traceId());
    }
}
