package cn.cccs7.cynexus.market.infra.basic.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Date;
import java.io.Serializable;

/**
 * (MsiIndexDaily)实体类
 *
 * @author cccs7 - csq020611@gmail.com
 * @since 2026-06-27 15:03:09
 */
public class MsiIndexDaily implements Serializable {
    private static final long serialVersionUID = 136120776257254960L;

    private Long id;

    private Date tradeDate;

    private BigDecimal volScore;

    private BigDecimal turnoverScore;

    private BigDecimal priceStrengthScore;

    private BigDecimal riskAppetiteScore;

    private BigDecimal breadthScore;

    private BigDecimal limitEmotionScore;

    private BigDecimal profitEffectScore;

    private BigDecimal sectorBreadthScore;

    private BigDecimal styleRiskScore;

    private BigDecimal compositeScore;

    private Integer componentCount;

    private String level;

    private Integer elapsedMs;

    private String source;

    private OffsetDateTime ingestedAt;

    private String extra;

    private String remark;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;

    private OffsetDateTime deletedAt;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Date getTradeDate() {
        return tradeDate;
    }

    public void setTradeDate(Date tradeDate) {
        this.tradeDate = tradeDate;
    }

    public BigDecimal getVolScore() {
        return volScore;
    }

    public void setVolScore(BigDecimal volScore) {
        this.volScore = volScore;
    }

    public BigDecimal getTurnoverScore() {
        return turnoverScore;
    }

    public void setTurnoverScore(BigDecimal turnoverScore) {
        this.turnoverScore = turnoverScore;
    }

    public BigDecimal getPriceStrengthScore() {
        return priceStrengthScore;
    }

    public void setPriceStrengthScore(BigDecimal priceStrengthScore) {
        this.priceStrengthScore = priceStrengthScore;
    }

    public BigDecimal getRiskAppetiteScore() {
        return riskAppetiteScore;
    }

    public void setRiskAppetiteScore(BigDecimal riskAppetiteScore) {
        this.riskAppetiteScore = riskAppetiteScore;
    }

    public BigDecimal getBreadthScore() {
        return breadthScore;
    }

    public void setBreadthScore(BigDecimal breadthScore) {
        this.breadthScore = breadthScore;
    }

    public BigDecimal getLimitEmotionScore() {
        return limitEmotionScore;
    }

    public void setLimitEmotionScore(BigDecimal limitEmotionScore) {
        this.limitEmotionScore = limitEmotionScore;
    }

    public BigDecimal getProfitEffectScore() {
        return profitEffectScore;
    }

    public void setProfitEffectScore(BigDecimal profitEffectScore) {
        this.profitEffectScore = profitEffectScore;
    }

    public BigDecimal getSectorBreadthScore() {
        return sectorBreadthScore;
    }

    public void setSectorBreadthScore(BigDecimal sectorBreadthScore) {
        this.sectorBreadthScore = sectorBreadthScore;
    }

    public BigDecimal getStyleRiskScore() {
        return styleRiskScore;
    }

    public void setStyleRiskScore(BigDecimal styleRiskScore) {
        this.styleRiskScore = styleRiskScore;
    }

    public BigDecimal getCompositeScore() {
        return compositeScore;
    }

    public void setCompositeScore(BigDecimal compositeScore) {
        this.compositeScore = compositeScore;
    }

    public Integer getComponentCount() {
        return componentCount;
    }

    public void setComponentCount(Integer componentCount) {
        this.componentCount = componentCount;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public Integer getElapsedMs() {
        return elapsedMs;
    }

    public void setElapsedMs(Integer elapsedMs) {
        this.elapsedMs = elapsedMs;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public OffsetDateTime getIngestedAt() {
        return ingestedAt;
    }

    public void setIngestedAt(OffsetDateTime ingestedAt) {
        this.ingestedAt = ingestedAt;
    }

    public String getExtra() {
        return extra;
    }

    public void setExtra(String extra) {
        this.extra = extra;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public OffsetDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(OffsetDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

}

