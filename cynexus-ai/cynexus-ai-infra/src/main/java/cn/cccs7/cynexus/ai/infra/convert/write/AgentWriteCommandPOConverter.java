package cn.cccs7.cynexus.ai.infra.convert.write;

import cn.cccs7.cynexus.ai.domain.entity.agent.AgentWriteCommandBO;
import cn.cccs7.cynexus.ai.infra.basic.entity.write.AgentWriteCommandPO;
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
