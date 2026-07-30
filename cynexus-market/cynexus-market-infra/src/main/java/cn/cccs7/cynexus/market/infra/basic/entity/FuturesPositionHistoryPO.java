package cn.cccs7.cynexus.market.infra.basic.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

@Data
public class FuturesPositionHistoryPO {
    private LocalDate tradeDate;
    private String instrumentCode;
    private String varietyCode;
    private BigDecimal volume;
    private BigDecimal longPosition;
    private BigDecimal shortPosition;
    private BigDecimal netPosition;
    private BigDecimal longShortRatio;
    private BigDecimal longChange;
    private BigDecimal shortChange;
    private String source;
}
