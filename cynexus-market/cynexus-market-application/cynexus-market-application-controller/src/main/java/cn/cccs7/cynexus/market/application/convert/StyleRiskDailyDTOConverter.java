package cn.cccs7.cynexus.market.application.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.market.api.dto.StyleRiskDailyDTO;
import cn.cccs7.cynexus.market.domain.entity.StyleRiskDailyBO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface StyleRiskDailyDTOConverter extends BaseMapper<StyleRiskDailyDTO, StyleRiskDailyBO> {

    StyleRiskDailyDTOConverter INSTANCE = Mappers.getMapper(StyleRiskDailyDTOConverter.class);
}
