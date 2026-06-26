package cn.cccs7.cynexus.market.application.assembler;

import cn.cccs7.cynexus.market.api.dto.SectorRankDTO;
import cn.cccs7.cynexus.market.domain.entity.SectorRankBO;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class SectorRankAssembler {

    private final SectorRankItemAssembler sectorRankItemAssembler;

    public SectorRankAssembler(SectorRankItemAssembler sectorRankItemAssembler) {
        this.sectorRankItemAssembler = sectorRankItemAssembler;
    }

    public SectorRankDTO toDTO(String tradeDate, Integer topN, List<SectorRankBO> ranks) {
        return new SectorRankDTO(tradeDate, topN, sectorRankItemAssembler.toTargetList(ranks));
    }
}
