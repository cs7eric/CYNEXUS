package cn.cccs7.cynexus.ai.infra.basic.service.impl;

import cn.cccs7.cynexus.ai.infra.basic.entity.AgentRunPO;
import cn.cccs7.cynexus.ai.infra.basic.entity.AgentRunStepPO;
import cn.cccs7.cynexus.ai.infra.basic.mapper.AgentRunMapper;
import cn.cccs7.cynexus.ai.infra.basic.mapper.AgentRunStepMapper;
import cn.cccs7.cynexus.ai.infra.basic.service.AgentRunBasicService;
import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.util.List;
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
        runMapper.updateById(run);
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
    public List<AgentRunStepPO> findSteps(String ownerKey, String runNo) {
        return stepMapper.selectList(new QueryWrapper<AgentRunStepPO>().eq("run_no", runNo)
                .exists("SELECT 1 FROM ai_agent_run r WHERE r.run_no = ai_agent_run_step.run_no AND r.owner_key = {0}", ownerKey)
                .isNull("deleted_at").orderByAsc("step_no"));
    }
}
