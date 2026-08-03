package cn.cccs7.cynexus.market.api.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PortfolioAccountDTO {
    BigDecimal availableCash;
    OffsetDateTime updatedAt;
}
