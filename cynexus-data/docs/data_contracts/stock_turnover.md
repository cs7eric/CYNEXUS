# Stock Turnover Contract

`sync_stock_turnover` produces per-stock turnover-rate series snapshots for Java/UI reads.

## Formula

The migrated formula follows legacy `f10/turnover.py` and `eltdx_adapter.py`:

`turnover_rate = volume_lots * 10000 / circulating_shares`

`volume_lots` comes from daily K-line rows. `circulating_shares` and `total_shares` normally come from eltdx `helpers.stock_profile_table`.

## Output Table

`cynexus_appl_market.stock_turnover_series_snapshot`

- `symbol`: six-digit stock code.
- `target_type`: normally `stock`.
- `period`: currently `1d`.
- `adjust`: `none`, `qfq`, or `hfq` K-line source.
- `payload`: full JSONB series with entries.
- `latest_trade_date` / `latest_turnover_rate`: denormalized latest values for quick UI reads.

The old `reference/stock/turnover/*.json` cache is intentionally not migrated.
