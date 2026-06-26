package cn.cccs7.cynexus.market.application.assembler;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.market.api.dto.MarketOverviewDTO;
import cn.cccs7.cynexus.market.domain.entity.MarketOverviewBO;
import cn.cccs7.cynexus.market.domain.valueobject.MarketBreadth;
import cn.cccs7.cynexus.market.domain.valueobject.MarketMoneyFlow;
import java.math.BigDecimal;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface MarketOverviewAssembler extends BaseMapper<MarketOverviewBO, MarketOverviewDTO> {

    @Override
    @Mapping(target = "northboundNetInflow", source = "moneyFlow.northboundNetInflow")
    @Mapping(target = "riseCount", source = "breadth.riseCount")
    @Mapping(target = "fallCount", source = "breadth.fallCount")
    @Mapping(target = "summary", constant = "market overview")
    MarketOverviewDTO toTarget(MarketOverviewBO source);

    @Override
    default MarketOverviewBO toSource(MarketOverviewDTO target) {
        if (target == null) {
            return null;
        }
        return new MarketOverviewBO(target.tradeDate(), target.indexChangePercent(), target.turnoverAmount(),
                new MarketBreadth(target.riseCount(), target.fallCount(), 0, 0),
                new MarketMoneyFlow(BigDecimal.ZERO, target.northboundNetInflow()));
    }
}
