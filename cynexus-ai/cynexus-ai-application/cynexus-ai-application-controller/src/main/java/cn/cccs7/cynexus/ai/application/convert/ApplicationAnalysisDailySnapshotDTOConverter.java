package cn.cccs7.cynexus.ai.application.convert;

import cn.cccs7.cynexus.ai.api.dto.ApplicationAnalysisDailySnapshotDTO;
import cn.cccs7.cynexus.ai.domain.entity.ApplicationAnalysisDailySnapshotBO;
import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface ApplicationAnalysisDailySnapshotDTOConverter
        extends BaseMapper<ApplicationAnalysisDailySnapshotDTO, ApplicationAnalysisDailySnapshotBO> {

    ApplicationAnalysisDailySnapshotDTOConverter INSTANCE =
            Mappers.getMapper(ApplicationAnalysisDailySnapshotDTOConverter.class);
}
