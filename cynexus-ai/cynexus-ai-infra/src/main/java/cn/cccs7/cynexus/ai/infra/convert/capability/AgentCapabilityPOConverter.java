package cn.cccs7.cynexus.ai.infra.convert.capability;

import cn.cccs7.cynexus.ai.domain.entity.capability.AgentCapabilityDescriptor;
import cn.cccs7.cynexus.ai.infra.basic.entity.capability.AgentCapabilityPO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface AgentCapabilityPOConverter {
    AgentCapabilityPOConverter INSTANCE = Mappers.getMapper(AgentCapabilityPOConverter.class);
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    AgentCapabilityPO toSource(AgentCapabilityDescriptor source);
    AgentCapabilityDescriptor toTarget(AgentCapabilityPO source);
}
