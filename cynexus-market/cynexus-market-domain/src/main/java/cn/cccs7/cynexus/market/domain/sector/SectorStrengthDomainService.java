package cn.cccs7.cynexus.market.domain.sector;

import cn.cccs7.cynexus.market.domain.entity.SectorRankBO;

public interface SectorStrengthDomainService {

    boolean isLeadingSector(SectorRankBO rank);
}
