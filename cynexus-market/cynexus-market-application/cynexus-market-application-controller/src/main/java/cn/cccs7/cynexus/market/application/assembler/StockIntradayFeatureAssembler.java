package cn.cccs7.cynexus.market.application.assembler;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.market.api.dto.StockIntradayFeatureDTO;
import cn.cccs7.cynexus.market.domain.entity.StockIntradayFeatureBO;
import cn.cccs7.cynexus.market.domain.valueobject.Stock;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface StockIntradayFeatureAssembler extends BaseMapper<StockIntradayFeatureBO, StockIntradayFeatureDTO> {

    @Override
    @Mapping(target = "symbol", source = "stock.symbol")
    StockIntradayFeatureDTO toTarget(StockIntradayFeatureBO source);

    @Override
    default StockIntradayFeatureBO toSource(StockIntradayFeatureDTO target) {
        if (target == null) {
            return null;
        }
        return new StockIntradayFeatureBO(new Stock(target.getSymbol(), target.getSymbol()), target.getTradeDate(),
                target.getLatestPrice(), target.getAmplitude(), target.getVolumeAcceleration(),
                target.getMainNetInflow(), target.getIntradayLabel());
    }
}
