package cn.cccs7.cynexus.market.infra.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.market.domain.entity.MarginStockDetailBO;
import cn.cccs7.cynexus.market.infra.basic.entity.MarginStockDetailPO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface MarginStockDetailPOConverter extends BaseMapper<MarginStockDetailPO, MarginStockDetailBO> {

    MarginStockDetailPOConverter INSTANCE = Mappers.getMapper(MarginStockDetailPOConverter.class);
}
