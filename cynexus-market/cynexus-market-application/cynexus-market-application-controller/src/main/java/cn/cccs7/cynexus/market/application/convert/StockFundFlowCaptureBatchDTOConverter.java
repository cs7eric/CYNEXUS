package cn.cccs7.cynexus.market.application.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.market.api.dto.StockFundFlowCaptureBatchDTO;
import cn.cccs7.cynexus.market.domain.entity.StockFundFlowCaptureBatchBO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface StockFundFlowCaptureBatchDTOConverter extends BaseMapper<StockFundFlowCaptureBatchDTO, StockFundFlowCaptureBatchBO> {

    StockFundFlowCaptureBatchDTOConverter INSTANCE = Mappers.getMapper(StockFundFlowCaptureBatchDTOConverter.class);
}
