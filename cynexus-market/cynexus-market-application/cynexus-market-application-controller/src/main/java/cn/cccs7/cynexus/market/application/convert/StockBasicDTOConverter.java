package cn.cccs7.cynexus.market.application.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.market.api.dto.StockBasicDTO;
import cn.cccs7.cynexus.market.domain.entity.StockBasicBO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface StockBasicDTOConverter extends BaseMapper<StockBasicDTO, StockBasicBO> {

    StockBasicDTOConverter INSTANCE = Mappers.getMapper(StockBasicDTOConverter.class);
}
