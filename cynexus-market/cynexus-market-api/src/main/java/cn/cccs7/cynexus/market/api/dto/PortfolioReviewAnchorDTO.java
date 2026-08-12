package cn.cccs7.cynexus.market.api.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Data;

@Data
public class PortfolioReviewAnchorDTO {
    private Long id;
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
