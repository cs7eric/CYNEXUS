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
public class TdxIndustryQuoteSnapshotBO {

    private Long id;
    private Long batchId;
    private LocalDate snapshotDate;
    private String code6;
    private String name;
    private String fullCode;
    private String exchange;
    private BigDecimal lastPrice;
    private BigDecimal preClosePrice;
    private BigDecimal openPrice;
    private BigDecimal highPrice;
    private BigDecimal lowPrice;
    private BigDecimal change;
    private BigDecimal changePct;
    private BigDecimal amount;
    private BigDecimal totalHand;
    private BigDecimal currentHand;
    private String source;
    private OffsetDateTime fetchedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime deletedAt;
}
