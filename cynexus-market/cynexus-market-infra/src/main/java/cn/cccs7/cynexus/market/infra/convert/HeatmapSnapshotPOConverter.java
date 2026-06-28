package cn.cccs7.cynexus.market.infra.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.market.domain.entity.HeatmapSnapshotBO;
import cn.cccs7.cynexus.market.infra.basic.entity.HeatmapSnapshotPO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface HeatmapSnapshotPOConverter extends BaseMapper<HeatmapSnapshotPO, HeatmapSnapshotBO> {

    HeatmapSnapshotPOConverter INSTANCE = Mappers.getMapper(HeatmapSnapshotPOConverter.class);
}
