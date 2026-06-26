package cn.cccs7.cynexus.market.application.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.market.api.dto.MarketOverviewDTO;
import cn.cccs7.cynexus.market.domain.entity.MarketOverviewBO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface MarketOverviewDTOConverter extends BaseMapper<MarketOverviewDTO, MarketOverviewBO> {

    MarketOverviewDTOConverter INSTANCE = Mappers.getMapper(MarketOverviewDTOConverter.class);
}
