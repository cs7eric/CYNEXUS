package cn.cccs7.cynexus.market.infra.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.market.domain.entity.SectorFundFlowDailySnapshotBO;
import cn.cccs7.cynexus.market.infra.basic.entity.SectorFundFlowDailySnapshotPO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface SectorFundFlowDailySnapshotPOConverter
        extends BaseMapper<SectorFundFlowDailySnapshotPO, SectorFundFlowDailySnapshotBO> {

    SectorFundFlowDailySnapshotPOConverter INSTANCE = Mappers.getMapper(SectorFundFlowDailySnapshotPOConverter.class);
}
