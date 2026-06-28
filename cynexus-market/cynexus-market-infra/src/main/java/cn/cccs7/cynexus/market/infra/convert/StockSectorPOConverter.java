package cn.cccs7.cynexus.market.infra.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.market.domain.entity.StockSectorBO;
import cn.cccs7.cynexus.market.infra.basic.entity.StockSectorPO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface StockSectorPOConverter extends BaseMapper<StockSectorPO, StockSectorBO> {

    StockSectorPOConverter INSTANCE = Mappers.getMapper(StockSectorPOConverter.class);
}
