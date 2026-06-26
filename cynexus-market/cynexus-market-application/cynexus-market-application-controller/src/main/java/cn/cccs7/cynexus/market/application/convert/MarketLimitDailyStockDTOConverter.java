package cn.cccs7.cynexus.market.application.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.market.api.dto.MarketLimitDailyStockDTO;
import cn.cccs7.cynexus.market.domain.entity.MarketLimitDailyStockBO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface MarketLimitDailyStockDTOConverter
        extends BaseMapper<MarketLimitDailyStockDTO, MarketLimitDailyStockBO> {

    MarketLimitDailyStockDTOConverter INSTANCE = Mappers.getMapper(MarketLimitDailyStockDTOConverter.class);
}
