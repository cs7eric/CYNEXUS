package cn.cccs7.cynexus.market.infra.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.market.domain.entity.TurnoverActivityDailyBO;
import cn.cccs7.cynexus.market.infra.basic.entity.TurnoverActivityDailyPO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface TurnoverActivityDailyPOConverter extends BaseMapper<TurnoverActivityDailyPO, TurnoverActivityDailyBO> {

    TurnoverActivityDailyPOConverter INSTANCE = Mappers.getMapper(TurnoverActivityDailyPOConverter.class);
}
