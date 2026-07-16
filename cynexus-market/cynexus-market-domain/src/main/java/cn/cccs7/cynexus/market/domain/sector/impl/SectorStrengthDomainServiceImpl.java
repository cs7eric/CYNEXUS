package cn.cccs7.cynexus.market.domain.sector.impl;

import cn.cccs7.cynexus.market.domain.entity.SectorRankBO;
import cn.cccs7.cynexus.market.domain.sector.SectorStrengthDomainService;

public class SectorStrengthDomainServiceImpl implements SectorStrengthDomainService {

    @Override
    public boolean isLeadingSector(SectorRankBO rank) {
        return rank != null
                && rank.getStrength() != null
                && rank.getStrength().getChangePercent() != null
                && rank.getStrength().getMainNetInflow() != null
                && rank.getStrength().getChangePercent().doubleValue() > 2.0
                && rank.getStrength().getMainNetInflow().doubleValue() > 0;
    }
}
