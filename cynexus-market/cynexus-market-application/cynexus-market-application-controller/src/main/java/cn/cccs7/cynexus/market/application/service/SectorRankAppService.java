package cn.cccs7.cynexus.market.application.service;

import cn.cccs7.cynexus.market.domain.entity.SectorRankBO;
import cn.cccs7.cynexus.market.domain.repository.SectorRankRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SectorRankAppService {

    private final SectorRankRepository sectorRankRepository;

    public SectorRankAppService(SectorRankRepository sectorRankRepository) {
        this.sectorRankRepository = sectorRankRepository;
    }

    public List<SectorRankBO> getSectorRank(String tradeDate, Integer topN) {
        return sectorRankRepository.findByTradeDate(tradeDate, topN == null ? 10 : topN);
    }
}
