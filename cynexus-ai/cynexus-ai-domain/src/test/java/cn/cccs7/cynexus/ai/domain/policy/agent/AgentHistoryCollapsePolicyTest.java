package cn.cccs7.cynexus.ai.domain.policy.agent;

import static org.assertj.core.api.Assertions.assertThat;

import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentModelMessage;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentModelToolCall;
import java.util.List;
import org.junit.jupiter.api.Test;

class AgentHistoryCollapsePolicyTest {

    @Test
    void preservesNativeAssistantOutputAndItsLinkedToolResult() {
        AgentModelMessage user = AgentModelMessage.builder().role("user").content("question").build();
        AgentModelMessage assistant = AgentModelMessage.builder()
                .role("assistant")
                .content("")
                .runtimeContext("runtime-1")
                .providerOutputJson("[{\"type\":\"reasoning\",\"encrypted_content\":\"opaque\"},"
                        + "{\"type\":\"function_call\",\"call_id\":\"call-1\"}]")
                .toolCalls(List.of(AgentModelToolCall.builder().callId("call-1").build()))
                .build();
        AgentModelMessage tool = AgentModelMessage.builder().role("tool")
                .toolCallId("call-1").content("{\"result\":1}").build();

        List<AgentModelMessage> collapsed = AgentHistoryCollapsePolicy.collapseForProvider(
                List.of(user, assistant, tool));

        assertThat(collapsed).containsExactly(user, assistant, tool);
        assertThat(collapsed.get(1).getRuntimeContext()).isEqualTo("runtime-1");
        assertThat(collapsed.get(1).getProviderOutputJson()).contains("encrypted_content");
    }

    @Test
    void dropsLegacyToolResultsThatCannotBeReplayedSafely() {
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
}
