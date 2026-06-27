package cn.cccs7.cynexus.market.infra.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.market.domain.entity.StyleRiskDailyBO;
import cn.cccs7.cynexus.market.infra.basic.entity.StyleRiskDailyPO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface StyleRiskDailyPOConverter extends BaseMapper<StyleRiskDailyPO, StyleRiskDailyBO> {

    StyleRiskDailyPOConverter INSTANCE = Mappers.getMapper(StyleRiskDailyPOConverter.class);
}
