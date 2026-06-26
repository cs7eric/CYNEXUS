package cn.cccs7.cynexus.market.api.dto;

import java.math.BigDecimal;
import java.util.List;

public record SectorRankDTO(String tradeDate, Integer topN, List<SectorRankItemDTO> items) {

    public static SectorRankDTO mock(String tradeDate, Integer topN) {
        int size = topN == null ? 10 : topN;
        List<SectorRankItemDTO> items = List.of(
                new SectorRankItemDTO("AI", "Artificial Intelligence", new BigDecimal("3.18"),
                        new BigDecimal("1280.50"), new BigDecimal("86.30")),
                new SectorRankItemDTO("SEMICONDUCTOR", "Semiconductor", new BigDecimal("2.42"),
                        new BigDecimal("980.10"), new BigDecimal("54.20")),
                new SectorRankItemDTO("BROKERAGE", "Brokerage", new BigDecimal("1.76"),
                        new BigDecimal("760.00"), new BigDecimal("21.80"))
        );
        return new SectorRankDTO(tradeDate, size, items.stream().limit(size).toList());
    }
}
