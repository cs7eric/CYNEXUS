package cn.cccs7.cynexus.market.api.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PortfolioTradeDTO {
    long id;
    String symbol;
    String securityName;
    String side;
    OffsetDateTime tradedAt;
    BigDecimal quantity;
    BigDecimal price;
    BigDecimal totalFee;
    String status;
}
