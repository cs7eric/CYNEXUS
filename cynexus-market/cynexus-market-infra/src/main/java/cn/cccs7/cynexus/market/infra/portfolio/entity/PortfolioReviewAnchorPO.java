package cn.cccs7.cynexus.market.infra.portfolio.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Data;

@Data
public class PortfolioReviewAnchorPO {
    private Long id;
    private String principalKey;
    private String tenantKey;
    private String symbol;
    private String instrumentType;
    private Long tradeId;
    private Long reviewId;
    private String anchorType;
    private String side;
    private OffsetDateTime occurredAt;
    private BigDecimal price;
    private String note;
    private String tags;
}
