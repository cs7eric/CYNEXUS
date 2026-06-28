package cn.cccs7.cynexus.market.infra.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.market.domain.entity.StockFundFlowCaptureBatchBO;
import cn.cccs7.cynexus.market.infra.basic.entity.StockFundFlowCaptureBatchPO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface StockFundFlowCaptureBatchPOConverter extends BaseMapper<StockFundFlowCaptureBatchPO, StockFundFlowCaptureBatchBO> {

    StockFundFlowCaptureBatchPOConverter INSTANCE = Mappers.getMapper(StockFundFlowCaptureBatchPOConverter.class);
}
