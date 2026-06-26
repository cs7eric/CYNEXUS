package cn.cccs7.cynexus.ai.infra.convert;

import cn.cccs7.cynexus.ai.domain.entity.ApplicationAnalysisResultHistoryBO;
import cn.cccs7.cynexus.ai.infra.basic.entity.ApplicationAnalysisResultHistoryPO;
import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface ApplicationAnalysisResultHistoryPOConverter
        extends BaseMapper<ApplicationAnalysisResultHistoryPO, ApplicationAnalysisResultHistoryBO> {

    ApplicationAnalysisResultHistoryPOConverter INSTANCE =
            Mappers.getMapper(ApplicationAnalysisResultHistoryPOConverter.class);
}
