package cn.cccs7.cynexus.ai.infra.convert;

import cn.cccs7.cynexus.ai.domain.entity.AgentQuotaPolicyBO;
import cn.cccs7.cynexus.ai.infra.basic.entity.agent.AgentQuotaPolicyPO;
import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface AgentQuotaPolicyPOConverter extends BaseMapper<AgentQuotaPolicyPO, AgentQuotaPolicyBO> {
    AgentQuotaPolicyPOConverter INSTANCE = Mappers.getMapper(AgentQuotaPolicyPOConverter.class);
}
