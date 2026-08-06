CREATE SCHEMA IF NOT EXISTS cynexus_appl_market;

CREATE TABLE IF NOT EXISTS cynexus_appl_market.market_indicator_benchmark_runs (
    id bigint PRIMARY KEY,
    run_id text NOT NULL UNIQUE,
    trade_date date NOT NULL,
    provider text NOT NULL,
    universe_size integer NOT NULL,
    rounds integer NOT NULL,
    target_cadence_seconds integer NOT NULL DEFAULT 5,
    p50_seconds numeric(12,6),
    p95_seconds numeric(12,6),
    p99_seconds numeric(12,6),
    coverage_ratio numeric(12,6),
    fresh_ratio numeric(12,6),
    payload jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_at timestamptz NOT NULL DEFAULT now()
);
