package cn.cccs7.cynexus.ai.infra.convert;

import cn.cccs7.cynexus.ai.domain.entity.AiUsageBindingBO;
import cn.cccs7.cynexus.ai.infra.basic.entity.AiUsageBindingPO;
import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface AiUsageBindingPOConverter extends BaseMapper<AiUsageBindingPO, AiUsageBindingBO> {

    AiUsageBindingPOConverter INSTANCE = Mappers.getMapper(AiUsageBindingPOConverter.class);
}
