package cn.cccs7.cynexus.market.infra.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.market.domain.entity.OverviewDetailSnapshotBO;
import cn.cccs7.cynexus.market.infra.basic.entity.OverviewDetailSnapshotPO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface OverviewDetailSnapshotPOConverter extends BaseMapper<OverviewDetailSnapshotPO, OverviewDetailSnapshotBO> {

    OverviewDetailSnapshotPOConverter INSTANCE = Mappers.getMapper(OverviewDetailSnapshotPOConverter.class);
}
