package cn.cccs7.cynexus.market.infra.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.market.domain.entity.MarginAccountStatsBO;
import cn.cccs7.cynexus.market.infra.basic.entity.MarginAccountStatsPO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface MarginAccountStatsPOConverter extends BaseMapper<MarginAccountStatsPO, MarginAccountStatsBO> {

    MarginAccountStatsPOConverter INSTANCE = Mappers.getMapper(MarginAccountStatsPOConverter.class);
}
