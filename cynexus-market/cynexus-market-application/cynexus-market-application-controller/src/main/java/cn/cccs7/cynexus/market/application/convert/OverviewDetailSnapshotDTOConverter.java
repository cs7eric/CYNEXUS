package cn.cccs7.cynexus.market.application.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.market.api.dto.OverviewDetailSnapshotDTO;
import cn.cccs7.cynexus.market.domain.entity.OverviewDetailSnapshotBO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface OverviewDetailSnapshotDTOConverter extends BaseMapper<OverviewDetailSnapshotDTO, OverviewDetailSnapshotBO> {

    OverviewDetailSnapshotDTOConverter INSTANCE = Mappers.getMapper(OverviewDetailSnapshotDTOConverter.class);
}
