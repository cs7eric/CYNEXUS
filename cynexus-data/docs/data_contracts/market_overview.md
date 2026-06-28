# Market Overview Data Contract

## PostgreSQL Table

`cynexus_data.market_overview_snapshot`

One active daily row stores:

- Market overview fields from `eltdx`, `eastmoney`, or `tencent`: `total_amount`, `total_volume`, rising/falling/flat counts, limit-up/down counts, and stock count.
- Fund-flow fields from `akshare` or `manual`: main/super-large/large/medium/small net inflow and ratio fields.
- Merge metadata: `source`, `is_manual_override`, `manual_updated_at`, and `extra`.

## Merge Rules

- `eltdx`, `eastmoney`, and `tencent` may overwrite market overview fields on same-day refresh.
- `akshare` and `manual` may overwrite fund-flow fields.
- Other existing non-null fields are preserved.
- `source` is merged as a deduplicated `+`-joined string.

## Technical Helper Payloads

`build_market_overview_detail` persists the UI-ready detail payload in:

`cynexus_data.market_overview_detail_snapshot`

Primary key:

- `trade_date`
- `benchmark_code`
- `source_table`

Columns:

- `payload`: JSONB object generated from pure market technical helpers.
- `source`: producer tag, default `python`.
- `generated_at`, `created_at`, `updated_at`, `deleted_at`.

The payload contains:

- Window metric rows include `window`, `returnN`, `highN`, `lowN`, `rangePosition`, `drawdownFromHigh`, `reboundFromLow`, `volatility`, `upDaysRatio`, `volumeRatio`, `amountRatio`, MA position flags, and MA slope fields.
- Support/resistance rows include `price`, `type`, `source`, `strength`, `label`, and `distancePct`.
- Style rows include `style`, `source`, `relativeReturn5`, `relativeReturn20`, `relativeReturn60`, and `state`.
- Industry-strength rows include `name`, `indexName`, `symbol`, `industryQuery`, relative return fields, `state`, and `score`.
- Sentiment payloads include `todayScore`, `trendScore`, `riskDiffusionScore`, `state`, `trend`, `score5`, `score20`, limit counts, `breakRate`, and `totalTurnover`.
- Similar-scenario payloads include `matchedCount`, `matchedDates`, `medianDistance`, `matchedDetails`, `forwardStats`, and a structured `conclusion`.

PostgreSQL stores only this compact JSON snapshot. ClickHouse remains the source of daily/index OHLCV facts.
