package cn.cccs7.cynexus.ai.application.convert;

import cn.cccs7.cynexus.ai.api.dto.AiProviderDTO;
import cn.cccs7.cynexus.ai.domain.entity.AiProviderBO;
import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface AiProviderDTOConverter extends BaseMapper<AiProviderDTO, AiProviderBO> {

    AiProviderDTOConverter INSTANCE = Mappers.getMapper(AiProviderDTOConverter.class);
}
