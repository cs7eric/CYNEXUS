package cn.cccs7.cynexus.market.application.service;

import cn.cccs7.cynexus.market.domain.entity.SectorRankBO;
import cn.cccs7.cynexus.market.domain.service.sector.SectorRankQueryDomainService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SectorRankAppService {

    private final SectorRankQueryDomainService domainService;

    public SectorRankAppService(SectorRankQueryDomainService domainService) {
        this.domainService = domainService;
    }

    public List<SectorRankBO> getSectorRank(String tradeDate, Integer topN) {
        return domainService.getSectorRank(tradeDate, topN);
    }
}
