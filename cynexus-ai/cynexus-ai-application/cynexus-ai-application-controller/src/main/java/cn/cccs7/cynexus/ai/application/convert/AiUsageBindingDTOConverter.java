package cn.cccs7.cynexus.ai.application.convert;

import cn.cccs7.cynexus.ai.api.dto.AiUsageBindingDTO;
import cn.cccs7.cynexus.ai.domain.entity.AiUsageBindingBO;
import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface AiUsageBindingDTOConverter extends BaseMapper<AiUsageBindingDTO, AiUsageBindingBO> {

    AiUsageBindingDTOConverter INSTANCE = Mappers.getMapper(AiUsageBindingDTOConverter.class);
}
