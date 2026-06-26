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
public class MarketPulseSectorDailySnapshotBO {

    private Long id;
    private Long batchId;
    private LocalDate tradeDate;
    private String sectorName;
    private String sectorIndex;
    private Integer rankByChange;
    private BigDecimal changePct;
    private BigDecimal inflow;
    private BigDecimal outflow;
    private BigDecimal mainNet;
    private Integer stockCount;
    private String leadingStock;
    private BigDecimal leadingChangePct;
    private BigDecimal leadingPrice;
    private String sourceKind;
    private String sourceName;
    private OffsetDateTime capturedAt;
    private String extra;
    private String remark;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime deletedAt;
}
