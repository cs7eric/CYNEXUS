package cn.cccs7.cynexus.market.infra.convert;

import cn.cccs7.cynexus.market.domain.entity.MarketOverviewBO;
import cn.cccs7.cynexus.market.infra.basic.entity.MarketOverviewPO;
import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface MarketOverviewPOConverter extends BaseMapper<MarketOverviewPO, MarketOverviewBO> {

    MarketOverviewPOConverter INSTANCE = Mappers.getMapper(MarketOverviewPOConverter.class);
}
