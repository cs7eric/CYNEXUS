package cn.cccs7.cynexus.ai.domain.repository;

import cn.cccs7.cynexus.ai.domain.agent.AgentConversationBO;
import cn.cccs7.cynexus.ai.domain.agent.AgentMessageBO;
import cn.cccs7.cynexus.ai.domain.agent.AgentRunBO;
import cn.cccs7.cynexus.ai.domain.agent.AgentRunStepBO;
import java.util.List;
import java.util.Optional;

public interface AgentRunRepository {

    void saveConversation(AgentConversationBO conversation);

    Optional<AgentConversationBO> findConversation(String ownerKey, String conversationNo);

    List<AgentConversationBO> listConversations(String ownerKey, int limit);

    void saveRun(AgentRunBO run);

    void updateRun(AgentRunBO run);

    void updateConversation(AgentConversationBO conversation);

    void saveStep(AgentRunStepBO step);

    Optional<AgentRunBO> findRun(String ownerKey, String runNo);

    List<AgentRunStepBO> findSteps(String ownerKey, String runNo);

    void saveMessage(AgentMessageBO message);

    List<AgentMessageBO> findMessages(String ownerKey, String conversationNo);
}
