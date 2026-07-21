package cn.cccs7.cynexus.ai.infra.convert;

import cn.cccs7.cynexus.ai.domain.agent.AgentRunStepBO;
import cn.cccs7.cynexus.ai.infra.basic.entity.AgentRunStepPO;
import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface AgentRunStepPOConverter extends BaseMapper<AgentRunStepPO, AgentRunStepBO> {
    AgentRunStepPOConverter INSTANCE = Mappers.getMapper(AgentRunStepPOConverter.class);
}
