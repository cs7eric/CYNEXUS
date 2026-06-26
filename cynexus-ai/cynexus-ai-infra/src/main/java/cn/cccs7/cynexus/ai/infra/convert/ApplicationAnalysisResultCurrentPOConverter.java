package cn.cccs7.cynexus.ai.infra.convert;

import cn.cccs7.cynexus.ai.domain.entity.ApplicationAnalysisResultCurrentBO;
import cn.cccs7.cynexus.ai.infra.basic.entity.ApplicationAnalysisResultCurrentPO;
import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface ApplicationAnalysisResultCurrentPOConverter
        extends BaseMapper<ApplicationAnalysisResultCurrentPO, ApplicationAnalysisResultCurrentBO> {

    ApplicationAnalysisResultCurrentPOConverter INSTANCE =
            Mappers.getMapper(ApplicationAnalysisResultCurrentPOConverter.class);
}
