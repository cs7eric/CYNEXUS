package cn.cccs7.cynexus.market.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketIndicatorEventBO {
    private Long id;
    private LocalDate tradeDate;
    private String code;
    private String name;
    private String market;
    private String eventType;
    private OffsetDateTime eventAt;
    private BigDecimal signalValue;
    private BigDecimal lastPrice;
    private BigDecimal changePct;
    private String limitStatus;
    private String source;
}
