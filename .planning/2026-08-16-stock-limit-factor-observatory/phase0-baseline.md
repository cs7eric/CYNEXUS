# Phase 0 Contract Freeze and Baseline

冻结日期：2026-08-18

## Contract freeze

- Event codes: `LIMIT_UP_CLOSE`, `LIMIT_UP_TOUCH`, `LARGE_MOVE_CLOSE`, `INTRADAY_SURGE`, `SWING_BREAKOUT`, `INDUSTRY_CYCLE_MOVE`.
- Time stages: `PRE_EVENT`, `EVENT_OPEN`, `EVENT_INTRADAY`, `EVENT_CLOSE`, `POST_EVENT`.
- Study lifecycle: `PENDING`, `RUNNING`, `SUCCEEDED`, `FAILED`, `CANCELLED`.
- Primary API names: event observatory, event replay, factor study, factor set, strategy run/backtest, watchlist performance.
- AI capability: `market_factor_study_synthesis`; Agent observatory/replay/study tools are read-only and bounded.

The DTO and Agent field contracts are covered by `MarketEventFactorObservatoryAgentToolHandlerTest`. The legacy `screeningHints` field is compatibility-only and deprecated; new research consumes validated factor-set versions.

## Baseline

- Release observatory cold read (`limit=2`): approximately 8.6 seconds.
- Same-key warm read after bounded cache: approximately 17 ms.
- Materialized snapshot coverage: 504 trading dates, 2,804,333 source rows and 2,804,333 materialized rows, 100% coverage, 2024-07-19 through 2026-08-17.
- Snapshot lookup uses `idx_a_stock_daily_factor_snapshot_code_date`.

These values are the acceptance baseline for subsequent UI and query optimizations.
