package cn.cccs7.cynexus.ai.infra.convert;

import cn.cccs7.cynexus.ai.domain.entity.AiPromptBO;
import cn.cccs7.cynexus.ai.infra.basic.entity.AiPromptPO;
import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface AiPromptPOConverter extends BaseMapper<AiPromptPO, AiPromptBO> {

    AiPromptPOConverter INSTANCE = Mappers.getMapper(AiPromptPOConverter.class);
}
