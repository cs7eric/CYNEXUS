package cn.cccs7.cynexus.ai.infra.convert;

import cn.cccs7.cynexus.ai.domain.entity.ApplicationAnalysisTargetBO;
import cn.cccs7.cynexus.ai.infra.basic.entity.ApplicationAnalysisTargetPO;
import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface ApplicationAnalysisTargetPOConverter extends BaseMapper<ApplicationAnalysisTargetPO, ApplicationAnalysisTargetBO> {

    ApplicationAnalysisTargetPOConverter INSTANCE = Mappers.getMapper(ApplicationAnalysisTargetPOConverter.class);
}
