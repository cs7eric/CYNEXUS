package cn.cccs7.cynexus.market.domain.valueobject;


import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SectorStrength {

    private BigDecimal changePercent;
    private BigDecimal turnoverAmount;
    private BigDecimal mainNetInflow;
}
