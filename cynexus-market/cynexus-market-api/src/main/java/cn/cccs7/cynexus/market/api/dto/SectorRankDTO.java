package cn.cccs7.cynexus.market.api.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SectorRankDTO {

    private String tradeDate;
    private Integer topN;
    private List<SectorRankItemDTO> items;

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
        return SectorRankDTO.builder()
                .tradeDate(tradeDate)
                .topN(size)
                .items(items.stream().limit(size).toList())
                .build();
    }
}
