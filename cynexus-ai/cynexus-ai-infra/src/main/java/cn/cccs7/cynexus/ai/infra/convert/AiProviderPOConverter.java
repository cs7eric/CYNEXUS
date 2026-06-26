package cn.cccs7.cynexus.ai.infra.convert;

import cn.cccs7.cynexus.ai.domain.entity.AiProviderBO;
import cn.cccs7.cynexus.ai.infra.basic.entity.AiProviderPO;
import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface AiProviderPOConverter extends BaseMapper<AiProviderPO, AiProviderBO> {

    AiProviderPOConverter INSTANCE = Mappers.getMapper(AiProviderPOConverter.class);
}
