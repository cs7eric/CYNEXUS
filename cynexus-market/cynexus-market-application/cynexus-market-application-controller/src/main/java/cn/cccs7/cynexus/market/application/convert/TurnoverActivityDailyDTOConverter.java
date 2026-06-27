package cn.cccs7.cynexus.market.application.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.market.api.dto.TurnoverActivityDailyDTO;
import cn.cccs7.cynexus.market.domain.entity.TurnoverActivityDailyBO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface TurnoverActivityDailyDTOConverter extends BaseMapper<TurnoverActivityDailyDTO, TurnoverActivityDailyBO> {

    TurnoverActivityDailyDTOConverter INSTANCE = Mappers.getMapper(TurnoverActivityDailyDTOConverter.class);
}
