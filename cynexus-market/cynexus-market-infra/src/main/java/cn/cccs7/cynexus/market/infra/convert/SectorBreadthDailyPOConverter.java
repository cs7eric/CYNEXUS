package cn.cccs7.cynexus.market.infra.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.market.domain.entity.SectorBreadthDailyBO;
import cn.cccs7.cynexus.market.infra.basic.entity.SectorBreadthDailyPO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface SectorBreadthDailyPOConverter extends BaseMapper<SectorBreadthDailyPO, SectorBreadthDailyBO> {

    SectorBreadthDailyPOConverter INSTANCE = Mappers.getMapper(SectorBreadthDailyPOConverter.class);
}
