package cn.cccs7.cynexus.market.application.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.market.api.dto.HeatmapSnapshotDTO;
import cn.cccs7.cynexus.market.domain.entity.HeatmapSnapshotBO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface HeatmapSnapshotDTOConverter extends BaseMapper<HeatmapSnapshotDTO, HeatmapSnapshotBO> {

    HeatmapSnapshotDTOConverter INSTANCE = Mappers.getMapper(HeatmapSnapshotDTOConverter.class);
}
