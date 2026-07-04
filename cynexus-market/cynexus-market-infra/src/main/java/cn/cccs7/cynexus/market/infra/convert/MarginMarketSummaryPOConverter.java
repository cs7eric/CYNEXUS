package cn.cccs7.cynexus.market.infra.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.market.domain.entity.MarginMarketSummaryBO;
import cn.cccs7.cynexus.market.infra.basic.entity.MarginMarketSummaryPO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface MarginMarketSummaryPOConverter extends BaseMapper<MarginMarketSummaryPO, MarginMarketSummaryBO> {

    MarginMarketSummaryPOConverter INSTANCE = Mappers.getMapper(MarginMarketSummaryPOConverter.class);
}
