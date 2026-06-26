package cn.cccs7.cynexus.market.application.assembler;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.market.api.dto.StockDailyFeatureDTO;
import cn.cccs7.cynexus.market.domain.entity.StockDailyFeatureBO;
import cn.cccs7.cynexus.market.domain.valueobject.Stock;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface StockDailyFeatureAssembler extends BaseMapper<StockDailyFeatureBO, StockDailyFeatureDTO> {

    @Override
    @Mapping(target = "symbol", source = "stock.symbol")
    StockDailyFeatureDTO toTarget(StockDailyFeatureBO source);

    @Override
    default StockDailyFeatureBO toSource(StockDailyFeatureDTO target) {
        if (target == null) {
            return null;
        }
        return new StockDailyFeatureBO(new Stock(target.symbol(), target.symbol()), target.tradeDate(),
                target.closePrice(), target.changePercent(), target.turnoverRate(), target.volumeRatio(),
                target.mainNetInflow(), target.trendLabel());
    }
}
