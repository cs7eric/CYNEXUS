package cn.cccs7.cynexus.market.application.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.market.api.dto.StockFundFlowSnapshotDTO;
import cn.cccs7.cynexus.market.domain.entity.StockFundFlowSnapshotBO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface StockFundFlowSnapshotDTOConverter extends BaseMapper<StockFundFlowSnapshotDTO, StockFundFlowSnapshotBO> {

    StockFundFlowSnapshotDTOConverter INSTANCE = Mappers.getMapper(StockFundFlowSnapshotDTOConverter.class);
}
