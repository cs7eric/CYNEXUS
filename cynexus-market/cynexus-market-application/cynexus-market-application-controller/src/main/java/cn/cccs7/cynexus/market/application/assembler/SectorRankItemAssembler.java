package cn.cccs7.cynexus.market.application.assembler;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.market.api.dto.SectorRankItemDTO;
import cn.cccs7.cynexus.market.domain.entity.SectorRankBO;
import cn.cccs7.cynexus.market.domain.valueobject.Sector;
import cn.cccs7.cynexus.market.domain.valueobject.SectorStrength;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface SectorRankItemAssembler extends BaseMapper<SectorRankBO, SectorRankItemDTO> {

    @Override
    @Mapping(target = "sectorCode", source = "sector.sectorCode")
    @Mapping(target = "sectorName", source = "sector.sectorName")
    @Mapping(target = "changePercent", source = "strength.changePercent")
    @Mapping(target = "turnoverAmount", source = "strength.turnoverAmount")
    @Mapping(target = "mainNetInflow", source = "strength.mainNetInflow")
    SectorRankItemDTO toTarget(SectorRankBO source);

    @Override
    default SectorRankBO toSource(SectorRankItemDTO target) {
        if (target == null) {
            return null;
        }
        return new SectorRankBO(
                new Sector(target.getSectorCode(), target.getSectorName()),
                new SectorStrength(target.getChangePercent(), target.getTurnoverAmount(), target.getMainNetInflow())
        );
    }
}
