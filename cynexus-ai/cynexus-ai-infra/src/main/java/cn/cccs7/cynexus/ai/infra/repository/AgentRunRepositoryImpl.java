package cn.cccs7.cynexus.ai.infra.repository;

import cn.cccs7.cynexus.ai.domain.agent.AgentConversationBO;
import cn.cccs7.cynexus.ai.domain.agent.AgentMessageBO;
import cn.cccs7.cynexus.ai.domain.agent.AgentRunBO;
import cn.cccs7.cynexus.ai.domain.agent.AgentRunEventBO;
import cn.cccs7.cynexus.ai.domain.agent.AgentRunStepBO;
import cn.cccs7.cynexus.ai.domain.repository.AgentRunRepository;
import cn.cccs7.cynexus.ai.infra.basic.service.AgentConversationBasicService;
import cn.cccs7.cynexus.ai.infra.basic.service.AgentMessageBasicService;
import cn.cccs7.cynexus.ai.infra.basic.service.AgentRunBasicService;
import cn.cccs7.cynexus.ai.infra.basic.service.AgentRunEventBasicService;
import cn.cccs7.cynexus.ai.infra.basic.service.AgentRunRetentionBasicService;
import cn.cccs7.cynexus.ai.infra.convert.AgentConversationPOConverter;
import cn.cccs7.cynexus.ai.infra.convert.AgentMessagePOConverter;
import cn.cccs7.cynexus.ai.infra.convert.AgentRunPOConverter;
import cn.cccs7.cynexus.ai.infra.convert.AgentRunEventPOConverter;
import cn.cccs7.cynexus.ai.infra.convert.AgentRunStepPOConverter;
import java.util.List;
import java.util.Optional;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Repository;

@Repository
public class AgentRunRepositoryImpl implements AgentRunRepository {
    private final AgentConversationBasicService conversationService;
    private final AgentRunBasicService runService;
    private final AgentMessageBasicService messageService;
    private final AgentRunEventBasicService eventService;
    private final AgentRunRetentionBasicService retentionService;

    public AgentRunRepositoryImpl(AgentConversationBasicService conversationService, AgentRunBasicService runService,
                                  AgentMessageBasicService messageService, AgentRunEventBasicService eventService,
                                  AgentRunRetentionBasicService retentionService) {
        this.conversationService = conversationService;
        this.runService = runService;
        this.messageService = messageService;
        this.eventService = eventService;
        this.retentionService = retentionService;
    }

    @Override
    public void saveConversation(AgentConversationBO conversation) {
        var source = AgentConversationPOConverter.INSTANCE.toSource(conversation);
        conversationService.create(source);
        conversation.setId(source.getId());
        conversation.setCreatedAt(source.getCreatedAt());
        conversation.setUpdatedAt(source.getUpdatedAt());
    }

    @Override
    public Optional<AgentConversationBO> findConversation(String ownerKey, String conversationNo) {
        return conversationService.find(ownerKey, conversationNo).map(AgentConversationPOConverter.INSTANCE::toTarget);
    }

    @Override
    public List<AgentConversationBO> listConversations(String ownerKey, int limit) {
        return listConversations(ownerKey, limit, null);
    }

    @Override
    public List<AgentConversationBO> listConversations(String ownerKey, int limit, String query) {
        return conversationService.list(ownerKey, limit, query).stream()
                .map(AgentConversationPOConverter.INSTANCE::toTarget).toList();
    }

    @Override
    public void saveRun(AgentRunBO run) {
        var source = AgentRunPOConverter.INSTANCE.toSource(run);
        runService.create(source);
        run.setId(source.getId());
        run.setCreatedAt(source.getCreatedAt());
        run.setUpdatedAt(source.getUpdatedAt());
    }

    @Override
    public void updateRun(AgentRunBO run) {
        var source = AgentRunPOConverter.INSTANCE.toSource(run);
        var persisted = runService.find(run.getOwnerKey(), run.getRunNo());
        if (persisted == null || persisted.getId() == null) {
            throw new IllegalStateException("Cannot update Agent run without a persisted row: " + run.getRunNo());
        }
        source.setId(persisted.getId());
        run.setId(persisted.getId());
        runService.update(source);
    }

    @Override
    public void updateConversation(AgentConversationBO conversation) {
        var source = AgentConversationPOConverter.INSTANCE.toSource(conversation);
        var persisted = conversationService.find(conversation.getOwnerKey(), conversation.getConversationNo())
                .orElseThrow(() -> new IllegalStateException(
                        "Cannot update Agent conversation without a persisted row: " + conversation.getConversationNo()));
        source.setId(persisted.getId());
        conversation.setId(persisted.getId());
        conversationService.update(source);
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
    public Optional<AgentRunBO> findRunByIdempotencyKey(String ownerKey, String idempotencyKey) {
        return Optional.ofNullable(runService.findByIdempotencyKey(ownerKey, idempotencyKey))
                .map(AgentRunPOConverter.INSTANCE::toTarget);
    }

    @Override
    public List<AgentRunStepBO> findSteps(String ownerKey, String runNo) {
        return runService.findSteps(ownerKey, runNo).stream()
                .map(AgentRunStepPOConverter.INSTANCE::toTarget).toList();
    }

    @Override
    public void saveMessage(AgentMessageBO message) {
        messageService.create(AgentMessagePOConverter.INSTANCE.toSource(message));
    }

    @Override
    public List<AgentMessageBO> findMessages(String ownerKey, String conversationNo) {
        if (conversationService.find(ownerKey, conversationNo).isEmpty()) return List.of();
        return messageService.find(conversationNo).stream().map(AgentMessagePOConverter.INSTANCE::toTarget).toList();
    }

    @Override
    public List<AgentRunBO> findStaleRunningRuns(OffsetDateTime cutoff) {
        return runService.findStaleRunning(cutoff).stream()
                .map(AgentRunPOConverter.INSTANCE::toTarget).toList();
    }

    @Override
    public void saveEvent(String ownerKey, AgentRunEventBO event) {
        var source = AgentRunEventPOConverter.INSTANCE.toSource(event);
        source.setOwnerKey(ownerKey);
        source.setRunNo(event.getRunNo());
        eventService.create(source);
    }

    @Override
    public List<AgentRunEventBO> findEvents(String ownerKey, String runNo, int afterSequenceNo, int limit) {
        return eventService.find(ownerKey, runNo, afterSequenceNo, limit).stream()
                .map(AgentRunEventPOConverter.INSTANCE::toTarget).toList();
    }

    @Override
    public int archiveBefore(OffsetDateTime cutoff) {
        return retentionService.archiveBefore(cutoff);
    }
}
