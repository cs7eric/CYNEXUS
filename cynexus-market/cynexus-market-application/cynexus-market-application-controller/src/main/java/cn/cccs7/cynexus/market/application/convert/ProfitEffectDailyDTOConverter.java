package cn.cccs7.cynexus.market.application.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.market.api.dto.ProfitEffectDailyDTO;
import cn.cccs7.cynexus.market.domain.entity.ProfitEffectDailyBO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface ProfitEffectDailyDTOConverter extends BaseMapper<ProfitEffectDailyDTO, ProfitEffectDailyBO> {

    ProfitEffectDailyDTOConverter INSTANCE = Mappers.getMapper(ProfitEffectDailyDTOConverter.class);
}
