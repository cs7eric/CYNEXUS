package cn.cccs7.cynexus.ai.infra.basic.service.impl.agent;

import cn.cccs7.cynexus.ai.infra.basic.entity.agent.AgentRunEventPO;
import cn.cccs7.cynexus.ai.infra.basic.mapper.agent.AgentRunEventMapper;
import cn.cccs7.cynexus.ai.infra.basic.service.agent.AgentRunEventBasicService;
import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AgentRunEventBasicServiceImpl implements AgentRunEventBasicService {
    private final AgentRunEventMapper mapper;
    private final SnowflakeIdGenerator idGenerator;

    public AgentRunEventBasicServiceImpl(AgentRunEventMapper mapper, SnowflakeIdGenerator idGenerator) {
        this.mapper = mapper;
        this.idGenerator = idGenerator;
    }

    @Override
    public void create(AgentRunEventPO event) {
        if (event.getId() == null) event.setId(idGenerator.nextId());
        mapper.insert(event);
    }

    @Override
    public List<AgentRunEventPO> find(String ownerKey, String runNo, int afterSequenceNo, int limit) {
        int boundedLimit = Math.max(1, Math.min(limit, 200));
        return mapper.selectList(new QueryWrapper<AgentRunEventPO>()
                .eq("owner_key", ownerKey)
                .eq("run_no", runNo)
                .gt("sequence_no", Math.max(0, afterSequenceNo))
                .isNull("deleted_at")
                .orderByAsc("sequence_no")
                .last("LIMIT " + boundedLimit));
    }
}
