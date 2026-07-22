package cn.cccs7.cynexus.ai.infra.basic.service.impl.agent;

import cn.cccs7.cynexus.ai.infra.basic.entity.agent.AgentConversationPO;
import cn.cccs7.cynexus.ai.infra.basic.entity.agent.AgentMessagePO;
import cn.cccs7.cynexus.ai.infra.basic.entity.agent.AgentRunEventPO;
import cn.cccs7.cynexus.ai.infra.basic.entity.agent.AgentRunPO;
import cn.cccs7.cynexus.ai.infra.basic.entity.agent.AgentRunStepPO;
import cn.cccs7.cynexus.ai.infra.basic.mapper.agent.AgentConversationMapper;
import cn.cccs7.cynexus.ai.infra.basic.mapper.agent.AgentMessageMapper;
import cn.cccs7.cynexus.ai.infra.basic.mapper.agent.AgentRunEventMapper;
import cn.cccs7.cynexus.ai.infra.basic.mapper.agent.AgentRunMapper;
import cn.cccs7.cynexus.ai.infra.basic.mapper.agent.AgentRunStepMapper;
import cn.cccs7.cynexus.ai.infra.basic.mapper.agent.AgentTaskMapper;
import cn.cccs7.cynexus.ai.infra.basic.mapper.write.AgentWriteAuditMapper;
import cn.cccs7.cynexus.ai.infra.basic.mapper.write.AgentWriteCommandMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AgentRunRetentionBasicServiceImpl implements cn.cccs7.cynexus.ai.infra.basic.service.agent.AgentRunRetentionBasicService {
    private static final String ARCHIVED = "[ARCHIVED_BY_RETENTION_POLICY]";
    private final AgentRunMapper runMapper;
    private final AgentRunStepMapper stepMapper;
    private final AgentMessageMapper messageMapper;
    private final AgentRunEventMapper eventMapper;
    private final AgentConversationMapper conversationMapper;
    private final AgentTaskMapper taskMapper;
    private final AgentWriteCommandMapper writeCommandMapper;
    private final AgentWriteAuditMapper writeAuditMapper;

    public AgentRunRetentionBasicServiceImpl(AgentRunMapper runMapper, AgentRunStepMapper stepMapper,
                                             AgentMessageMapper messageMapper, AgentRunEventMapper eventMapper,
                                             AgentConversationMapper conversationMapper, AgentTaskMapper taskMapper,
                                             AgentWriteCommandMapper writeCommandMapper, AgentWriteAuditMapper writeAuditMapper) {
        this.runMapper = runMapper;
        this.stepMapper = stepMapper;
        this.messageMapper = messageMapper;
        this.eventMapper = eventMapper;
        this.conversationMapper = conversationMapper;
        this.taskMapper = taskMapper;
        this.writeCommandMapper = writeCommandMapper;
        this.writeAuditMapper = writeAuditMapper;
    }

    @Override
    public int archiveBefore(OffsetDateTime cutoff) {
        List<AgentRunPO> runs = runMapper.selectList(new QueryWrapper<AgentRunPO>()
                .in("status", List.of("SUCCEEDED", "NEEDS_CLARIFICATION", "FAILED", "CANCELLED"))
                .lt("finished_at", cutoff).isNull("deleted_at").orderByAsc("finished_at").last("LIMIT 100"));
        int archived = 0;
        for (AgentRunPO run : runs) {
            archiveRun(run);
            archived++;
        }
        taskMapper.archiveBefore(cutoff);
        writeCommandMapper.archiveBefore(cutoff);
        writeAuditMapper.redactBefore(cutoff);
        return archived;
    }

    private void archiveRun(AgentRunPO run) {
        run.setStatus("ARCHIVED");
        run.setUserInput(ARCHIVED);
        run.setAnswer(ARCHIVED);
        run.setPlan("{}");
        run.setContextJson("{}");
        run.setAssumptionsJson("[]");
        run.setClarificationJson("null");
        run.setEvidenceJson("[]");
        run.setWarningsJson("[]");
        run.setErrorMessage(null);
        run.setUpdatedAt(OffsetDateTime.now());
        runMapper.updateById(run);

        List<AgentRunStepPO> steps = stepMapper.selectList(new QueryWrapper<AgentRunStepPO>()
                .eq("owner_key", run.getOwnerKey()).eq("run_no", run.getRunNo()).isNull("deleted_at"));
        for (AgentRunStepPO step : steps) {
            step.setInputJson("{}");
            step.setOutputJson("{}");
            step.setSummary(ARCHIVED);
            step.setErrorMessage(null);
            step.setUpdatedAt(OffsetDateTime.now());
            stepMapper.updateById(step);
        }

        List<AgentMessagePO> messages = messageMapper.selectList(new QueryWrapper<AgentMessagePO>()
                .eq("owner_key", run.getOwnerKey()).eq("conversation_no", run.getConversationNo())
                .isNull("deleted_at"));
        for (AgentMessagePO message : messages) {
            message.setContent(ARCHIVED);
            message.setToolCalls("[]");
            message.setUpdatedAt(OffsetDateTime.now());
            messageMapper.updateById(message);
        }

        List<AgentRunEventPO> events = eventMapper.selectList(new QueryWrapper<AgentRunEventPO>()
                .eq("owner_key", run.getOwnerKey()).eq("run_no", run.getRunNo()).isNull("deleted_at"));
        for (AgentRunEventPO event : events) {
            event.setAnswer(null);
            event.setDelta(null);
            event.setSummary(ARCHIVED);
            event.setErrorMessage(null);
            event.setUpdatedAt(OffsetDateTime.now());
            eventMapper.updateById(event);
        }

        AgentConversationPO conversation = conversationMapper.selectOne(new QueryWrapper<AgentConversationPO>()
                .eq("owner_key", run.getOwnerKey()).eq("conversation_no", run.getConversationNo()).isNull("deleted_at"));
        if (conversation != null) {
            conversation.setStatus("ARCHIVED");
            conversation.setLastMessage(ARCHIVED);
            conversation.setMetadata("{}");
            conversation.setUpdatedAt(OffsetDateTime.now());
            conversationMapper.updateById(conversation);
        }
    }
}
