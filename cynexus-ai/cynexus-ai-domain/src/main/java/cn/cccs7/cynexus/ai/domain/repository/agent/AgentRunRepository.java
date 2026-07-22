package cn.cccs7.cynexus.ai.domain.repository.agent;

import cn.cccs7.cynexus.ai.domain.entity.agent.AgentConversationBO;
import cn.cccs7.cynexus.ai.domain.entity.agent.AgentMessageBO;
import cn.cccs7.cynexus.ai.domain.entity.agent.AgentRunBO;
import cn.cccs7.cynexus.ai.domain.entity.agent.AgentRunEventBO;
import cn.cccs7.cynexus.ai.domain.entity.agent.AgentRunStepBO;
import java.util.List;
import java.util.Optional;
import java.time.OffsetDateTime;

public interface AgentRunRepository {

    void saveConversation(AgentConversationBO conversation);

    Optional<AgentConversationBO> findConversation(String ownerKey, String conversationNo);

    default List<AgentConversationBO> listConversations(String ownerKey, int limit) {
        return listConversations(ownerKey, limit, null);
    }

    List<AgentConversationBO> listConversations(String ownerKey, int limit, String query);

    void saveRun(AgentRunBO run);

    void updateRun(AgentRunBO run);

    void updateConversation(AgentConversationBO conversation);

    void saveStep(AgentRunStepBO step);

    Optional<AgentRunBO> findRun(String ownerKey, String runNo);

    Optional<AgentRunBO> findRunByIdempotencyKey(String ownerKey, String idempotencyKey);

    Optional<AgentRunBO> requestCancellation(String ownerKey, String runNo, String requestedBy);

    boolean isCancellationRequested(String ownerKey, String runNo);

    List<AgentRunStepBO> findSteps(String ownerKey, String runNo);

    void saveMessage(AgentMessageBO message);

    List<AgentMessageBO> findMessages(String ownerKey, String conversationNo);

    List<AgentRunBO> findStaleRunningRuns(OffsetDateTime cutoff);

    default void saveEvent(String ownerKey, AgentRunEventBO event) {
        // Legacy test repositories may not persist stream events.
    }

    default List<AgentRunEventBO> findEvents(String ownerKey, String runNo, int afterSequenceNo, int limit) {
        return List.of();
    }

    default int archiveBefore(OffsetDateTime cutoff) {
        return 0;
    }
}
