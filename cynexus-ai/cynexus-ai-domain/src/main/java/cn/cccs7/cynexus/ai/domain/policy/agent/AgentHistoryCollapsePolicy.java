package cn.cccs7.cynexus.ai.domain.policy.agent;

import cn.cccs7.cynexus.ai.common.constant.AiConstants;
import cn.cccs7.cynexus.ai.domain.entity.agent.AgentMessageBO;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentModelMessage;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
        for (AgentModelMessage message : history) {
            if (message == null || message.getRole() == null) {
                continue;
            }
            String role = message.getRole();
            if (AiConstants.AI_ROLE_USER.equals(role)) {
                collapsed.add(AgentModelMessage.builder().role(AiConstants.AI_ROLE_USER)
                        .content(message.getContent() == null ? "" : message.getContent()).build());
                continue;
            }
            if (AiConstants.AI_ROLE_ASSISTANT.equals(role)) {
                String content = message.getContent() == null ? "" : message.getContent().trim();
                if (!content.isBlank()) {
                    collapsed.add(AgentModelMessage.builder().role(AiConstants.AI_ROLE_ASSISTANT)
                            .content(content).build());
                }
            }
        }
        return collapsed;
    }

    public static List<AgentModelMessage> collapsePersistedForProvider(
            List<AgentMessageBO> history, Set<String> replayableRunNos) {
        if (history == null || history.isEmpty() || replayableRunNos == null || replayableRunNos.isEmpty()) {
            return List.of();
        }
        Map<String, List<AgentMessageBO>> messagesByRun = new LinkedHashMap<>();
        for (AgentMessageBO message : history) {
            if (message == null || message.getRunNo() == null || message.getRunNo().isBlank()
                    || !replayableRunNos.contains(message.getRunNo())) {
                continue;
            }
            messagesByRun.computeIfAbsent(message.getRunNo(), ignored -> new ArrayList<>()).add(message);
        }

        List<AgentModelMessage> collapsed = new ArrayList<>();
        for (List<AgentMessageBO> runMessages : messagesByRun.values()) {
            AgentMessageBO finalAssistant = finalVisibleAssistant(runMessages);
            if (finalAssistant == null) continue;
            for (AgentMessageBO message : runMessages) {
                if (AiConstants.AI_ROLE_USER.equals(message.getRole())
                        && message.getContent() != null && !message.getContent().isBlank()) {
                    collapsed.add(AgentModelMessage.builder().role(AiConstants.AI_ROLE_USER)
                            .content(message.getContent()).build());
                }
            }
            collapsed.add(AgentModelMessage.builder().role(AiConstants.AI_ROLE_ASSISTANT)
                    .content(finalAssistant.getContent().trim()).build());
        }
        return List.copyOf(collapsed);
    }

    private static AgentMessageBO finalVisibleAssistant(List<AgentMessageBO> runMessages) {
        AgentMessageBO finalAssistant = null;
        for (AgentMessageBO message : runMessages) {
            if (message != null && AiConstants.AI_ROLE_ASSISTANT.equals(message.getRole())
                    && message.getContent() != null && !message.getContent().isBlank()) {
                finalAssistant = message;
            }
        }
        return finalAssistant;
    }
}
