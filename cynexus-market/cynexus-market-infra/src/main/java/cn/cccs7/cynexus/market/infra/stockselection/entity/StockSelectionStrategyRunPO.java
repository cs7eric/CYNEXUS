package cn.cccs7.cynexus.market.infra.stockselection.entity;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.Data;

@Data
public class StockSelectionStrategyRunPO {
    private Long id;
    private String tenantId;
    private String ownerId;
    private Long strategyVersionId;
    private LocalDate requestedTradeDate;
    private LocalDate actualTradeDate;
    private String status;
    private String parameterSnapshot;
    private String dataSource;
    private String dataFreshness;
    private Integer universeCount;
    private Integer candidateCount;
    private String warnings;
    private OffsetDateTime startedAt;
    private OffsetDateTime completedAt;
    private Integer strategyVersionNo;
    private String strategyCode;
}
