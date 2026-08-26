ALTER TABLE cynexus_appl_market.stock_selection_strategy_backtest_run
    DROP CONSTRAINT IF EXISTS ck_stock_selection_backtest_status;

ALTER TABLE cynexus_appl_market.stock_selection_strategy_backtest_run
    ADD CONSTRAINT ck_stock_selection_backtest_status
    CHECK (status IN ('QUEUED', 'RUNNING', 'SUCCEEDED', 'NO_DATA', 'FAILED'));
