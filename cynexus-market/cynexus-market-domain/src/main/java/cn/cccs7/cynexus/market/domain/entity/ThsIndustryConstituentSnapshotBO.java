package cn.cccs7.cynexus.market.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThsIndustryConstituentSnapshotBO {

    private Long id;
    private Long batchId;
    private LocalDate snapshotDate;
    private String industryCode;
    private String stockCode;
    private String stockName;
    private Integer rank;
    private BigDecimal price;
    private BigDecimal changePct;
    private BigDecimal change;
    private BigDecimal speedPct;
    private BigDecimal turnoverPct;
    private BigDecimal volumeRatio;
    private BigDecimal amplitudePct;
    private String amountText;
    private String floatSharesText;
    private String floatMarketCapText;
    private BigDecimal pe;
    private String source;
    private String sourceType;
    private OffsetDateTime capturedAt;
    private String extra;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime deletedAt;
}
