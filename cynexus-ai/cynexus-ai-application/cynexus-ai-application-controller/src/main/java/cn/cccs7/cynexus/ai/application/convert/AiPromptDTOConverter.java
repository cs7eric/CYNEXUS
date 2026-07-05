package cn.cccs7.cynexus.ai.application.convert;

import cn.cccs7.cynexus.ai.api.dto.AiPromptDTO;
import cn.cccs7.cynexus.ai.domain.entity.AiPromptBO;
import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface AiPromptDTOConverter extends BaseMapper<AiPromptDTO, AiPromptBO> {

    AiPromptDTOConverter INSTANCE = Mappers.getMapper(AiPromptDTOConverter.class);
}
