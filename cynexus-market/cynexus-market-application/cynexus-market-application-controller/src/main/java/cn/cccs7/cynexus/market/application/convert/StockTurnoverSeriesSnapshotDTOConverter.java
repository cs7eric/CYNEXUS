package cn.cccs7.cynexus.market.application.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.market.api.dto.StockTurnoverSeriesSnapshotDTO;
import cn.cccs7.cynexus.market.domain.entity.StockTurnoverSeriesSnapshotBO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface StockTurnoverSeriesSnapshotDTOConverter extends BaseMapper<StockTurnoverSeriesSnapshotDTO, StockTurnoverSeriesSnapshotBO> {

    StockTurnoverSeriesSnapshotDTOConverter INSTANCE = Mappers.getMapper(StockTurnoverSeriesSnapshotDTOConverter.class);
}
