package cn.cccs7.cynexus.market.infra.factorresearch.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.Data;

@Data
public class MarketFactorStudySequencePointPO {
    private String code;
    private String market;
    private LocalDate eventTradeDate;
    private String sampleType;
    private String frequency;
    private String sequenceCode;
    private int offsetValue;
    private OffsetDateTime observedAt;
    private OffsetDateTime availableAt;
    private BigDecimal rawValue;
    private BigDecimal normalizedValue;
    private String source;
}
