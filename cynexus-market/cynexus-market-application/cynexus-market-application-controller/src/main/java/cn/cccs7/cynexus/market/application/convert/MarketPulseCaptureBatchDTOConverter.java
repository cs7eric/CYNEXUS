package cn.cccs7.cynexus.market.application.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.market.api.dto.MarketPulseCaptureBatchDTO;
import cn.cccs7.cynexus.market.domain.entity.MarketPulseCaptureBatchBO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface MarketPulseCaptureBatchDTOConverter
        extends BaseMapper<MarketPulseCaptureBatchDTO, MarketPulseCaptureBatchBO> {

    MarketPulseCaptureBatchDTOConverter INSTANCE = Mappers.getMapper(MarketPulseCaptureBatchDTOConverter.class);
}
