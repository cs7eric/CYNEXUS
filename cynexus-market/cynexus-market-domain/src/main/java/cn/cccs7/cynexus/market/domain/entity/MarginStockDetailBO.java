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
public class MarginStockDetailBO {

    private Long id;
    private LocalDate tradeDate;
    private String exchange;
    private String stockCode;
    private String stockName;
    private String source;
    private BigDecimal financingBalance;
    private BigDecimal financingBuyAmount;
    private BigDecimal financingRepayAmount;
    private BigDecimal securitiesLendingBalance;
    private BigDecimal securitiesLendingBalanceAmount;
    private BigDecimal securitiesLendingSellVolume;
    private BigDecimal securitiesLendingRepayVolume;
    private BigDecimal marginBalance;
    private String extra;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime deletedAt;
}
