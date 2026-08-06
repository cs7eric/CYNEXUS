CREATE SCHEMA IF NOT EXISTS cynexus_appl_market;

CREATE TABLE IF NOT EXISTS cynexus_appl_market.market_indicator_sector_episodes (
    id bigint PRIMARY KEY,
    episode_id text NOT NULL UNIQUE,
    trade_date date NOT NULL,
    sector_type text NOT NULL,
    sector_code text,
    sector_name text NOT NULL,
    direction text NOT NULL,
    stage text NOT NULL,
    started_at timestamptz NOT NULL,
    confirmed_at timestamptz,
    last_evidence_at timestamptz NOT NULL,
    ended_at timestamptz,
    eligible_count integer NOT NULL DEFAULT 0,
    observed_count integer NOT NULL DEFAULT 0,
    responded_count integer NOT NULL DEFAULT 0,
    coverage_ratio numeric(12,6),
    coverage_peak numeric(12,6),
    median_lag_seconds integer,
    p75_lag_seconds integer,
    sector_equal_return_pct numeric(14,6),
    sector_median_return_pct numeric(14,6),
    ex_leader_return_pct numeric(14,6),
    weighted_return_pct numeric(14,6),
    benchmark_excess_pct numeric(14,6),
    breadth_delta_pct_point numeric(14,6),
    amount_acceleration numeric(20,8),
    confidence_score numeric(12,6),
    confidence_level text,
    data_quality_grade text,
    source text NOT NULL,
    algorithm_version text NOT NULL,
    payload jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz
);
CREATE INDEX IF NOT EXISTS idx_market_indicator_episode_latest
    ON cynexus_appl_market.market_indicator_sector_episodes (trade_date, last_evidence_at DESC, confidence_score DESC)
    WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS cynexus_appl_market.market_indicator_episode_outcomes (
    id bigint PRIMARY KEY,
    episode_id text NOT NULL,
    anchor_type text NOT NULL,
    evaluated_at timestamptz NOT NULL,
    outcome text NOT NULL,
    algorithm_version text NOT NULL,
    payload jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    CONSTRAINT uk_market_indicator_episode_outcome UNIQUE (episode_id, evaluated_at)
);
CREATE INDEX IF NOT EXISTS idx_market_indicator_episode_outcome_date
    ON cynexus_appl_market.market_indicator_episode_outcomes (evaluated_at DESC)
    WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS cynexus_appl_market.market_indicator_trade_reference_advice (
    id bigint PRIMARY KEY,
    advice_id text NOT NULL UNIQUE,
    episode_id text NOT NULL,
    target_type text NOT NULL,
    target_code text,
    target_name text NOT NULL,
    position_state text NOT NULL,
    action_class text NOT NULL,
    generated_at timestamptz NOT NULL,
    as_of timestamptz NOT NULL,
    valid_until timestamptz NOT NULL,
    horizon text NOT NULL,
    confidence numeric(12,6),
    disclaimer_code text NOT NULL,
    policy_version text NOT NULL,
    source text NOT NULL,
    payload jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz
);
CREATE INDEX IF NOT EXISTS idx_market_indicator_advice_latest
    ON cynexus_appl_market.market_indicator_trade_reference_advice (generated_at DESC, confidence DESC)
    WHERE deleted_at IS NULL;
