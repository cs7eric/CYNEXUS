package cn.cccs7.cynexus.ai.infra.convert;

import cn.cccs7.cynexus.ai.domain.agent.AgentFeedbackBO;
import cn.cccs7.cynexus.ai.infra.basic.entity.AgentFeedbackPO;
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
