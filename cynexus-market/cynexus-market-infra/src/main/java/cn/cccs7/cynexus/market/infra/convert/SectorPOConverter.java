package cn.cccs7.cynexus.market.infra.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.market.domain.entity.SectorBO;
import cn.cccs7.cynexus.market.infra.basic.entity.SectorPO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface SectorPOConverter extends BaseMapper<SectorPO, SectorBO> {

    SectorPOConverter INSTANCE = Mappers.getMapper(SectorPOConverter.class);
}
