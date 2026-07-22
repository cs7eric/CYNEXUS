package cn.cccs7.cynexus.ai.infra.basic.service.impl.capability;

import cn.cccs7.cynexus.ai.infra.basic.entity.capability.AgentCapabilityPO;
import cn.cccs7.cynexus.ai.infra.basic.mapper.capability.AgentCapabilityMapper;
import cn.cccs7.cynexus.ai.infra.basic.service.capability.AgentCapabilityBasicService;
import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AgentCapabilityBasicServiceImpl implements AgentCapabilityBasicService {
    private final AgentCapabilityMapper mapper;
    private final SnowflakeIdGenerator idGenerator;

    public AgentCapabilityBasicServiceImpl(AgentCapabilityMapper mapper, SnowflakeIdGenerator idGenerator) {
        this.mapper = mapper;
        this.idGenerator = idGenerator;
    }

    @Override
    public AgentCapabilityPO find(String code, String version) {
        return mapper.selectOne(new QueryWrapper<AgentCapabilityPO>().eq("capability_code", code)
                .eq("version", version).isNull("deleted_at"));
    }

    @Override
    public AgentCapabilityPO save(AgentCapabilityPO capability) {
        if (capability.getId() == null) capability.setId(idGenerator.nextId());
        OffsetDateTime now = OffsetDateTime.now();
        capability.setCreatedAt(now);
        capability.setUpdatedAt(now);
        mapper.insert(capability);
        return capability;
    }

    @Override
    public boolean update(AgentCapabilityPO capability) {
        capability.setUpdatedAt(OffsetDateTime.now());
        return mapper.updateById(capability) > 0;
    }

    @Override
    public List<AgentCapabilityPO> listAll() {
        return mapper.selectList(new QueryWrapper<AgentCapabilityPO>().isNull("deleted_at")
                .orderByAsc("priority").orderByAsc("capability_code"));
    }

    @Override
    public List<AgentCapabilityPO> listRuntime() {
        return mapper.selectList(new QueryWrapper<AgentCapabilityPO>().isNull("deleted_at")
                .eq("is_enabled", true).eq("availability", "AVAILABLE").eq("governance_availability", "AVAILABLE")
                .and(wrapper -> wrapper.isNull("stale_at").or().gt("stale_at", OffsetDateTime.now()))
                .orderByAsc("priority").orderByAsc("capability_code"));
    }

    @Override
    public boolean updateGovernance(String code, boolean enabled, String availability, int priority) {
        return mapper.updateGovernance(code, enabled, availability, priority) > 0;
    }

    @Override
    public void markUnseenUnavailable(OffsetDateTime seenAt) {
        mapper.markUnseenUnavailable(seenAt);
    }

    @Override
    public void markOlderVersionsUnavailable(String code, String version) {
        mapper.markOlderVersionsUnavailable(code, version);
    }
}
