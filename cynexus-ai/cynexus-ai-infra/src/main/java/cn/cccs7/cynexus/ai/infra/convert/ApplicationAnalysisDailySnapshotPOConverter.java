package cn.cccs7.cynexus.ai.infra.convert;

import cn.cccs7.cynexus.ai.domain.entity.ApplicationAnalysisDailySnapshotBO;
import cn.cccs7.cynexus.ai.infra.basic.entity.ApplicationAnalysisDailySnapshotPO;
import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface ApplicationAnalysisDailySnapshotPOConverter
        extends BaseMapper<ApplicationAnalysisDailySnapshotPO, ApplicationAnalysisDailySnapshotBO> {

    ApplicationAnalysisDailySnapshotPOConverter INSTANCE =
            Mappers.getMapper(ApplicationAnalysisDailySnapshotPOConverter.class);
}
