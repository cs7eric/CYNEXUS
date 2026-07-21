package cn.cccs7.cynexus.ai.domain.repository;

import cn.cccs7.cynexus.ai.domain.agent.AgentConversationBO;
import cn.cccs7.cynexus.ai.domain.agent.AgentMessageBO;
import cn.cccs7.cynexus.ai.domain.agent.AgentRunBO;
import cn.cccs7.cynexus.ai.domain.agent.AgentRunStepBO;
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

    List<AgentRunStepBO> findSteps(String ownerKey, String runNo);

    void saveMessage(AgentMessageBO message);

    List<AgentMessageBO> findMessages(String ownerKey, String conversationNo);

    List<AgentRunBO> findStaleRunningRuns(OffsetDateTime cutoff);
}
