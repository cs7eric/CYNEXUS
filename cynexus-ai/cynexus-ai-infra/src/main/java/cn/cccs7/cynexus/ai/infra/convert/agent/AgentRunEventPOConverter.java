package cn.cccs7.cynexus.ai.infra.convert.agent;

import cn.cccs7.cynexus.ai.domain.entity.agent.AgentRunEventBO;
import cn.cccs7.cynexus.ai.infra.basic.entity.agent.AgentRunEventPO;
import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface AgentRunEventPOConverter extends BaseMapper<AgentRunEventPO, AgentRunEventBO> {
    AgentRunEventPOConverter INSTANCE = Mappers.getMapper(AgentRunEventPOConverter.class);

    @Override
    @Mapping(source = "eventType", target = "type")
    AgentRunEventBO toTarget(AgentRunEventPO source);

    @Override
    @Mapping(source = "type", target = "eventType")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "ownerKey", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    AgentRunEventPO toSource(AgentRunEventBO target);
}
