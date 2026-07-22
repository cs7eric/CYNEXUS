package cn.cccs7.cynexus.ai.infra.basic.service.impl.agent;

import cn.cccs7.cynexus.ai.infra.basic.entity.agent.AgentRunPO;
import cn.cccs7.cynexus.ai.infra.basic.entity.agent.AgentRunStepPO;
import cn.cccs7.cynexus.ai.infra.basic.mapper.agent.AgentRunMapper;
import cn.cccs7.cynexus.ai.infra.basic.mapper.agent.AgentRunStepMapper;
import cn.cccs7.cynexus.ai.infra.basic.service.agent.AgentRunBasicService;
import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.util.List;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;

@Service
public class AgentRunBasicServiceImpl implements AgentRunBasicService {
    private final AgentRunMapper runMapper;
    private final AgentRunStepMapper stepMapper;
    private final SnowflakeIdGenerator idGenerator;

    public AgentRunBasicServiceImpl(AgentRunMapper runMapper, AgentRunStepMapper stepMapper,
                                    SnowflakeIdGenerator idGenerator) {
        this.runMapper = runMapper;
        this.stepMapper = stepMapper;
        this.idGenerator = idGenerator;
    }

    @Override
    public void create(AgentRunPO run) {
        if (run.getId() == null) run.setId(idGenerator.nextId());
        runMapper.insert(run);
    }

    @Override
    public void update(AgentRunPO run) {
        var wrapper = new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<AgentRunPO>()
                .eq("id", run.getId());
        if (!"CANCELLED".equals(run.getStatus()) && !"FAILED".equals(run.getStatus())) {
            wrapper.isNull("cancellation_requested_at");
        }
        if (runMapper.update(run, wrapper) != 1) {
            throw new IllegalStateException("Agent run update affected no row: " + run.getRunNo());
        }
    }

    @Override
    public void createStep(AgentRunStepPO step) {
        if (step.getId() == null) step.setId(idGenerator.nextId());
        stepMapper.insert(step);
    }

    @Override
    public AgentRunPO find(String ownerKey, String runNo) {
        return runMapper.selectOne(new QueryWrapper<AgentRunPO>().eq("owner_key", ownerKey)
                .eq("run_no", runNo).isNull("deleted_at"));
    }

    @Override
    public AgentRunPO findByIdempotencyKey(String ownerKey, String idempotencyKey) {
        return runMapper.selectOne(new QueryWrapper<AgentRunPO>().eq("owner_key", ownerKey)
                .eq("idempotency_key", idempotencyKey).isNull("deleted_at"));
    }

    @Override
    public AgentRunPO requestCancellation(String ownerKey, String runNo, String requestedBy) {
        AgentRunPO current = find(ownerKey, runNo);
        if (current == null) return null;
        if (current.getFinishedAt() != null || !"RUNNING".equals(current.getStatus())
                && !"RETRYING".equals(current.getStatus())) return current;
        AgentRunPO update = new AgentRunPO();
        update.setId(current.getId());
        update.setCancellationRequestedAt(OffsetDateTime.now());
        update.setCancellationRequestedBy(requestedBy);
        var wrapper = new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<AgentRunPO>()
                .eq("id", current.getId()).in("status", "RUNNING", "RETRYING")
                .isNull("finished_at").isNull("cancellation_requested_at");
        if (runMapper.update(update, wrapper) != 1) {
            AgentRunPO latest = find(ownerKey, runNo);
            if (latest == null || latest.getFinishedAt() != null || !"RUNNING".equals(latest.getStatus())
                    && !"RETRYING".equals(latest.getStatus())) return latest;
            throw new IllegalStateException("Agent cancellation request affected no row: " + runNo);
        }
        return find(ownerKey, runNo);
    }

    @Override
    public boolean isCancellationRequested(String ownerKey, String runNo) {
        AgentRunPO current = find(ownerKey, runNo);
        return current != null && current.getCancellationRequestedAt() != null;
    }

    @Override
    public List<AgentRunStepPO> findSteps(String ownerKey, String runNo) {
        return stepMapper.selectList(new QueryWrapper<AgentRunStepPO>().eq("run_no", runNo)
                .exists("SELECT 1 FROM ai_agent_run r WHERE r.run_no = ai_agent_run_step.run_no AND r.owner_key = {0}", ownerKey)
                .isNull("deleted_at").orderByAsc("step_no"));
    }

    @Override
    public List<AgentRunPO> findStaleRunning(OffsetDateTime cutoff) {
        return runMapper.selectList(new QueryWrapper<AgentRunPO>().eq("status", "RUNNING")
                .lt("started_at", cutoff).isNull("finished_at").isNull("deleted_at")
                .orderByAsc("started_at").last("LIMIT 100"));
    }
}
