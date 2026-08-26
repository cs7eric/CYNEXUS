package cn.cccs7.cynexus.market.infra.factorresearch.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.Data;

@Data
public class MarketFactorStudySnapshotPO {
    private String code;
    private String market;
    private LocalDate featureTradeDate;
    private OffsetDateTime availableAt;
    private String industry;
    private Boolean st;
    private BigDecimal floatMarketCap;
    private BigDecimal amount;
    private String source;
    private String coverage;
    private String marketRegime;
    private String industryRegime;
    private java.math.BigDecimal close;
    private java.math.BigDecimal volume;
    private java.math.BigDecimal turnoverPct;
    private java.math.BigDecimal return1d;
    private java.math.BigDecimal return3d;
    private java.math.BigDecimal return5d;
    private java.math.BigDecimal return10d;
    private java.math.BigDecimal return20d;
    private java.math.BigDecimal return60d;
    private java.math.BigDecimal return120d;
    private java.math.BigDecimal return252d;
    private java.math.BigDecimal ma5;
    private java.math.BigDecimal ma10;
    private java.math.BigDecimal ma20;
    private java.math.BigDecimal ma60;
    private java.math.BigDecimal closeVsMa5;
    private java.math.BigDecimal closeVsMa10;
    private java.math.BigDecimal closeVsMa20;
    private java.math.BigDecimal closeVsMa60;
    private java.math.BigDecimal volumeVsMa5;
    private java.math.BigDecimal volumeVsMa10;
    private java.math.BigDecimal volumeVsMa20;
    private java.math.BigDecimal volumeVsMa60;
    private java.math.BigDecimal benchmarkClose;
}
