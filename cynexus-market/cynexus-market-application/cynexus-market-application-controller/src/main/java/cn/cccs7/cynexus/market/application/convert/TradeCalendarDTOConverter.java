package cn.cccs7.cynexus.market.application.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.market.api.dto.TradeCalendarDTO;
import cn.cccs7.cynexus.market.domain.entity.TradeCalendarBO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface TradeCalendarDTOConverter extends BaseMapper<TradeCalendarDTO, TradeCalendarBO> {

    TradeCalendarDTOConverter INSTANCE = Mappers.getMapper(TradeCalendarDTOConverter.class);
}
