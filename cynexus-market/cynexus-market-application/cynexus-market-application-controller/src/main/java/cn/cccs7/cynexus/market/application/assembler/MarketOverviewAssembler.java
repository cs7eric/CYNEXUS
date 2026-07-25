package cn.cccs7.cynexus.market.application.assembler;

import cn.cccs7.cynexus.market.api.dto.MarketOverviewDTO;
import cn.cccs7.cynexus.market.domain.entity.MarketOverviewBO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface MarketOverviewAssembler {

    MarketOverviewDTO toDTO(MarketOverviewBO source);
}
