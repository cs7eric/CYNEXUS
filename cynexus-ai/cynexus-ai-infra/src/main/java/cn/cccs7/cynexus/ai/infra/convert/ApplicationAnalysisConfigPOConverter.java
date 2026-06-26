package cn.cccs7.cynexus.ai.infra.convert;

import cn.cccs7.cynexus.ai.domain.entity.ApplicationAnalysisConfigBO;
import cn.cccs7.cynexus.ai.infra.basic.entity.ApplicationAnalysisConfigPO;
import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface ApplicationAnalysisConfigPOConverter extends BaseMapper<ApplicationAnalysisConfigPO, ApplicationAnalysisConfigBO> {

    ApplicationAnalysisConfigPOConverter INSTANCE = Mappers.getMapper(ApplicationAnalysisConfigPOConverter.class);
}
