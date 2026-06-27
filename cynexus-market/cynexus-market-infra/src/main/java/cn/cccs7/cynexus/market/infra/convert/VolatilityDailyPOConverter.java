package cn.cccs7.cynexus.market.infra.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.market.domain.entity.VolatilityDailyBO;
import cn.cccs7.cynexus.market.infra.basic.entity.VolatilityDailyPO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface VolatilityDailyPOConverter extends BaseMapper<VolatilityDailyPO, VolatilityDailyBO> {

    VolatilityDailyPOConverter INSTANCE = Mappers.getMapper(VolatilityDailyPOConverter.class);
}
