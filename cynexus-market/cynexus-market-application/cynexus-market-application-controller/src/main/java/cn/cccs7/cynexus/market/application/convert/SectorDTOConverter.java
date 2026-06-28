package cn.cccs7.cynexus.market.application.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.market.api.dto.SectorDTO;
import cn.cccs7.cynexus.market.domain.entity.SectorBO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface SectorDTOConverter extends BaseMapper<SectorDTO, SectorBO> {

    SectorDTOConverter INSTANCE = Mappers.getMapper(SectorDTOConverter.class);
}
