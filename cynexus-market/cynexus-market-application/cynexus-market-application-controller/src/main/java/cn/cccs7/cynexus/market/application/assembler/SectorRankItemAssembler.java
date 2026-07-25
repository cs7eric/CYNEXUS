package cn.cccs7.cynexus.market.application.assembler;

import cn.cccs7.cynexus.market.api.dto.SectorRankItemDTO;
import cn.cccs7.cynexus.market.domain.entity.SectorRankBO;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface SectorRankItemAssembler {

    @Mapping(target = "sectorCode", source = "sector.sectorCode")
    @Mapping(target = "sectorName", source = "sector.sectorName")
    @Mapping(target = "changePercent", source = "strength.changePercent")
    @Mapping(target = "turnoverAmount", source = "strength.turnoverAmount")
    @Mapping(target = "mainNetInflow", source = "strength.mainNetInflow")
    SectorRankItemDTO toDTO(SectorRankBO source);

    List<SectorRankItemDTO> toDTOList(List<SectorRankBO> source);
}
