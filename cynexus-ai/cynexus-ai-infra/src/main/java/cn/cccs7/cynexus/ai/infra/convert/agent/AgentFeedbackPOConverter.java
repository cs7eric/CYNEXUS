package cn.cccs7.cynexus.ai.infra.convert.agent;

import cn.cccs7.cynexus.ai.domain.entity.agent.AgentFeedbackBO;
import cn.cccs7.cynexus.ai.infra.basic.entity.agent.AgentFeedbackPO;
import org.mapstruct.Mapping;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface AgentFeedbackPOConverter {
    AgentFeedbackPOConverter INSTANCE = Mappers.getMapper(AgentFeedbackPOConverter.class);
    @Mapping(target = "deletedAt", ignore = true)
    AgentFeedbackPO toSource(AgentFeedbackBO source);
    AgentFeedbackBO toTarget(AgentFeedbackPO source);
}
