# MSI Pipeline

## Jobs

- `build_msi`: build one date's market sentiment composite.
- `build_ma_count`: build MSI price-strength and market-breadth upstream values from ClickHouse daily adjusted K-line data.
- `build_profit_effect`: build MSI profit-effect raw score from `msi_ma_count_daily`.
- `build_turnover_activity`: build turnover activity from Shanghai/Shenzhen index amount rows.
- `build_volatility_sentiment`: build realized-volatility sentiment from index close series.
- `build_risk_appetite`: build HS300 versus treasury ETF spread.
- `build_style_risk`: build CSI1000 versus HS300 style spread.
- `build_limit_emotion`: build limit-up/limit-down emotion summary.
- `build_sector_breadth`: build MSI factor 8 from sector fund-flow snapshots.
- `rebuild_msi`: run `build_msi` across a date range.

## Current Migration State

This pass migrates the composite layer:

1. Parse component scores from request params, or read existing PostgreSQL component tables when running against storage.
2. Apply the legacy 9-factor weights.
3. Treat missing scores as neutral `50.0`.
4. Persist `msi_index_daily` when not in dry-run mode.

Individual factor producers stay separate from the composite builder so each one can be repaired/backfilled independently.

`build_ma_count` and `build_profit_effect` migrate two of those producers:

- `build_ma_count` computes MA20/MA60 participation, 5-day up ratio, 60-day new lows, 252-day new highs, and `breadth_raw`.
- `build_profit_effect` computes `0.60 * up_5d_pct + 0.40 * (100 - new_low_60d_pct)`.
- `build_turnover_activity` computes current index amount divided by the prior window average. Historical percentile score can be passed as `score`; otherwise the factor stores neutral `50`.
- `build_volatility_sentiment` computes annualized realized volatility and scores lower volatility as higher sentiment using in-window percentile rank.
- `build_risk_appetite` computes HS300 return minus weighted treasury ETF return. Historical percentile score can be passed as `score`; otherwise the factor stores neutral `50`.
- `build_style_risk` computes CSI1000 return minus HS300 return. Historical percentile score can be passed as `score`; otherwise the factor stores neutral `50`.
- `build_limit_emotion` computes limit-up/down ratio, break-board rate, yesterday limit-up average return, sub-scores, composite score, and level. Historical score overrides can be passed through `score_overrides` during repair.
- In production mode `build_limit_emotion` reads OHLC/pre-close rows from ClickHouse and uses `sec_stock_basic.limit_up_threshold` / `is_st` to build ST-aware limit snapshots before summarizing. If stock metadata is unavailable, it falls back to code-prefix threshold rules and records a note.
- It also reads a bounded historical OHLC window (`streak_lookback_days`, default `120`) to compute `previousLimitUpStreak`, `limitUpStreak`, `isPromoted`, `isBrokenStreak`, and `streakSummary`.

`build_sector_breadth` is implemented because it depends directly on the migrated THS industry fund-flow snapshot:

```
advance_pct = advancing_industries / total_industries
score = advance_pct * 100
```

## Parameters

- `trade_date`: target date for `build_msi`.
- `start_date`, `end_date`: range for `rebuild_msi`.
- `force`: bypass cached index reads.
- `require_full`: skip if any component is missing.
- `create_table`: apply `sql/postgres/20260628_create_msi_tables.sql`.
- `components`: JSON object containing component scores, useful for tests/manual repair.
