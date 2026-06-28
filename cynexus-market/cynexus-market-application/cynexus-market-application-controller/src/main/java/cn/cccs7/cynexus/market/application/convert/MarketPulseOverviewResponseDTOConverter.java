package cn.cccs7.cynexus.market.application.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.market.api.dto.MarketPulseOverviewDTO;
import cn.cccs7.cynexus.market.domain.entity.MarketOverviewBO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MarketPulseOverviewResponseDTOConverter extends BaseMapper<MarketPulseOverviewDTO, MarketOverviewBO> {

    MarketPulseOverviewResponseDTOConverter INSTANCE = Mappers.getMapper(MarketPulseOverviewResponseDTOConverter.class);

    @Override
    @Mapping(target = "ok", constant = "true")
    @Mapping(target = "error", ignore = true)
    @Mapping(target = "tradingDate", source = "tradeDate")
    @Mapping(target = "fetchedAt", source = "updatedAt")
    @Mapping(target = "isTradeTime", constant = "false")
    @Mapping(target = "superLargeNetInflowRatio", source = "superLargeNetRatio")
    @Mapping(target = "largeNetInflowRatio", source = "largeNetRatio")
    @Mapping(target = "mediumNetInflowRatio", source = "mediumNetRatio")
    @Mapping(target = "smallNetInflowRatio", source = "smallNetRatio")
    @Mapping(target = "prevDayFlow", ignore = true)
    @Mapping(target = "prevDayTradingDate", ignore = true)
    MarketPulseOverviewDTO toSource(MarketOverviewBO target);
}
