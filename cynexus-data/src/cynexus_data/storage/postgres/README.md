# storage/postgres

Postgres storage layer.

Use this folder for UI-serving repositories, metadata tables, task states, and upsert logic. Do not store large high-frequency raw records here.

Repositories own DDL application and small business-result snapshots. High-volume market facts stay in ClickHouse.

THS industry constituents are stored as lightweight snapshot/membership rows in `ths_industry_constituent_*`; the pipeline replaces only requested industry codes for a date/source so partial refreshes are safe.

Market overview detail uses `market_overview_detail_snapshot` for compact JSONB UI payloads generated from ClickHouse bars. It intentionally stores summaries, not OHLCV history.

Index returns use `mkt_index_return_daily` for broad-index window return serving rows. Raw index OHLCV stays in ClickHouse `index_daily_raw`.

TDX industry quotes use `tdx_industry_quote_*` for lightweight TDX 56 industry quote snapshots. These are serving/ranking rows, not high-frequency facts.

Market heatmaps use `market_heatmap_snapshot` for compact JSONB UI payloads derived from persisted industry quote rows.

TDX sector quote details use `tdx_sector_quote_detail_snapshot` for JSONB `theme_market` sections keyed by date and industry code.

Stock turnover uses `stock_turnover_series_snapshot` for compact per-stock turnover JSONB series; raw OHLCV stays in ClickHouse.

Limit emotion stock lists use `market_limit_stock_snapshot` for compact limit-up, limit-down, and broken-board serving rows. Raw OHLCV and intraday facts stay in ClickHouse.

Style sectors use `market_style_sector_snapshot` for equal-weight style-board change snapshots built from stock-sector mappings and Tencent realtime quotes.
