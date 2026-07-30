package cn.cccs7.cynexus.market.infra.basic.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

@Data
public class FuturesPositionMemberPO {
    private LocalDate tradeDate;
    private String exchangeCode;
    private String scopeType;
    private String instrumentCode;
    private String varietyCode;
    private String contractCode;
    private String rankType;
    private Integer rankNo;
    private String memberName;
    private BigDecimal rankValue;
    private BigDecimal rankChange;
    private String source;
}
