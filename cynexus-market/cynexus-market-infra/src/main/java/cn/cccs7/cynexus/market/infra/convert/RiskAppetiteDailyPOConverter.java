package cn.cccs7.cynexus.market.infra.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.market.domain.entity.RiskAppetiteDailyBO;
import cn.cccs7.cynexus.market.infra.basic.entity.RiskAppetiteDailyPO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface RiskAppetiteDailyPOConverter extends BaseMapper<RiskAppetiteDailyPO, RiskAppetiteDailyBO> {

    RiskAppetiteDailyPOConverter INSTANCE = Mappers.getMapper(RiskAppetiteDailyPOConverter.class);
}
