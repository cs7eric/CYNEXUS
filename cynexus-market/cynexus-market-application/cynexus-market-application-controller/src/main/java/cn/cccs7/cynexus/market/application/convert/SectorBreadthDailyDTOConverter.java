package cn.cccs7.cynexus.market.application.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.market.api.dto.SectorBreadthDailyDTO;
import cn.cccs7.cynexus.market.domain.entity.SectorBreadthDailyBO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface SectorBreadthDailyDTOConverter extends BaseMapper<SectorBreadthDailyDTO, SectorBreadthDailyBO> {

    SectorBreadthDailyDTOConverter INSTANCE = Mappers.getMapper(SectorBreadthDailyDTOConverter.class);
}
