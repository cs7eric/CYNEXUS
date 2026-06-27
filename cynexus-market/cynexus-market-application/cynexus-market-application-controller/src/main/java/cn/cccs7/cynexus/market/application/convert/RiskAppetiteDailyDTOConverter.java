package cn.cccs7.cynexus.market.application.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.market.api.dto.RiskAppetiteDailyDTO;
import cn.cccs7.cynexus.market.domain.entity.RiskAppetiteDailyBO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface RiskAppetiteDailyDTOConverter extends BaseMapper<RiskAppetiteDailyDTO, RiskAppetiteDailyBO> {

    RiskAppetiteDailyDTOConverter INSTANCE = Mappers.getMapper(RiskAppetiteDailyDTOConverter.class);
}
