package cn.cccs7.cynexus.market.infra.basic.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Date;
import java.io.Serializable;

/**
 * (MsiLimitEmotionDaily)实体类
 *
 * @author cccs7 - csq020611@gmail.com
 * @since 2026-06-27 15:05:21
 */
public class MsiLimitEmotionDaily implements Serializable {
    private static final long serialVersionUID = 341225801578184413L;

    private Long id;

    private Date tradeDate;

    private Integer limitUpCount;

    private Integer limitDownCount;

    private Integer touchedCount;

    private Integer brokenCount;

    private BigDecimal breakBoardRate;

    private BigDecimal limitUpDownRatio;

    private Integer yesterdayLimitUpCount;

    private BigDecimal yesterdayLimitUpAvgReturn;

    private BigDecimal upDownScore;

    private BigDecimal breakBoardScore;

    private BigDecimal yesterdayReturnScore;

    private BigDecimal compositeScore;

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

    public Integer getLimitUpCount() {
        return limitUpCount;
    }

    public void setLimitUpCount(Integer limitUpCount) {
        this.limitUpCount = limitUpCount;
    }

    public Integer getLimitDownCount() {
        return limitDownCount;
    }

    public void setLimitDownCount(Integer limitDownCount) {
        this.limitDownCount = limitDownCount;
    }

    public Integer getTouchedCount() {
        return touchedCount;
    }

    public void setTouchedCount(Integer touchedCount) {
        this.touchedCount = touchedCount;
    }

    public Integer getBrokenCount() {
        return brokenCount;
    }

    public void setBrokenCount(Integer brokenCount) {
        this.brokenCount = brokenCount;
    }

    public BigDecimal getBreakBoardRate() {
        return breakBoardRate;
    }

    public void setBreakBoardRate(BigDecimal breakBoardRate) {
        this.breakBoardRate = breakBoardRate;
    }

    public BigDecimal getLimitUpDownRatio() {
        return limitUpDownRatio;
    }

    public void setLimitUpDownRatio(BigDecimal limitUpDownRatio) {
        this.limitUpDownRatio = limitUpDownRatio;
    }

    public Integer getYesterdayLimitUpCount() {
        return yesterdayLimitUpCount;
    }

    public void setYesterdayLimitUpCount(Integer yesterdayLimitUpCount) {
        this.yesterdayLimitUpCount = yesterdayLimitUpCount;
    }

    public BigDecimal getYesterdayLimitUpAvgReturn() {
        return yesterdayLimitUpAvgReturn;
    }

    public void setYesterdayLimitUpAvgReturn(BigDecimal yesterdayLimitUpAvgReturn) {
        this.yesterdayLimitUpAvgReturn = yesterdayLimitUpAvgReturn;
    }

    public BigDecimal getUpDownScore() {
        return upDownScore;
    }

    public void setUpDownScore(BigDecimal upDownScore) {
        this.upDownScore = upDownScore;
    }

    public BigDecimal getBreakBoardScore() {
        return breakBoardScore;
    }

    public void setBreakBoardScore(BigDecimal breakBoardScore) {
        this.breakBoardScore = breakBoardScore;
    }

    public BigDecimal getYesterdayReturnScore() {
        return yesterdayReturnScore;
    }

    public void setYesterdayReturnScore(BigDecimal yesterdayReturnScore) {
        this.yesterdayReturnScore = yesterdayReturnScore;
    }

    public BigDecimal getCompositeScore() {
        return compositeScore;
    }

    public void setCompositeScore(BigDecimal compositeScore) {
        this.compositeScore = compositeScore;
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

