package cn.cccs7.cynexus.market.api.dto;

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
public class MarketLimitDailyStockDTO {

    private Long id;
    private LocalDate tradeDate;
    private String code;
    private String name;
    private String category;
    private Integer streak;
    private BigDecimal changePct;
    private BigDecimal limitUpPrice;
    private BigDecimal limitDownPrice;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime deletedAt;
}
