package cn.cccs7.cynexus.ai.application.convert;

import cn.cccs7.cynexus.ai.api.dto.AgentQuotaPolicyDTO;
import cn.cccs7.cynexus.ai.domain.entity.AgentQuotaPolicyBO;
import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface AgentQuotaPolicyDTOConverter extends BaseMapper<AgentQuotaPolicyDTO, AgentQuotaPolicyBO> {
    AgentQuotaPolicyDTOConverter INSTANCE = Mappers.getMapper(AgentQuotaPolicyDTOConverter.class);
}
