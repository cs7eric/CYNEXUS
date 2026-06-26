package cn.cccs7.cynexus.market.infra.basic.entity;

import cn.cccs7.cynexus.common.db.entity.BasePO;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class MarketOverviewPO extends BasePO {

    private String tradeDate;
    private BigDecimal indexChangePercent;
    private BigDecimal turnoverAmount;
    private BigDecimal northboundNetInflow;
    private Integer riseCount;
    private Integer fallCount;
    private String summary;
}
