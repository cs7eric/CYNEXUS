# Market Limit Stock Snapshot Contract

`build_limit_emotion` writes two PostgreSQL outputs:

- `msi_limit_emotion_daily`: daily MSI summary and component score.
- `market_limit_daily_stocks`: existing Java-facing stock-level serving rows for limit-up, limit-down, and broken-board lists.

## Output Table

`cynexus_appl_market.market_limit_daily_stocks`

- `trade_date`: trading date.
- `code` / `name`: stock identity.
- `category`: `limit_up`, `limit_down`, or `broken`.
- `streak`: current limit-up streak for `limit_up`, previous streak for `broken`, and `0` for `limit_down`.
- `change_pct`, `limit_up_price`, `limit_down_price`, `close`: compact display metrics.
- `source`: normally `clickhouse.daily_raw`.
- `payload`: original normalized domain row for traceability.

Raw OHLCV facts remain in ClickHouse. Python now writes the old Java-facing `market_limit_daily_stocks` table directly instead of creating a parallel snapshot table.
