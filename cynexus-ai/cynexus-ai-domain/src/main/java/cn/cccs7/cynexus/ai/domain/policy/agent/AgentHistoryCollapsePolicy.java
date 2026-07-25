package cn.cccs7.cynexus.ai.domain.policy.agent;

import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentModelMessage;
import java.util.ArrayList;
import java.util.List;

/**
 * Collapses persisted multi-run history into provider-safe user/assistant text only.
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
                if (!content.isBlank()) {
                    collapsed.add(AgentModelMessage.builder().role("assistant").content(message.getContent()).build());
                }
            }
        }
        return collapsed;
    }
}