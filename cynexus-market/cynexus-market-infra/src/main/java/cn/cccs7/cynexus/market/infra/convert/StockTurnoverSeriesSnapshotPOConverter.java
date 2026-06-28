package cn.cccs7.cynexus.market.infra.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.market.domain.entity.StockTurnoverSeriesSnapshotBO;
import cn.cccs7.cynexus.market.infra.basic.entity.StockTurnoverSeriesSnapshotPO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface StockTurnoverSeriesSnapshotPOConverter extends BaseMapper<StockTurnoverSeriesSnapshotPO, StockTurnoverSeriesSnapshotBO> {

    StockTurnoverSeriesSnapshotPOConverter INSTANCE = Mappers.getMapper(StockTurnoverSeriesSnapshotPOConverter.class);
}
