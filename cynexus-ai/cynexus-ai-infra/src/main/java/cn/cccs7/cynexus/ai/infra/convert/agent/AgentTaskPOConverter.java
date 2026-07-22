package cn.cccs7.cynexus.ai.infra.convert.agent;

import cn.cccs7.cynexus.ai.domain.entity.agent.AgentTaskBO;
import cn.cccs7.cynexus.ai.infra.basic.entity.agent.AgentTaskPO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface AgentTaskPOConverter {
    AgentTaskPOConverter INSTANCE = Mappers.getMapper(AgentTaskPOConverter.class);
    @Mapping(target = "deletedAt", ignore = true)
    AgentTaskPO toSource(AgentTaskBO source);
    AgentTaskBO toTarget(AgentTaskPO source);
}
