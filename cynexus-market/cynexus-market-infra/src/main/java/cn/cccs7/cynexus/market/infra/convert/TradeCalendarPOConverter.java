package cn.cccs7.cynexus.market.infra.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.market.domain.entity.TradeCalendarBO;
import cn.cccs7.cynexus.market.infra.basic.entity.TradeCalendarPO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface TradeCalendarPOConverter extends BaseMapper<TradeCalendarPO, TradeCalendarBO> {

    TradeCalendarPOConverter INSTANCE = Mappers.getMapper(TradeCalendarPOConverter.class);
}
