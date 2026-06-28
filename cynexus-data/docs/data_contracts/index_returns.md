# Index Returns Data Contract

## PostgreSQL Table

`cynexus_data.mkt_index_return_daily`

One row per:

- `trade_date`
- `index_code`
- `window_days`

Fields:

- `index_name`: display name such as `沪深300`.
- `current_close`, `current_trade_date`: latest close inside the window.
- `base_close`, `base_date`: first close inside the window.
- `return_pct`: cumulative window return in percent.
- `source`: default `clickhouse.index_daily_raw`.

This table is a lightweight UI-serving cache. Raw index OHLCV facts stay in ClickHouse `index_daily_raw`.

## Job Payload

`build_index_returns` returns items compatible with the old index return shape:

- `name`
- `code`
- `fullCode`
- `current`
- `currentDate`
- `baseClose`
- `baseDate`
- `returnPct`
- `daily`
- `available`

Default targets:

- `sh000001` 上证指数
- `sh000300` 沪深300
- `sh000852` 中证1000
