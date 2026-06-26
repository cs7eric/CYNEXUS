package cn.cccs7.cynexus.market.api.dto;

import java.math.BigDecimal;

public record SectorRankItemDTO(
        String sectorCode,
        String sectorName,
        BigDecimal changePercent,
        BigDecimal turnoverAmount,
        BigDecimal mainNetInflow
) {
}
