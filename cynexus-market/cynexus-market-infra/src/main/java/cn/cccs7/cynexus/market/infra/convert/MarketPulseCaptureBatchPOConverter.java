package cn.cccs7.cynexus.market.infra.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.market.domain.entity.MarketPulseCaptureBatchBO;
import cn.cccs7.cynexus.market.infra.basic.entity.MarketPulseCaptureBatchPO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface MarketPulseCaptureBatchPOConverter extends BaseMapper<MarketPulseCaptureBatchPO, MarketPulseCaptureBatchBO> {

    MarketPulseCaptureBatchPOConverter INSTANCE = Mappers.getMapper(MarketPulseCaptureBatchPOConverter.class);
}
