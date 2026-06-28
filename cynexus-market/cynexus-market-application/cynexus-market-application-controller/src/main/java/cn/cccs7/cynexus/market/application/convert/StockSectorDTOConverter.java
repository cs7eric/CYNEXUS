package cn.cccs7.cynexus.market.application.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.market.api.dto.StockSectorDTO;
import cn.cccs7.cynexus.market.domain.entity.StockSectorBO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface StockSectorDTOConverter extends BaseMapper<StockSectorDTO, StockSectorBO> {

    StockSectorDTOConverter INSTANCE = Mappers.getMapper(StockSectorDTOConverter.class);
}
