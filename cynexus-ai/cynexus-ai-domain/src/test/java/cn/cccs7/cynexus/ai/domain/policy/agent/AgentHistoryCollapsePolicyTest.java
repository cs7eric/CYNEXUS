package cn.cccs7.cynexus.ai.domain.policy.agent;

import static org.assertj.core.api.Assertions.assertThat;

import cn.cccs7.cynexus.ai.domain.entity.agent.AgentMessageBO;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentModelMessage;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentModelToolCall;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AgentHistoryCollapsePolicyTest {

    @Test
    void collapsesCompletedPriorRunToVisibleConversationOnly() {
        List<AgentMessageBO> persisted = List.of(
                message("AGR-1", 1, "user", "question"),
                AgentMessageBO.builder().runNo("AGR-1").sequenceNo(2).role("assistant").content("")
                        .runtimeContext("runtime-1")
                        .providerOutputJson("[{\"type\":\"reasoning\"},{\"type\":\"function_call\"}]")
                        .toolCallsJson("[{\"callId\":\"call-1\"}]").build(),
                AgentMessageBO.builder().runNo("AGR-1").sequenceNo(3).role("tool")
                        .toolCallId("call-1").content("{\"result\":1}").build(),
                AgentMessageBO.builder().runNo("AGR-1").sequenceNo(4).role("assistant")
                        .content(" final answer ").runtimeContext("runtime-2")
                        .providerOutputJson("[{\"type\":\"message\"}]").build());

        List<AgentModelMessage> collapsed = AgentHistoryCollapsePolicy.collapsePersistedForProvider(
                persisted, Set.of("AGR-1"));

        assertThat(collapsed).extracting(AgentModelMessage::getRole)
                .containsExactly("user", "assistant");
        assertThat(collapsed).extracting(AgentModelMessage::getContent)
                .containsExactly("question", "final answer");
        assertThat(collapsed).allSatisfy(message -> {
            assertThat(message.getRuntimeContext()).isNull();
            assertThat(message.getProviderOutputJson()).isNull();
            assertThat(message.getToolCalls()).isNull();
            assertThat(message.getToolCallId()).isNull();
        });
    }

    @Test
    void dropsFailedRunTailAndCrossRunOrphanedToolHistory() {
        List<AgentMessageBO> persisted = List.of(
                message("AGR-ok", 1, "user", "completed question"),
                AgentMessageBO.builder().runNo("AGR-ok").sequenceNo(2).role("assistant").content("")
                        .toolCallsJson("[{\"callId\":\"call-ok\"}]").build(),
                AgentMessageBO.builder().runNo("AGR-ok").sequenceNo(3).role("tool")
                        .toolCallId("call-ok").content("result").build(),
                message("AGR-ok", 4, "assistant", "completed answer"),
                message("AGR-failed", 5, "user", "unmatched failed question"),
                AgentMessageBO.builder().runNo("AGR-failed").sequenceNo(6).role("assistant").content("")
                        .toolCallsJson("[{\"callId\":\"call-failed\"}]").build(),
                AgentMessageBO.builder().runNo("AGR-failed").sequenceNo(7).role("tool")
                        .toolCallId("call-failed").content("partial result").build());

        List<AgentModelMessage> collapsed = AgentHistoryCollapsePolicy.collapsePersistedForProvider(
                persisted, Set.of("AGR-ok"));

        assertThat(collapsed).extracting(AgentModelMessage::getContent)
                .containsExactly("completed question", "completed answer");
        assertThat(collapsed).noneMatch(message -> "tool".equals(message.getRole()));
    }

    @Test
    void dropsReplayableRunWhenFinalVisibleAssistantIsMissing() {
        List<AgentMessageBO> persisted = List.of(
                message("AGR-incomplete", 1, "user", "question"),
                AgentMessageBO.builder().runNo("AGR-incomplete").sequenceNo(2).role("assistant").content("")
                        .providerOutputJson("[{\"type\":\"function_call\"}]").build());

        assertThat(AgentHistoryCollapsePolicy.collapsePersistedForProvider(
                persisted, Set.of("AGR-incomplete"))).isEmpty();
    }

    @Test
    void legacyProjectionDropsNativeToolWireState() {
        AgentModelMessage legacyAssistant = AgentModelMessage.builder().role("assistant")
                .content("")
                .toolCalls(List.of(AgentModelToolCall.builder().callId("call-old").build()))
                .build();
        AgentModelMessage orphanTool = AgentModelMessage.builder().role("tool")
                .toolCallId("call-old").content("result").build();
        AgentModelMessage visibleAssistant = AgentModelMessage.builder().role("assistant")
                .content("answer").build();

        assertThat(AgentHistoryCollapsePolicy.collapseForProvider(
                List.of(legacyAssistant, orphanTool, visibleAssistant)))
                .containsExactly(visibleAssistant);
    }

    private AgentMessageBO message(String runNo, int sequenceNo, String role, String content) {
        return AgentMessageBO.builder().runNo(runNo).sequenceNo(sequenceNo).role(role).content(content).build();
    }
}
