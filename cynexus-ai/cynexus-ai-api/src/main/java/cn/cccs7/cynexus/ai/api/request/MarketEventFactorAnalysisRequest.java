package cn.cccs7.cynexus.ai.api.request;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketEventFactorAnalysisRequest {
    private String tradeDate;
    private String cohort;
    private BigDecimal minLargeMovePct;
    private Integer maxEvents;
    private String symbol;
}
