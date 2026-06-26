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
public class MarketMoneyFlow {

    private BigDecimal mainNetInflow;
    private BigDecimal northboundNetInflow;
}
