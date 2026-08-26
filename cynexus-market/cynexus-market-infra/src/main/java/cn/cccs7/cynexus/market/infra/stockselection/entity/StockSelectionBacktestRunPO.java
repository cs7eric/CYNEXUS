package cn.cccs7.cynexus.market.infra.stockselection.entity;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.Data;

@Data
public class StockSelectionBacktestRunPO {
    private Long id;
    private String tenantId;
    private String ownerId;
    private Long strategyVersionId;
    private String strategyCode;
    private Integer strategyVersionNo;
    private LocalDate requestedAsOfDate;
    private LocalDate actualAsOfDate;
    private String status;
    private String config;
    private String benchmark;
    private String dataSource;
    private String warnings;
    private String summary;
    private OffsetDateTime startedAt;
    private OffsetDateTime completedAt;
}
