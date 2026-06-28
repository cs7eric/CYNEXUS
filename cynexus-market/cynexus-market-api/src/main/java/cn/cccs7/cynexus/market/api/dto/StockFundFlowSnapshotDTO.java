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
public class StockFundFlowSnapshotDTO {

    private Long id;
    private Long batchId;
    private LocalDate tradeDate;
    private String code;
    private String symbol;
    private String name;
    private BigDecimal lastPrice;
    private BigDecimal preClose;
    private BigDecimal open;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal change;
    private BigDecimal changePct;
    private BigDecimal volumeLots;
    private BigDecimal amountWan;
    private BigDecimal turnoverRate;
    private BigDecimal outerDiscLots;
    private BigDecimal insideDishLots;
    private BigDecimal activeNetLots;
    private BigDecimal activeNetAmountWan;
    private BigDecimal activeBuyRatio;
    private BigDecimal activeSellRatio;
    private BigDecimal buyBigRatio;
    private BigDecimal buySmallRatio;
    private BigDecimal sellBigRatio;
    private BigDecimal sellSmallRatio;
    private BigDecimal pe;
    private BigDecimal pb;
    private BigDecimal amplitude;
    private BigDecimal floatMarketCapYi;
    private BigDecimal totalMarketCapYi;
    private String source;
    private String sourceType;
    private OffsetDateTime capturedAt;
    private String extra;
    private String remark;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime deletedAt;
}
