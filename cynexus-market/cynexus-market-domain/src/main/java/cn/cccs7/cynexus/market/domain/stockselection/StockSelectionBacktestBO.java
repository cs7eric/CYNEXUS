package cn.cccs7.cynexus.market.domain.stockselection;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/** Immutable audit record for one bounded, daily-close strategy replay. */
public final class StockSelectionBacktestBO {

    private StockSelectionBacktestBO() {
    }

    public record Request(String asOfDate,
                          Integer lookbackSessions,
                          Integer signalIntervalSessions,
                          Integer holdingSessions,
                          Integer maxCandidatesPerSignal,
                          Integer transactionCostBpsPerSide,
                          String stockCode,
                          String stockMarket,
                          List<MarketSymbol> targetSymbols) {
    }

    public record Config(LocalDate requestedAsOfDate,
                         int lookbackSessions,
                         int signalIntervalSessions,
                         int holdingSessions,
                         int maxCandidatesPerSignal,
                         int transactionCostBpsPerSide,
                         String stockCode,
                         String stockMarket,
                         List<MarketSymbol> targetSymbols) {
    }

    public record MarketSymbol(String market, String code) {
    }

    public record DailyBar(String market, String code, LocalDate tradeDate, BigDecimal close) {
    }

    public record Trade(long id,
                        LocalDate signalDate,
                        LocalDate entryDate,
                        LocalDate exitDate,
                        String market,
                        String stockCode,
                        String stockName,
                        int signalRank,
                        BigDecimal score,
                        BigDecimal entryPrice,
                        BigDecimal exitPrice,
                        BigDecimal grossReturnPct,
                        BigDecimal netReturnPct,
                        BigDecimal benchmarkReturnPct,
                        BigDecimal excessReturnPct,
                        String outcome) {
    }

    public record Summary(int signalCount,
                          int evaluatedTradeCount,
                          int skippedTradeCount,
                          BigDecimal winRatePct,
                          BigDecimal averageNetReturnPct,
                          BigDecimal medianNetReturnPct,
                          BigDecimal benchmarkAverageReturnPct,
                          BigDecimal averageExcessReturnPct) {
    }

    public record Run(long id,
                      StockSelectionStrategyRunBO.Scope scope,
                      String strategyCode,
                      long strategyVersionId,
                      int strategyVersionNo,
                      LocalDate requestedAsOfDate,
                      LocalDate actualAsOfDate,
                      String status,
                      Config config,
                      String benchmark,
                      String dataSource,
                      List<String> warnings,
                      Summary summary,
                      OffsetDateTime startedAt,
                      OffsetDateTime completedAt,
                      List<Trade> trades) {
    }

    public record StoredRun(long id,
                            String strategyCode,
                            long strategyVersionId,
                            int strategyVersionNo,
                            LocalDate requestedAsOfDate,
                            LocalDate actualAsOfDate,
                            String status,
                            Config config,
                            String benchmark,
                            String dataSource,
                            List<String> warnings,
                            Summary summary,
                            OffsetDateTime startedAt,
                            OffsetDateTime completedAt,
                            List<Trade> trades) {
    }

    /** A tenant- and owner-scoped job claimed by the durable backtest dispatcher. */
    public record ClaimedRun(StockSelectionStrategyRunBO.Scope scope, long id) {
    }
}
