package cn.cccs7.cynexus.market.infra.factorresearch.entity;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.Data;

@Data
public class MarketFactorStudySamplePO {
    private String sampleType;
    private String stockCode;
    private String market;
    private LocalDate eventTradeDate;
    private OffsetDateTime anchorAt;
    private String eventType;
    private String matchingMetadata;
    private String source;
    private String coverage;
}
