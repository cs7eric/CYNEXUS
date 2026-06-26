package cn.cccs7.cynexus.ai.application.convert;

import cn.cccs7.cynexus.ai.api.dto.ApplicationAnalysisTargetDTO;
import cn.cccs7.cynexus.ai.domain.entity.ApplicationAnalysisTargetBO;
import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface ApplicationAnalysisTargetDTOConverter
        extends BaseMapper<ApplicationAnalysisTargetDTO, ApplicationAnalysisTargetBO> {

    ApplicationAnalysisTargetDTOConverter INSTANCE = Mappers.getMapper(ApplicationAnalysisTargetDTOConverter.class);
}
