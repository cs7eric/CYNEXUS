package cn.cccs7.cynexus.ai.domain.service.agent.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cn.cccs7.cynexus.ai.domain.entity.agent.AgentRunBO;
import cn.cccs7.cynexus.ai.domain.policy.agent.AgentToolBudgetPolicy;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentExecutionLimits;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentModelToolCall;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolCallAdmission;
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
    void rejectsDuplicateCallsAndIncrementsToolCountWithoutHardFail() {
        AgentModelToolCall first = AgentModelToolCall.builder().callId("call-1")
                .toolCode("market.overview").arguments(Map.of()).build();
        assertThrows(IllegalStateException.class,
                () -> service.validateToolCalls(List.of(first, first), 0));

        AgentRunBO run = AgentRunBO.builder().toolCallCount(11).build();
        assertEquals(12, service.incrementToolCallCount(run));
        // Soft path: increment only tracks usage and never hard-fails.
        assertEquals(13, service.incrementToolCallCount(run));
        assertEquals(13, run.getToolCallCount());
    }

    @Test
    void softAdmitsToolCallsUntilBudgetThenRejectsRemainder() {
        List<AgentModelToolCall> calls = List.of(
                AgentModelToolCall.builder().callId("c1").toolCode("market.overview").arguments(Map.of()).build(),
                AgentModelToolCall.builder().callId("c2").toolCode("market.quote").arguments(Map.of()).build(),
                AgentModelToolCall.builder().callId("c3").toolCode("market.news").arguments(Map.of()).build());
        AgentToolCallAdmission admission = service.admitToolCalls(calls, 1, 3);
        assertEquals(2, admission.getAccepted().size());
        assertEquals(1, admission.getRejected().size());
        assertEquals("c3", admission.getRejected().get(0).getCallId());
        assertTrue(admission.isBudgetExhausted());
        assertEquals(0, admission.getRemainingBudget());
    }

    @Test
    void absoluteCeilingStillHardFailsViaLegacyValidateToolCalls() {
        AgentModelToolCall call = AgentModelToolCall.builder().callId("c1")
                .toolCode("market.overview").arguments(Map.of()).build();
        // defaults maxToolCalls=50; existingCount already at ceiling rejects the new call
        assertThrows(IllegalStateException.class, () -> service.validateToolCalls(List.of(call), 50));
    }

    @Test
    void classifiesSimpleAndDeepToolBudgets() {
        AgentExecutionLimits limits = AgentExecutionLimits.defaults();
        assertEquals(AgentToolBudgetPolicy.Depth.SIMPLE, AgentToolBudgetPolicy.classify("现在多少点"));
        assertEquals(limits.getSimpleMaxToolCalls(), AgentToolBudgetPolicy.budgetFor("现在多少点", limits));
        assertEquals(AgentToolBudgetPolicy.Depth.DEEP, AgentToolBudgetPolicy.classify("请做深度复盘并分析板块轮动风险"));
        assertEquals(limits.getDeepMaxToolCalls(),
                AgentToolBudgetPolicy.budgetFor("请做深度复盘并分析板块轮动风险", limits));
        assertEquals(12, limits.getSimpleMaxToolCalls());
        assertEquals(30, limits.getDeepMaxToolCalls());
        assertEquals(50, limits.getMaxToolCalls());
        assertFalse(AgentToolBudgetPolicy.budgetFor("hi", limits) > limits.getMaxToolCalls());
    }

    @Test
    void classifiesCancellationAndFailureAtTheDomainBoundary() {
        assertEquals(true, service.isCancellationRequested(false, true));
        assertEquals("TIMEOUT", service.classifyFailure(new IllegalStateException("provider timeout")));
        assertThrows(IllegalStateException.class,
                () -> service.checkTurn(1, OffsetDateTime.now().minusSeconds(601), OffsetDateTime.now()));
    }
}
