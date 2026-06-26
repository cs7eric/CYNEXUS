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
public class SectorFundFlowDailySnapshotBO {

    private Long id;
    private Long batchId;
    private String scope;
    private LocalDate tradeDate;
    private String sectorCode;
    private String sectorName;
    private Integer rank;
    private BigDecimal changePct;
    private BigDecimal inflow;
    private BigDecimal outflow;
    private BigDecimal net;
    private Integer companyCount;
    private String leaderStock;
    private BigDecimal leaderChange;
    private BigDecimal leaderPrice;
    private String source;
    private String sourceType;
    private OffsetDateTime capturedAt;
    private String extra;
    private String remark;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime deletedAt;
}
