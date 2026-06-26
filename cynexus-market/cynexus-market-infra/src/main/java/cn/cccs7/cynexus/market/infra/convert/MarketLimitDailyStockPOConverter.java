package cn.cccs7.cynexus.market.infra.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.market.domain.entity.MarketLimitDailyStockBO;
import cn.cccs7.cynexus.market.infra.basic.entity.MarketLimitDailyStockPO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface MarketLimitDailyStockPOConverter extends BaseMapper<MarketLimitDailyStockPO, MarketLimitDailyStockBO> {

    MarketLimitDailyStockPOConverter INSTANCE = Mappers.getMapper(MarketLimitDailyStockPOConverter.class);
}
