package cn.cccs7.cynexus.ai.domain.policy.agent;

import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentModelMessage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Collapses persisted multi-run history into provider-safe replayable turns.
 * Cross-run tool wire history must not be replayed without original response linkage.
 */
public final class AgentHistoryCollapsePolicy {

    private AgentHistoryCollapsePolicy() {
    }

    public static List<AgentModelMessage> collapseForProvider(List<AgentModelMessage> history) {
        if (history == null || history.isEmpty()) {
            return List.of();
        }
        List<AgentModelMessage> collapsed = new ArrayList<>();
        Set<String> replayableToolCalls = new HashSet<>();
        for (AgentModelMessage message : history) {
            if (message == null || message.getRole() == null) {
                continue;
            }
            String role = message.getRole();
            if ("user".equals(role)) {
                collapsed.add(AgentModelMessage.builder().role("user")
                        .content(message.getContent() == null ? "" : message.getContent()).build());
                continue;
            }
            if ("assistant".equals(role)) {
                String content = message.getContent() == null ? "" : message.getContent().trim();
                boolean nativeOutput = message.getProviderOutputJson() != null
                        && !message.getProviderOutputJson().isBlank();
                if (nativeOutput || !content.isBlank()) {
                    collapsed.add(message);
                    if (nativeOutput && message.getToolCalls() != null) {
                        message.getToolCalls().stream()
                                .filter(call -> call != null && call.getCallId() != null && !call.getCallId().isBlank())
                                .forEach(call -> replayableToolCalls.add(call.getCallId()));
                    }
                }
                continue;
            }
            if ("tool".equals(role) && message.getToolCallId() != null
                    && replayableToolCalls.remove(message.getToolCallId())) {
                collapsed.add(message);
            }
        }
        return collapsed;
    }
}
