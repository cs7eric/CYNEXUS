package cn.cccs7.cynexus.market.infra.basic.entity;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class FuturesPositionInstrumentPO {
    private String exchangeCode;
    private String instrumentCode;
    private String varietyCode;
    private String contractCode;
    private BigDecimal volume;
    private BigDecimal longPosition;
    private BigDecimal shortPosition;
    private BigDecimal netPosition;
    private BigDecimal longShortRatio;
    private BigDecimal longChange;
    private BigDecimal shortChange;
    private String source;
}
