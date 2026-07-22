package cn.cccs7.cynexus.ai.infra.convert.agent;

import cn.cccs7.cynexus.ai.domain.entity.agent.AgentRunStepBO;
import cn.cccs7.cynexus.ai.infra.basic.entity.agent.AgentRunStepPO;
import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface AgentRunStepPOConverter extends BaseMapper<AgentRunStepPO, AgentRunStepBO> {
    @Mapping(target = "ownerKey", ignore = true)
    AgentRunStepPO toSource(AgentRunStepBO target);

    AgentRunStepPOConverter INSTANCE = Mappers.getMapper(AgentRunStepPOConverter.class);
}
