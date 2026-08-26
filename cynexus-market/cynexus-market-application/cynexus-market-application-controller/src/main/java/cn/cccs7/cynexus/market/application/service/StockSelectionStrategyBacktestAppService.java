package cn.cccs7.cynexus.market.application.service;

import cn.cccs7.cynexus.market.api.dto.StockSelectionStrategyDTO;
import cn.cccs7.cynexus.market.domain.stockselection.StockSelectionBacktestBO;
import cn.cccs7.cynexus.market.domain.stockselection.StockSelectionStrategyBacktestDomainService;
import cn.cccs7.cynexus.market.domain.stockselection.StockSelectionStrategyRunBO;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;

@Service
public class StockSelectionStrategyBacktestAppService {

    private final StockSelectionStrategyBacktestDomainService domainService;

    public StockSelectionStrategyBacktestAppService(StockSelectionStrategyBacktestDomainService domainService) {
        this.domainService = domainService;
    }

    public StockSelectionStrategyDTO.BacktestResponse queue(StockSelectionStrategyRunBO.Scope scope,
                                                             String strategyCode,
                                                             StockSelectionStrategyDTO.BacktestRequest request) {
        StockSelectionBacktestBO.Request input = request == null ? null : new StockSelectionBacktestBO.Request(
                request.getAsOfDate(), request.getLookbackSessions(), request.getSignalIntervalSessions(),
                request.getHoldingSessions(), request.getMaxCandidatesPerSignal(), request.getTransactionCostBpsPerSide(),
                request.getStockCode(), request.getStockMarket(), request.getTargetSymbols() == null ? java.util.List.of()
                : request.getTargetSymbols().stream().map(value -> new StockSelectionBacktestBO.MarketSymbol(value.getMarket(), value.getCode())).toList());
        return response(domainService.queue(scope, strategyCode, input));
    }

    public void processNextQueuedBacktest() {
        domainService.claimNextQueued().ifPresent(claimedRun -> {
            try {
                domainService.complete(claimedRun);
            } catch (RuntimeException exception) {
                domainService.fail(claimedRun);
            }
        });
    }

    public StockSelectionStrategyDTO.BacktestResponse find(StockSelectionStrategyRunBO.Scope scope, long id) {
        return response(domainService.find(scope, id));
    }

    private StockSelectionStrategyDTO.BacktestResponse response(StockSelectionBacktestBO.Run value) {
        return StockSelectionStrategyDTO.BacktestResponse.builder()
                .id(value.id()).strategyCode(value.strategyCode()).strategyVersionId(value.strategyVersionId()).strategyVersionNo(value.strategyVersionNo())
                .requestedAsOfDate(date(value.requestedAsOfDate())).actualAsOfDate(date(value.actualAsOfDate())).status(value.status())
                .config(config(value.config())).benchmark(value.benchmark()).dataSource(value.dataSource()).warnings(value.warnings())
                .summary(summary(value.summary())).startedAt(time(value.startedAt())).completedAt(time(value.completedAt()))
                .trades(value.trades().stream().map(this::trade).toList()).build();
    }

    private StockSelectionStrategyDTO.BacktestResponse response(StockSelectionBacktestBO.StoredRun value) {
        return StockSelectionStrategyDTO.BacktestResponse.builder()
                .id(value.id()).strategyCode(value.strategyCode()).strategyVersionId(value.strategyVersionId()).strategyVersionNo(value.strategyVersionNo())
                .requestedAsOfDate(date(value.requestedAsOfDate())).actualAsOfDate(date(value.actualAsOfDate())).status(value.status())
                .config(config(value.config())).benchmark(value.benchmark()).dataSource(value.dataSource()).warnings(value.warnings())
                .summary(summary(value.summary())).startedAt(time(value.startedAt())).completedAt(time(value.completedAt()))
                .trades(value.trades().stream().map(this::trade).toList()).build();
    }

    private StockSelectionStrategyDTO.BacktestConfig config(StockSelectionBacktestBO.Config value) {
        return StockSelectionStrategyDTO.BacktestConfig.builder().requestedAsOfDate(date(value.requestedAsOfDate()))
                .lookbackSessions(value.lookbackSessions()).signalIntervalSessions(value.signalIntervalSessions())
                .holdingSessions(value.holdingSessions()).maxCandidatesPerSignal(value.maxCandidatesPerSignal())
                .transactionCostBpsPerSide(value.transactionCostBpsPerSide()).stockCode(value.stockCode())
                .stockMarket(value.stockMarket()).targetSymbols(value.targetSymbols().stream()
                        .map(value1 -> StockSelectionStrategyDTO.TargetSymbol.builder().market(value1.market()).code(value1.code()).build())
                        .toList()).build();
    }

    private StockSelectionStrategyDTO.BacktestSummary summary(StockSelectionBacktestBO.Summary value) {
        return StockSelectionStrategyDTO.BacktestSummary.builder().signalCount(value.signalCount()).evaluatedTradeCount(value.evaluatedTradeCount())
                .skippedTradeCount(value.skippedTradeCount()).winRatePct(value.winRatePct()).averageNetReturnPct(value.averageNetReturnPct())
                .medianNetReturnPct(value.medianNetReturnPct()).benchmarkAverageReturnPct(value.benchmarkAverageReturnPct())
                .averageExcessReturnPct(value.averageExcessReturnPct()).build();
    }

    private StockSelectionStrategyDTO.BacktestTrade trade(StockSelectionBacktestBO.Trade value) {
        return StockSelectionStrategyDTO.BacktestTrade.builder().id(value.id()).signalDate(date(value.signalDate()))
                .entryDate(date(value.entryDate())).exitDate(date(value.exitDate())).market(value.market()).stockCode(value.stockCode())
                .stockName(value.stockName()).signalRank(value.signalRank()).score(value.score()).entryPrice(value.entryPrice())
                .exitPrice(value.exitPrice()).grossReturnPct(value.grossReturnPct()).netReturnPct(value.netReturnPct())
                .benchmarkReturnPct(value.benchmarkReturnPct()).excessReturnPct(value.excessReturnPct()).outcome(value.outcome()).build();
    }

    private String date(java.time.LocalDate value) { return value == null ? null : value.toString(); }
    private String time(OffsetDateTime value) { return value == null ? null : value.toString(); }
}
