package cn.cccs7.cynexus.market.api.dto;


import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SectorRankItemDTO {

    private String sectorCode;
    private String sectorName;
    private BigDecimal changePercent;
    private BigDecimal turnoverAmount;
    private BigDecimal mainNetInflow;
}
