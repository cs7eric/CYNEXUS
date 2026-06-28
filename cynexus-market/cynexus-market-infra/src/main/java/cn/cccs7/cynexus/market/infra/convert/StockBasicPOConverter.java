package cn.cccs7.cynexus.market.infra.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.market.domain.entity.StockBasicBO;
import cn.cccs7.cynexus.market.infra.basic.entity.StockBasicPO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface StockBasicPOConverter extends BaseMapper<StockBasicPO, StockBasicBO> {

    StockBasicPOConverter INSTANCE = Mappers.getMapper(StockBasicPOConverter.class);
}
