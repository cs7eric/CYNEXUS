package cn.cccs7.cynexus.market.infra.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.market.domain.entity.ProfitEffectDailyBO;
import cn.cccs7.cynexus.market.infra.basic.entity.ProfitEffectDailyPO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface ProfitEffectDailyPOConverter extends BaseMapper<ProfitEffectDailyPO, ProfitEffectDailyBO> {

    ProfitEffectDailyPOConverter INSTANCE = Mappers.getMapper(ProfitEffectDailyPOConverter.class);
}
