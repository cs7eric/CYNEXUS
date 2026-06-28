package cn.cccs7.cynexus.market.application.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.market.api.dto.MarketPulseStyleSectorListDTO;
import cn.cccs7.cynexus.market.domain.entity.StyleSectorSnapshotBO;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MarketPulseStyleSectorItemDTOConverter
        extends BaseMapper<MarketPulseStyleSectorListDTO.ItemDTO, StyleSectorSnapshotBO> {

    MarketPulseStyleSectorItemDTOConverter INSTANCE = Mappers.getMapper(MarketPulseStyleSectorItemDTOConverter.class);
}
