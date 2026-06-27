package cn.cccs7.cynexus.market.application.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.market.api.dto.VolatilityDailyDTO;
import cn.cccs7.cynexus.market.domain.entity.VolatilityDailyBO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface VolatilityDailyDTOConverter extends BaseMapper<VolatilityDailyDTO, VolatilityDailyBO> {

    VolatilityDailyDTOConverter INSTANCE = Mappers.getMapper(VolatilityDailyDTOConverter.class);
}
