package cn.cccs7.cynexus.market.infra.factorresearch.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.Data;

@Data
public class MarketFactorEventFactPO {
    private String code;
    private String market;
    private LocalDate tradeDate;
    private OffsetDateTime firstLimitUpAt;
    private Boolean finalLimitUp;
    private BigDecimal changePct;
}
