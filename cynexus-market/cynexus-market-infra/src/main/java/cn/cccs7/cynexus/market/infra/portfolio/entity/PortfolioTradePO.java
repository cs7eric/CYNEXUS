package cn.cccs7.cynexus.market.infra.portfolio.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Data;

@Data
public class PortfolioTradePO {
    private Long id;
    private String principalKey;
    private String tenantKey;
    private Long sourceTaskId;
    private Long sourceCandidateId;
    private String symbol;
    private String securityName;
    private String instrumentType;
    private String side;
    private OffsetDateTime tradedAt;
    private BigDecimal quantity;
    private BigDecimal price;
    private BigDecimal grossAmount;
    private BigDecimal totalFee;
    private String status;
    private OffsetDateTime confirmedAt;
    private String confirmedBy;
}
