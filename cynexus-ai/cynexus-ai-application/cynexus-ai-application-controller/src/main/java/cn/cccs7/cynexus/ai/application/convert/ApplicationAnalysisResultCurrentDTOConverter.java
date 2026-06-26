package cn.cccs7.cynexus.ai.application.convert;

import cn.cccs7.cynexus.ai.api.dto.ApplicationAnalysisResultCurrentDTO;
import cn.cccs7.cynexus.ai.domain.entity.ApplicationAnalysisResultCurrentBO;
import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface ApplicationAnalysisResultCurrentDTOConverter
        extends BaseMapper<ApplicationAnalysisResultCurrentDTO, ApplicationAnalysisResultCurrentBO> {

    ApplicationAnalysisResultCurrentDTOConverter INSTANCE =
            Mappers.getMapper(ApplicationAnalysisResultCurrentDTOConverter.class);
}
