package cn.cccs7.cynexus.market.api.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PortfolioTradeAnalyticsWarningDTO {
    String code;
    String symbol;
    BigDecimal quantity;
    String message;
}
