package cn.cccs7.cynexus.market.infra.stockselection.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

@Data
public class StockSelectionBacktestTradePO {
    private Long id;
    private Long backtestRunId;
    private LocalDate signalDate;
    private LocalDate entryDate;
    private LocalDate exitDate;
    private String market;
    private String stockCode;
    private String stockName;
    private Integer signalRank;
    private BigDecimal score;
    private BigDecimal entryPrice;
    private BigDecimal exitPrice;
    private BigDecimal grossReturnPct;
    private BigDecimal netReturnPct;
    private BigDecimal benchmarkReturnPct;
    private BigDecimal excessReturnPct;
    private String outcome;
}
