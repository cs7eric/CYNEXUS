package cn.cccs7.cynexus.market.infra.stockselection.entity;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class StockSelectionStrategyCandidatePO {
    private Long id;
    private Long strategyRunId;
    private String stockCode;
    private String stockName;
    private Integer rankNo;
    private BigDecimal score;
    private String reasonSummary;
    private String snapshot;
}
