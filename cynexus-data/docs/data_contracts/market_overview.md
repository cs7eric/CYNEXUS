# Market Overview Data Contract

## PostgreSQL Table

`cynexus_data.market_overview_snapshot`

One active daily row stores:

- Market overview fields from `eltdx` or `eastmoney`: `total_amount`, `total_volume`, rising/falling/flat counts, limit-up/down counts, and stock count.
- Fund-flow fields from `akshare` or `manual`: main/super-large/large/medium/small net inflow and ratio fields.
- Merge metadata: `source`, `is_manual_override`, `manual_updated_at`, and `extra`.

## Merge Rules

- `eltdx` and `eastmoney` may overwrite market overview fields on same-day refresh.
- `akshare` and `manual` may overwrite fund-flow fields.
- Other existing non-null fields are preserved.
- `source` is merged as a deduplicated `+`-joined string.
