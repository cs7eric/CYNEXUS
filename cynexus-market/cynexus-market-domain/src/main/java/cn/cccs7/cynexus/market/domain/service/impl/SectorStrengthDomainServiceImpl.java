package cn.cccs7.cynexus.market.domain.service.impl;

import cn.cccs7.cynexus.market.domain.entity.SectorRankBO;
import cn.cccs7.cynexus.market.domain.service.SectorStrengthDomainService;

public class SectorStrengthDomainServiceImpl implements SectorStrengthDomainService {

    @Override
    public boolean isLeadingSector(SectorRankBO rank) {
        return rank.strength().changePercent().doubleValue() > 2.0
                && rank.strength().mainNetInflow().doubleValue() > 0;
    }
}
