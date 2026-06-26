package cn.cccs7.cynexus.ai.application.convert;

import cn.cccs7.cynexus.ai.api.dto.ApplicationAnalysisResultHistoryDTO;
import cn.cccs7.cynexus.ai.domain.entity.ApplicationAnalysisResultHistoryBO;
import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface ApplicationAnalysisResultHistoryDTOConverter
        extends BaseMapper<ApplicationAnalysisResultHistoryDTO, ApplicationAnalysisResultHistoryBO> {

    ApplicationAnalysisResultHistoryDTOConverter INSTANCE =
            Mappers.getMapper(ApplicationAnalysisResultHistoryDTOConverter.class);
}
