package cn.cccs7.cynexus.ai.domain.service.agent.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import cn.cccs7.cynexus.ai.domain.entity.agent.AgentRunBO;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentExecutionLimits;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentModelToolCall;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AgentExecutionDomainServiceTest {

    private final AgentExecutionDomainService service = new AgentExecutionDomainService(
            AgentExecutionLimits.defaults(), null);

    @Test
    void validatesAndNormalizesBoundedInput() {
        assertEquals("今天市场怎么样？", service.validateUserInput("  今天市场怎么样？  "));
        assertEquals("idem-1", service.normalizeIdempotencyKey(" idem-1 "));
        assertThrows(IllegalArgumentException.class, () -> service.validateUserInput(" "));
        assertThrows(IllegalArgumentException.class,
                () -> service.validateUserInput("x".repeat(12_001)));
    }

    @Test
    void rejectsDuplicateCallsAndIncrementsToolCountWithinTheLimit() {
        AgentModelToolCall first = AgentModelToolCall.builder().callId("call-1")
                .toolCode("market.overview").arguments(Map.of()).build();
        assertThrows(IllegalStateException.class,
                () -> service.validateToolCalls(List.of(first, first), 0));

        AgentRunBO run = AgentRunBO.builder().toolCallCount(11).build();
        assertEquals(12, service.incrementToolCallCount(run));
        assertThrows(IllegalStateException.class, () -> service.incrementToolCallCount(run));
    }

    @Test
    void classifiesCancellationAndFailureAtTheDomainBoundary() {
        assertEquals(true, service.isCancellationRequested(false, true));
        assertEquals("TIMEOUT", service.classifyFailure(new IllegalStateException("provider timeout")));
        assertThrows(IllegalStateException.class,
                () -> service.checkTurn(1, OffsetDateTime.now().minusSeconds(601), OffsetDateTime.now()));
    }
}
