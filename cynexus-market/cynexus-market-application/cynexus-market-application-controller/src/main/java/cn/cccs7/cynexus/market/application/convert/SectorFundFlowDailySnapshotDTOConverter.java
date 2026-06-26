package cn.cccs7.cynexus.market.application.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.market.api.dto.SectorFundFlowDailySnapshotDTO;
import cn.cccs7.cynexus.market.domain.entity.SectorFundFlowDailySnapshotBO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface SectorFundFlowDailySnapshotDTOConverter
        extends BaseMapper<SectorFundFlowDailySnapshotDTO, SectorFundFlowDailySnapshotBO> {

    SectorFundFlowDailySnapshotDTOConverter INSTANCE =
            Mappers.getMapper(SectorFundFlowDailySnapshotDTOConverter.class);
}
