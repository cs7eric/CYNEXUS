package cn.cccs7.cynexus.ai.infra.convert;

import cn.cccs7.cynexus.ai.domain.entity.AiExecutionLogBO;
import cn.cccs7.cynexus.ai.infra.basic.entity.AiExecutionLogPO;
import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface AiExecutionLogPOConverter extends BaseMapper<AiExecutionLogPO, AiExecutionLogBO> {

    AiExecutionLogPOConverter INSTANCE = Mappers.getMapper(AiExecutionLogPOConverter.class);
}
