package cn.cccs7.cynexus.market.application.assembler;

import cn.cccs7.cynexus.market.api.dto.StockIntradayFeatureDTO;
import cn.cccs7.cynexus.market.domain.entity.StockIntradayFeatureBO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface StockIntradayFeatureAssembler {

    @Mapping(target = "symbol", source = "stock.symbol")
    StockIntradayFeatureDTO toDTO(StockIntradayFeatureBO source);
}
