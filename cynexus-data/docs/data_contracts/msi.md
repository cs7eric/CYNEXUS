# MSI Data Contract

## Composite Table

`cynexus_data.msi_index_daily`

- `trade_date`: primary key.
- `*_score`: the 9 MSI component scores, each normalized to `0..100` when present.
- `composite_score`: weighted score.
- `component_count`: number of non-null components used.
- `level`: `hot`, `active`, `normal`, `weak`, or `ice`.
- `source`: currently `composite`.

## Component Policy

Missing components are treated as neutral `50.0` during composite calculation. If `require_full=true`, the job skips dates that do not have all 9 components.

## Sector Breadth Table

`cynexus_data.msi_sector_breadth_daily`

- `advancing`, `declining`, `flat`, `total`: counts from industry fund-flow rows.
- `advance_pct`: `advancing / total`.
- `score`: `advance_pct * 100`.
- `source`: expected to be `postgres.sector_fund_flow_daily_snapshot` for production runs.

## MA Count Table

`cynexus_data.msi_ma_count_daily`

- Shared upstream raw values for MSI price strength and market breadth.
- `new_high_252d_pct` feeds `price_strength`.
- `breadth_raw = 0.40 * advancing_pct + 0.35 * pct_ma20 + 0.25 * pct_ma60` feeds `breadth`.
- `up_5d_pct` and `new_low_60d_pct` feed `profit_effect`.

## Profit Effect Table

`cynexus_data.msi_profit_effect_daily`

- `score = 0.60 * up_5d_pct + 0.40 * (100 - new_low_60d_pct)`.
- The composite layer may percentile-normalize this raw score later, matching the legacy cache-aside behavior.

## Turnover Activity Table

`cynexus_data.msi_turnover_activity_daily`

- Stores market amount activity, prior-window average, `ratio`, and `score`.

## Volatility Sentiment Table

`cynexus_data.msi_volatility_daily`

- Stores realized volatility, one-year percentile, and `sentiment_score`.

## Risk Appetite Table

`cynexus_data.msi_risk_appetite_daily`

- Stores HS300, treasury ETF returns, spread values, and `score`.

## Style Risk Table

`cynexus_data.msi_style_risk_daily`

- Stores HS300/CSI1000 returns, spread, and `score`.

## Limit Emotion Table

`cynexus_data.msi_limit_emotion_daily`

- Stores limit-up/down summary counts, sub-scores, composite score, and level.
- Upstream snapshot generation should prefer `sec_stock_basic.limit_up_threshold` and `is_st` for ST-aware limit-up/down detection.
- The runtime payload includes `streakSummary` for max height, leaders, streak distribution, promoted rates, and broken-streak stocks. The compact PG table stores the MSI daily summary score; richer streak snapshots can be promoted to a dedicated table later if UI/history consumers need it.

## Weights

- `vol`: 15%
- `turnover`: 15%
- `price_strength`: 10%
- `risk_appetite`: 10%
- `breadth`: 15%
- `limit_emotion`: 15%
- `profit_effect`: 10%
- `sector_breadth`: 5%
- `style_risk`: 5%
