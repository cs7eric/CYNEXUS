package cn.cccs7.cynexus.market.infra.stockselection;

import cn.cccs7.cynexus.market.domain.stockselection.StockSelectionBacktestBO;
import cn.cccs7.cynexus.market.domain.stockselection.StockSelectionBacktestRepository;
import cn.cccs7.cynexus.market.domain.stockselection.StockSelectionStrategyRunBO;
import cn.cccs7.cynexus.market.infra.stockselection.entity.StockSelectionBacktestRunPO;
import cn.cccs7.cynexus.market.infra.stockselection.entity.StockSelectionBacktestTradePO;
import cn.cccs7.cynexus.market.infra.stockselection.mapper.StockSelectionBacktestMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.List;
import java.util.Optional;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class StockSelectionBacktestRepositoryAdapter implements StockSelectionBacktestRepository {

    private final StockSelectionBacktestMapper mapper;
    private final ObjectMapper objectMapper;

    public StockSelectionBacktestRepositoryAdapter(StockSelectionBacktestMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper.copy().registerModule(new JavaTimeModule());
    }

    @Override
    @Transactional
    public void save(StockSelectionBacktestBO.Run value) {
        mapper.insertRun(run(value));
        for (StockSelectionBacktestBO.Trade trade : value.trades()) {
            mapper.insertTrade(trade(value.id(), trade));
        }
    }

    @Override
    @Transactional
    public Optional<StockSelectionBacktestBO.ClaimedRun> claimNextQueued() {
        StockSelectionBacktestRunPO next = mapper.findNextQueued();
        if (next == null || mapper.claimRun(next.getId()) != 1) return Optional.empty();
        return Optional.of(new StockSelectionBacktestBO.ClaimedRun(
                new StockSelectionStrategyRunBO.Scope(next.getTenantId(), next.getOwnerId()), next.getId()));
    }

    @Override
    @Transactional
    public void complete(StockSelectionBacktestBO.Run value) {
        if (mapper.completeRun(run(value)) != 1) {
            throw new IllegalStateException("backtest was not in a running state");
        }
        mapper.deleteTrades(value.id());
        for (StockSelectionBacktestBO.Trade trade : value.trades()) {
            mapper.insertTrade(trade(value.id(), trade));
        }
    }

    @Override
    public void fail(StockSelectionBacktestBO.ClaimedRun value, String warning) {
        mapper.failRun(value.scope().tenantId(), value.scope().ownerId(), value.id(), json(List.of(warning)), OffsetDateTime.now());
    }

    private StockSelectionBacktestRunPO run(StockSelectionBacktestBO.Run value) {
        StockSelectionBacktestRunPO run = new StockSelectionBacktestRunPO();
        run.setId(value.id());
        run.setTenantId(value.scope().tenantId());
        run.setOwnerId(value.scope().ownerId());
        run.setStrategyVersionId(value.strategyVersionId());
        run.setRequestedAsOfDate(value.requestedAsOfDate());
        run.setActualAsOfDate(value.actualAsOfDate());
        run.setStatus(value.status());
        run.setConfig(json(value.config()));
        run.setBenchmark(value.benchmark());
        run.setDataSource(value.dataSource());
        run.setWarnings(json(value.warnings()));
        run.setSummary(json(value.summary()));
        run.setStartedAt(value.startedAt());
        run.setCompletedAt(value.completedAt());
        return run;
    }

    @Override
    public Optional<StockSelectionBacktestBO.StoredRun> findById(StockSelectionStrategyRunBO.Scope scope, long backtestId) {
        StockSelectionBacktestRunPO run = mapper.findRun(scope.tenantId(), scope.ownerId(), backtestId);
        if (run == null) return Optional.empty();
        return Optional.of(storedRun(run));
    }

    @Override
    public Optional<StockSelectionBacktestBO.StoredRun> findLatestCompletedByStrategy(StockSelectionStrategyRunBO.Scope scope, String strategyCode) {
        StockSelectionBacktestRunPO run = mapper.findLatestCompleted(scope.tenantId(), scope.ownerId(), strategyCode);
        return run == null ? Optional.empty() : Optional.of(storedRun(run));
    }

    private StockSelectionBacktestBO.StoredRun storedRun(StockSelectionBacktestRunPO run) {
        List<StockSelectionBacktestBO.Trade> trades = mapper.findTrades(run.getId()).stream().map(this::trade).toList();
        return new StockSelectionBacktestBO.StoredRun(run.getId(), run.getStrategyCode(), run.getStrategyVersionId(), run.getStrategyVersionNo(),
                run.getRequestedAsOfDate(), run.getActualAsOfDate(), run.getStatus(), config(run.getConfig()), run.getBenchmark(),
                run.getDataSource(), list(run.getWarnings()), summary(run.getSummary()), run.getStartedAt(), run.getCompletedAt(), trades);
    }

    private StockSelectionBacktestBO.Trade trade(StockSelectionBacktestTradePO value) {
        return new StockSelectionBacktestBO.Trade(value.getId(), value.getSignalDate(), value.getEntryDate(), value.getExitDate(),
                value.getMarket(), value.getStockCode(), value.getStockName(), value.getSignalRank(), value.getScore(),
                value.getEntryPrice(), value.getExitPrice(), value.getGrossReturnPct(), value.getNetReturnPct(),
                value.getBenchmarkReturnPct(), value.getExcessReturnPct(), value.getOutcome());
    }

    private StockSelectionBacktestTradePO trade(long runId, StockSelectionBacktestBO.Trade trade) {
        StockSelectionBacktestTradePO item = new StockSelectionBacktestTradePO();
        item.setId(trade.id()); item.setBacktestRunId(runId); item.setSignalDate(trade.signalDate());
        item.setEntryDate(trade.entryDate()); item.setExitDate(trade.exitDate()); item.setMarket(trade.market());
        item.setStockCode(trade.stockCode()); item.setStockName(trade.stockName()); item.setSignalRank(trade.signalRank());
        item.setScore(trade.score()); item.setEntryPrice(trade.entryPrice()); item.setExitPrice(trade.exitPrice());
        item.setGrossReturnPct(trade.grossReturnPct()); item.setNetReturnPct(trade.netReturnPct());
        item.setBenchmarkReturnPct(trade.benchmarkReturnPct()); item.setExcessReturnPct(trade.excessReturnPct());
        item.setOutcome(trade.outcome());
        return item;
    }

    private StockSelectionBacktestBO.Config config(String value) { return read(value, StockSelectionBacktestBO.Config.class); }
    private StockSelectionBacktestBO.Summary summary(String value) { return read(value, StockSelectionBacktestBO.Summary.class); }
    private List<String> list(String value) { return read(value, new TypeReference<List<String>>() { }); }

    private <T> T read(String value, Class<T> type) {
        try { return objectMapper.readValue(value, type); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("Unable to read persisted backtest JSON", exception); }
    }

    private <T> T read(String value, TypeReference<T> type) {
        try { return objectMapper.readValue(value, type); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("Unable to read persisted backtest JSON", exception); }
    }

    private String json(Object value) {
        try { return objectMapper.writeValueAsString(value); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("Unable to persist backtest JSON", exception); }
    }
}
