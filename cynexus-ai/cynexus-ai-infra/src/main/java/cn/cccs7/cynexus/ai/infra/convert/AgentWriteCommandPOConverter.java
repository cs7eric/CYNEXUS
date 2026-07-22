package cn.cccs7.cynexus.ai.infra.convert;

import cn.cccs7.cynexus.ai.domain.agent.AgentWriteCommandBO;
import cn.cccs7.cynexus.ai.infra.basic.entity.AgentWriteCommandPO;
import org.mapstruct.Mapping;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface AgentWriteCommandPOConverter {
    AgentWriteCommandPOConverter INSTANCE = Mappers.getMapper(AgentWriteCommandPOConverter.class);
    @Mapping(target = "deletedAt", ignore = true)
    AgentWriteCommandPO toSource(AgentWriteCommandBO source);
    AgentWriteCommandBO toTarget(AgentWriteCommandPO source);
}
