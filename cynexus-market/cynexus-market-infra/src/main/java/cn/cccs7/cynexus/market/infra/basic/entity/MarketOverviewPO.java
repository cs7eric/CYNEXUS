package cn.cccs7.cynexus.market.infra.basic.entity;

import cn.cccs7.cynexus.common.db.entity.BasePO;
import java.math.BigDecimal;

public class MarketOverviewPO extends BasePO {

    private String tradeDate;
    private BigDecimal indexChangePercent;
    private BigDecimal turnoverAmount;
    private BigDecimal northboundNetInflow;
    private Integer riseCount;
    private Integer fallCount;
    private String summary;

    public String getTradeDate() {
        return tradeDate;
    }

    public void setTradeDate(String tradeDate) {
        this.tradeDate = tradeDate;
    }

    public BigDecimal getIndexChangePercent() {
        return indexChangePercent;
    }

    public void setIndexChangePercent(BigDecimal indexChangePercent) {
        this.indexChangePercent = indexChangePercent;
    }

    public BigDecimal getTurnoverAmount() {
        return turnoverAmount;
    }

    public void setTurnoverAmount(BigDecimal turnoverAmount) {
        this.turnoverAmount = turnoverAmount;
    }

    public BigDecimal getNorthboundNetInflow() {
        return northboundNetInflow;
    }

    public void setNorthboundNetInflow(BigDecimal northboundNetInflow) {
        this.northboundNetInflow = northboundNetInflow;
    }

    public Integer getRiseCount() {
        return riseCount;
    }

    public void setRiseCount(Integer riseCount) {
        this.riseCount = riseCount;
    }

    public Integer getFallCount() {
        return fallCount;
    }

    public void setFallCount(Integer fallCount) {
        this.fallCount = fallCount;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }
}
