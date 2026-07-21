package cn.cccs7.cynexus.ai.infra.repository;

import cn.cccs7.cynexus.ai.domain.agent.AgentConversationBO;
import cn.cccs7.cynexus.ai.domain.agent.AgentRunBO;
import cn.cccs7.cynexus.ai.domain.agent.AgentRunStepBO;
import cn.cccs7.cynexus.ai.domain.repository.AgentRunRepository;
import cn.cccs7.cynexus.ai.infra.basic.service.AgentConversationBasicService;
import cn.cccs7.cynexus.ai.infra.basic.service.AgentRunBasicService;
import cn.cccs7.cynexus.ai.infra.convert.AgentConversationPOConverter;
import cn.cccs7.cynexus.ai.infra.convert.AgentRunPOConverter;
import cn.cccs7.cynexus.ai.infra.convert.AgentRunStepPOConverter;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class AgentRunRepositoryImpl implements AgentRunRepository {
    private final AgentConversationBasicService conversationService;
    private final AgentRunBasicService runService;

    public AgentRunRepositoryImpl(AgentConversationBasicService conversationService, AgentRunBasicService runService) {
        this.conversationService = conversationService;
        this.runService = runService;
    }

    @Override
    public void saveConversation(AgentConversationBO conversation) {
        conversationService.create(AgentConversationPOConverter.INSTANCE.toSource(conversation));
    }

    @Override
    public Optional<AgentConversationBO> findConversation(String ownerKey, String conversationNo) {
        return conversationService.find(ownerKey, conversationNo).map(AgentConversationPOConverter.INSTANCE::toTarget);
    }

    @Override
    public void saveRun(AgentRunBO run) {
        runService.create(AgentRunPOConverter.INSTANCE.toSource(run));
    }

    @Override
    public void updateRun(AgentRunBO run) {
        runService.update(AgentRunPOConverter.INSTANCE.toSource(run));
    }

    @Override
    public void updateConversation(AgentConversationBO conversation) {
        conversationService.update(AgentConversationPOConverter.INSTANCE.toSource(conversation));
    }

    @Override
    public void saveStep(AgentRunStepBO step) {
        runService.createStep(cn.cccs7.cynexus.ai.infra.convert.AgentRunStepPOConverter.INSTANCE.toSource(step));
    }

    @Override
    public Optional<AgentRunBO> findRun(String ownerKey, String runNo) {
        return Optional.ofNullable(runService.find(ownerKey, runNo)).map(AgentRunPOConverter.INSTANCE::toTarget);
    }

    @Override
    public List<AgentRunStepBO> findSteps(String ownerKey, String runNo) {
        return runService.findSteps(ownerKey, runNo).stream()
                .map(AgentRunStepPOConverter.INSTANCE::toTarget).toList();
    }
}
