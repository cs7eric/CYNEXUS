package cn.cccs7.cynexus.market.infra.convert;

import cn.cccs7.cynexus.market.domain.entity.MarketOverviewBO;
import cn.cccs7.cynexus.market.domain.valueobject.MarketBreadth;
import cn.cccs7.cynexus.market.domain.valueobject.MarketMoneyFlow;
import cn.cccs7.cynexus.market.infra.basic.entity.MarketOverviewPO;
import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import java.math.BigDecimal;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(imports = {
        BigDecimal.class,
        MarketBreadth.class,
        MarketMoneyFlow.class
})
public interface MarketOverviewPOConverter extends BaseMapper<MarketOverviewPO, MarketOverviewBO> {

    MarketOverviewPOConverter INSTANCE = Mappers.getMapper(MarketOverviewPOConverter.class);

    @Override
    @Mapping(target = "breadth", expression = "java(new MarketBreadth(source.getRiseCount(), source.getFallCount(), 0, 0))")
    @Mapping(target = "moneyFlow", expression = "java(new MarketMoneyFlow(BigDecimal.ZERO, source.getNorthboundNetInflow()))")
    MarketOverviewBO toTarget(MarketOverviewPO source);

    @Override
    default MarketOverviewPO toSource(MarketOverviewBO target) {
        if (target == null) {
            return null;
        }
        MarketOverviewPO source = new MarketOverviewPO();
        source.setTradeDate(target.tradeDate());
        source.setIndexChangePercent(target.indexChangePercent());
        source.setTurnoverAmount(target.turnoverAmount());
        source.setRiseCount(target.breadth().riseCount());
        source.setFallCount(target.breadth().fallCount());
        source.setNorthboundNetInflow(target.moneyFlow().northboundNetInflow());
        return source;
    }
}
