package cn.cccs7.cynexus.market.domain.entity;

import cn.cccs7.cynexus.market.domain.valueobject.Sector;
import cn.cccs7.cynexus.market.domain.valueobject.SectorStrength;

public record SectorRankBO(Sector sector, SectorStrength strength) {
}
