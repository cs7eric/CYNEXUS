package cn.cccs7.cynexus.market.application.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/** Dispatches one durable historical replay at a time to keep market-data load bounded. */
@Service
public class StockSelectionBacktestRuntimeService {

    private final StockSelectionStrategyBacktestAppService backtestAppService;

    public StockSelectionBacktestRuntimeService(StockSelectionStrategyBacktestAppService backtestAppService) {
        this.backtestAppService = backtestAppService;
    }

    @Scheduled(initialDelayString = "${cynexus.market.screener.backtest.initial-delay-ms:1000}",
            fixedDelayString = "${cynexus.market.screener.backtest.dispatch-delay-ms:1000}")
    public void dispatch() {
        backtestAppService.processNextQueuedBacktest();
    }
}
