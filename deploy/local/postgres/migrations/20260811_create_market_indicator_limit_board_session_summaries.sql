CREATE SCHEMA IF NOT EXISTS cynexus_appl_market;

CREATE TABLE IF NOT EXISTS cynexus_appl_market.market_indicator_limit_board_session_summaries (
    id bigint PRIMARY KEY,
    trade_date date NOT NULL,
    trade_session varchar(16) NOT NULL,
    code varchar(6) NOT NULL,
    name text,
    market varchar(8),
    direction varchar(8) NOT NULL,
    closing_status varchar(16) NOT NULL DEFAULT 'unknown',
    is_sealed_at_close boolean NOT NULL DEFAULT FALSE,
    first_touch_at timestamptz,
    first_sealed_at timestamptz,
    last_sealed_at timestamptz,
    last_observed_at timestamptz,
    sealed_duration_seconds integer NOT NULL DEFAULT 0,
    open_count integer NOT NULL DEFAULT 0,
    reseal_count integer NOT NULL DEFAULT 0,
    sample_count integer NOT NULL DEFAULT 0,
    valid_sample_count integer NOT NULL DEFAULT 0,
    gap_count integer NOT NULL DEFAULT 0,
    coverage_ratio numeric(8,6),
    max_sealed_amount numeric(24,6),
    avg_sealed_amount numeric(24,6),
    median_sealed_amount numeric(24,6),
    peak_sealed_amount_at timestamptz,
    first_sealed_amount numeric(24,6),
    last_sealed_amount numeric(24,6),
    sealed_amount_change numeric(24,6),
    sealed_amount_slope numeric(24,10),
    trend_direction varchar(16) NOT NULL DEFAULT 'insufficient',
    trend_series_origin_at timestamptz,
    trend_time_offsets smallint[] NOT NULL DEFAULT '{}'::smallint[],
    trend_sealed_amounts bigint[] NOT NULL DEFAULT '{}'::bigint[],
    source text NOT NULL,
    algorithm_version varchar(32) NOT NULL DEFAULT 'v1',
    trend_features jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    CONSTRAINT ck_market_indicator_limit_board_summary_session
        CHECK (trade_session IN ('morning', 'afternoon')),
    CONSTRAINT ck_market_indicator_limit_board_summary_direction
        CHECK (direction IN ('up', 'down')),
    CONSTRAINT ck_market_indicator_limit_board_summary_status
        CHECK (closing_status IN ('touch', 'sealed', 'opened', 'unknown')),
    CONSTRAINT ck_market_indicator_limit_board_summary_trend
        CHECK (trend_direction IN ('increasing', 'decreasing', 'flat', 'insufficient')),
    CONSTRAINT ck_market_indicator_limit_board_summary_counts
        CHECK (sealed_duration_seconds >= 0 AND open_count >= 0 AND reseal_count >= 0
            AND sample_count >= 0 AND valid_sample_count >= 0 AND gap_count >= 0),
    CONSTRAINT ck_market_indicator_limit_board_summary_coverage
        CHECK (coverage_ratio IS NULL OR coverage_ratio BETWEEN 0 AND 1),
    CONSTRAINT ck_market_indicator_limit_board_summary_trend_series
        CHECK (cardinality(trend_time_offsets) = cardinality(trend_sealed_amounts)
            AND cardinality(trend_time_offsets) <= 120)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_market_indicator_limit_board_session_summary
    ON cynexus_appl_market.market_indicator_limit_board_session_summaries (trade_date, trade_session, code)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_market_indicator_limit_board_session_summary_code
    ON cynexus_appl_market.market_indicator_limit_board_session_summaries (trade_date, code, trade_session)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_market_indicator_limit_board_session_summary_direction
    ON cynexus_appl_market.market_indicator_limit_board_session_summaries (trade_date, trade_session, direction)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_market_indicator_limit_board_session_summary_amount
    ON cynexus_appl_market.market_indicator_limit_board_session_summaries
        (trade_date, trade_session, max_sealed_amount DESC NULLS LAST)
    WHERE deleted_at IS NULL;
