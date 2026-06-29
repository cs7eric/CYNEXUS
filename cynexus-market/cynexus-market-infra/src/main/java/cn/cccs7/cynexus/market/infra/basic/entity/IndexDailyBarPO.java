package cn.cccs7.cynexus.market.infra.basic.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

@Data
public class IndexDailyBarPO {

    private LocalDate tradeDate;
    private String code;
    private String name;
    private BigDecimal close;
    private BigDecimal amount;
}
