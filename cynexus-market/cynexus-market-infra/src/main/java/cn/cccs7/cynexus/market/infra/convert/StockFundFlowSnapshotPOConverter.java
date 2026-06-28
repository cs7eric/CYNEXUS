package cn.cccs7.cynexus.market.infra.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.market.domain.entity.StockFundFlowSnapshotBO;
import cn.cccs7.cynexus.market.infra.basic.entity.StockFundFlowSnapshotPO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface StockFundFlowSnapshotPOConverter extends BaseMapper<StockFundFlowSnapshotPO, StockFundFlowSnapshotBO> {

    StockFundFlowSnapshotPOConverter INSTANCE = Mappers.getMapper(StockFundFlowSnapshotPOConverter.class);
}
