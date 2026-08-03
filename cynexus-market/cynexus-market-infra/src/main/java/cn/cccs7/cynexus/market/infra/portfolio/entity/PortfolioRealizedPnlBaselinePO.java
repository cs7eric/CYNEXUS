package cn.cccs7.cynexus.market.infra.portfolio.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Data;

@Data
public class PortfolioRealizedPnlBaselinePO {
    private String symbol;
    private BigDecimal realizedPnl;
    private OffsetDateTime resetAt;
}
