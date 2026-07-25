package cn.cccs7.cynexus.market.application.assembler;

import cn.cccs7.cynexus.market.api.dto.StockDailyFeatureDTO;
import cn.cccs7.cynexus.market.domain.entity.StockDailyFeatureBO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface StockDailyFeatureAssembler {

    @Mapping(target = "symbol", source = "stock.symbol")
    StockDailyFeatureDTO toDTO(StockDailyFeatureBO source);
}
