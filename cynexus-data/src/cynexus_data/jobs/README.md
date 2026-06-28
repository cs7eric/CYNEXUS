# jobs

Executable entrypoints.

Jobs parse CLI arguments, create runtime context, call pipelines, and report operator-facing progress. Keep business logic in pipelines/domain/storage.

Current jobs:

- `ingest_parquet_1m_to_clickhouse.py`
- `ingest_tdx_1m_to_clickhouse.py`
- `health_check.py`
- `sync_kline_daily.py`
- `sync_kline_minute.py`
- `aggregate_kline_periods.py`
- `sync_fund_flow.py`
- `build_index_returns.py`
- `build_market_heatmap.py`
- `sync_tdx_industry_quotes.py`
- `sync_tdx_sector_quote_details.py`
- `sync_stock_turnover.py`
- `build_market_overview.py`
- `build_market_overview_detail.py`
- `build_market_pulse.py`
- `build_ma_count.py`
- `build_turnover_activity.py`
- `build_volatility_sentiment.py`
- `build_risk_appetite.py`
- `build_style_risk.py`
- `build_limit_emotion.py`
- `build_msi.py`
- `build_profit_effect.py`
- `build_sector_breadth.py`
- `trigger_probe.py`
- `transcribe_media.py`

All migrated jobs should be registered in `registry.py` and invoked through `runtime.job_runner.JobRunner`.
