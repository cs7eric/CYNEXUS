package cn.cccs7.cynexus.market.domain.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketIndicatorSnapshotBO {
    private Long id;
    private String code;
    private String name;
    private String industry;
    private String market;
    private BigDecimal lastPrice;
    private BigDecimal prevClose;
    private BigDecimal openPrice;
    private BigDecimal highPrice;
    private BigDecimal lowPrice;
    private BigDecimal changePct;
    private BigDecimal amount;
    private BigDecimal volume;
    private BigDecimal turnoverRate;
    private BigDecimal limitUpPrice;
    private BigDecimal limitDownPrice;
    private String limitStatus;
    private String source;
    private OffsetDateTime capturedAt;
}
