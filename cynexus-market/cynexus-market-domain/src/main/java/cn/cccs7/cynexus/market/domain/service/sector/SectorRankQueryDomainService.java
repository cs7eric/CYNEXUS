package cn.cccs7.cynexus.market.domain.service.sector;

import cn.cccs7.cynexus.market.domain.entity.SectorRankBO;
import java.util.List;

public interface SectorRankQueryDomainService {

    List<SectorRankBO> getSectorRank(String tradeDate, Integer topN);
}
