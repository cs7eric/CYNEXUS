package cn.cccs7.cynexus.market.infra.portfolio.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Data;

@Data
public class PortfolioAccountPO {
    private String principalKey;
    private String tenantKey;
    private BigDecimal availableCash;
    private OffsetDateTime updatedAt;
}
