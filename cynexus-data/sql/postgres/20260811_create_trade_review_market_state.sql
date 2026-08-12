CREATE SCHEMA IF NOT EXISTS cynexus_appl_market;

CREATE TABLE IF NOT EXISTS cynexus_appl_market.trade_review_day_manifest (
    trade_date date PRIMARY KEY,
    status varchar(16) NOT NULL DEFAULT 'PENDING',
    minute_bar_count integer NOT NULL DEFAULT 0,
    minute_symbol_count integer NOT NULL DEFAULT 0,
    context_count integer NOT NULL DEFAULT 0,
    indicator_event_count integer NOT NULL DEFAULT 0,
    market_minute_count integer NOT NULL DEFAULT 0,
    industry_minute_count integer NOT NULL DEFAULT 0,
    daily_pool_count integer NOT NULL DEFAULT 0,
    cache_published boolean NOT NULL DEFAULT false,
    quality jsonb NOT NULL DEFAULT '{}'::jsonb,
    error_message text,
    built_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS cynexus_appl_market.trade_review_security_day_context (
    trade_date date NOT NULL,
    code varchar(16) NOT NULL,
    name varchar(128),
    market varchar(8),
    security_type varchar(16) NOT NULL,
    pre_close numeric(20,6),
    limit_up_price numeric(20,6),
    limit_down_price numeric(20,6),
    previous_streak integer NOT NULL DEFAULT 0,
    previous_limit_status varchar(32),
    memberships jsonb NOT NULL DEFAULT '[]'::jsonb,
    source jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (trade_date, code)
);
CREATE INDEX IF NOT EXISTS idx_trade_review_security_context_date_type
    ON cynexus_appl_market.trade_review_security_day_context (trade_date, security_type);

-- End-of-day process facts are intentionally separate from the minute state:
-- daily replay needs immutable close/limit process evidence for every
-- security without expanding each cursor snapshot to the full universe.
CREATE TABLE IF NOT EXISTS cynexus_appl_market.trade_review_security_daily_fact (
    trade_date date NOT NULL,
    code varchar(16) NOT NULL,
    name varchar(128),
    security_type varchar(16) NOT NULL,
    open numeric(20,6),
    high numeric(20,6),
    low numeric(20,6),
    close numeric(20,6),
    change_pct numeric(12,6),
    amplitude numeric(12,6),
    volume numeric(24,4),
    amount numeric(24,4),
    touch_limit_up boolean NOT NULL DEFAULT false,
    touch_limit_down boolean NOT NULL DEFAULT false,
    final_limit_up boolean NOT NULL DEFAULT false,
    final_limit_down boolean NOT NULL DEFAULT false,
    first_limit_up_at timestamptz,
    first_limit_down_at timestamptz,
    open_count integer NOT NULL DEFAULT 0,
    reseal_count integer NOT NULL DEFAULT 0,
    previous_streak integer NOT NULL DEFAULT 0,
    current_streak integer NOT NULL DEFAULT 0,
    is_advanced boolean NOT NULL DEFAULT false,
    is_broken_streak boolean NOT NULL DEFAULT false,
    previous_limit_status varchar(32),
    intraday_available boolean NOT NULL DEFAULT false,
    is_one_word_limit boolean NOT NULL DEFAULT false,
    event_evidence_quality varchar(32) NOT NULL DEFAULT 'derived_1m',
    memberships jsonb NOT NULL DEFAULT '[]'::jsonb,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (trade_date, code)
);
CREATE INDEX IF NOT EXISTS idx_trade_review_security_daily_fact_limit
    ON cynexus_appl_market.trade_review_security_daily_fact (trade_date, final_limit_up, current_streak DESC);

CREATE TABLE IF NOT EXISTS cynexus_appl_market.trade_review_indicator_event (
    id bigint PRIMARY KEY,
    trade_date date NOT NULL,
    code varchar(16) NOT NULL,
    name varchar(128),
    event_type varchar(48) NOT NULL,
    event_at timestamptz NOT NULL,
    effective_minute timestamptz NOT NULL,
    price numeric(20,6),
    change_pct numeric(12,6),
    limit_up_price numeric(20,6),
    limit_down_price numeric(20,6),
    source varchar(64) NOT NULL,
    payload jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (trade_date, code, event_type, event_at)
);
CREATE INDEX IF NOT EXISTS idx_trade_review_indicator_event_cursor
    ON cynexus_appl_market.trade_review_indicator_event (trade_date, event_at, code);

CREATE TABLE IF NOT EXISTS cynexus_appl_market.trade_review_limit_state_interval (
    id bigint PRIMARY KEY,
    trade_date date NOT NULL,
    code varchar(16) NOT NULL,
    state varchar(24) NOT NULL,
    started_at timestamptz NOT NULL,
    ended_at timestamptz,
    event_count integer NOT NULL DEFAULT 0,
    source varchar(64) NOT NULL,
    payload jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (trade_date, code, state, started_at)
);
CREATE INDEX IF NOT EXISTS idx_trade_review_limit_interval_cursor
    ON cynexus_appl_market.trade_review_limit_state_interval (trade_date, started_at, ended_at, code);

CREATE TABLE IF NOT EXISTS cynexus_appl_market.trade_review_market_minute_state (
    trade_date date NOT NULL,
    minute_at timestamptz NOT NULL,
    advancing_count integer NOT NULL DEFAULT 0,
    declining_count integer NOT NULL DEFAULT 0,
    flat_count integer NOT NULL DEFAULT 0,
    above_5_count integer NOT NULL DEFAULT 0,
    below_minus_5_count integer NOT NULL DEFAULT 0,
    touched_limit_up_count integer NOT NULL DEFAULT 0,
    sealed_limit_up_count integer NOT NULL DEFAULT 0,
    broken_limit_up_count integer NOT NULL DEFAULT 0,
    sealed_limit_down_count integer NOT NULL DEFAULT 0,
    max_height integer NOT NULL DEFAULT 0,
    seal_rate numeric(12,6),
    break_rate numeric(12,6),
    ladder jsonb NOT NULL DEFAULT '[]'::jsonb,
    source_confidence varchar(16) NOT NULL DEFAULT 'derived_1m',
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (trade_date, minute_at)
);

CREATE TABLE IF NOT EXISTS cynexus_appl_market.trade_review_industry_minute_state (
    trade_date date NOT NULL,
    minute_at timestamptz NOT NULL,
    membership varchar(12) NOT NULL,
    industry_code varchar(64) NOT NULL,
    industry_name varchar(128) NOT NULL,
    stock_count integer NOT NULL DEFAULT 0,
    advancing_count integer NOT NULL DEFAULT 0,
    declining_count integer NOT NULL DEFAULT 0,
    flat_count integer NOT NULL DEFAULT 0,
    touched_limit_up_count integer NOT NULL DEFAULT 0,
    sealed_limit_up_count integer NOT NULL DEFAULT 0,
    broken_limit_up_count integer NOT NULL DEFAULT 0,
    sealed_limit_down_count integer NOT NULL DEFAULT 0,
    max_height integer NOT NULL DEFAULT 0,
    seal_rate numeric(12,6),
    break_rate numeric(12,6),
    relative_change_pct numeric(12,6),
    ladder jsonb NOT NULL DEFAULT '[]'::jsonb,
    core_pool jsonb NOT NULL DEFAULT '[]'::jsonb,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (trade_date, minute_at, membership, industry_code)
);
CREATE INDEX IF NOT EXISTS idx_trade_review_industry_minute_cursor
    ON cynexus_appl_market.trade_review_industry_minute_state (trade_date, membership, industry_code, minute_at);

CREATE TABLE IF NOT EXISTS cynexus_appl_market.trade_review_minute_pool_index (
    trade_date date NOT NULL,
    minute_at timestamptz NOT NULL,
    pool_type varchar(32) NOT NULL,
    membership varchar(12) NOT NULL DEFAULT 'union',
    industry_code varchar(64) NOT NULL DEFAULT '',
    stocks jsonb NOT NULL DEFAULT '[]'::jsonb,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (trade_date, minute_at, pool_type, membership, industry_code)
);

-- Compact cursor index for second-resolution Indicator evidence.  The
-- individual events remain queryable in trade_review_indicator_event; this
-- table avoids rebuilding minute buckets on every replay request.
CREATE TABLE IF NOT EXISTS cynexus_appl_market.trade_review_minute_event_index (
    trade_date date NOT NULL,
    minute_at timestamptz NOT NULL,
    events jsonb NOT NULL DEFAULT '[]'::jsonb,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (trade_date, minute_at)
);

CREATE TABLE IF NOT EXISTS cynexus_appl_market.trade_review_market_daily_state (
    trade_date date PRIMARY KEY,
    advancing_count integer NOT NULL DEFAULT 0,
    declining_count integer NOT NULL DEFAULT 0,
    flat_count integer NOT NULL DEFAULT 0,
    above_5_count integer NOT NULL DEFAULT 0,
    below_minus_5_count integer NOT NULL DEFAULT 0,
    touched_limit_up_count integer NOT NULL DEFAULT 0,
    sealed_limit_up_count integer NOT NULL DEFAULT 0,
    broken_limit_up_count integer NOT NULL DEFAULT 0,
    sealed_limit_down_count integer NOT NULL DEFAULT 0,
    max_height integer NOT NULL DEFAULT 0,
    seal_rate numeric(12,6),
    break_rate numeric(12,6),
    promotion_rate numeric(12,6),
    break_streak_rate numeric(12,6),
    ladder jsonb NOT NULL DEFAULT '[]'::jsonb,
    source jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS cynexus_appl_market.trade_review_industry_daily_state (
    trade_date date NOT NULL,
    membership varchar(12) NOT NULL,
    industry_code varchar(64) NOT NULL,
    industry_name varchar(128) NOT NULL,
    stock_count integer NOT NULL DEFAULT 0,
    advancing_count integer NOT NULL DEFAULT 0,
    declining_count integer NOT NULL DEFAULT 0,
    touched_limit_up_count integer NOT NULL DEFAULT 0,
    sealed_limit_up_count integer NOT NULL DEFAULT 0,
    broken_limit_up_count integer NOT NULL DEFAULT 0,
    sealed_limit_down_count integer NOT NULL DEFAULT 0,
    max_height integer NOT NULL DEFAULT 0,
    seal_rate numeric(12,6),
    break_rate numeric(12,6),
    relative_change_pct numeric(12,6),
    factor jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (trade_date, membership, industry_code)
);

CREATE TABLE IF NOT EXISTS cynexus_appl_market.trade_review_daily_pool (
    trade_date date NOT NULL,
    pool_type varchar(32) NOT NULL,
    membership varchar(12) NOT NULL DEFAULT 'union',
    industry_code varchar(64) NOT NULL DEFAULT '',
    stocks jsonb NOT NULL DEFAULT '[]'::jsonb,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (trade_date, pool_type, membership, industry_code)
);

CREATE TABLE IF NOT EXISTS cynexus_appl_market.trade_review_trend_phase (
    trade_date date NOT NULL,
    scope varchar(16) NOT NULL,
    membership varchar(12) NOT NULL DEFAULT 'union',
    industry_code varchar(64) NOT NULL DEFAULT '',
    window_days integer NOT NULL,
    phase varchar(24) NOT NULL,
    phase_score numeric(12,6) NOT NULL,
    factor jsonb NOT NULL DEFAULT '{}'::jsonb,
    policy_version varchar(32) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (trade_date, scope, membership, industry_code, window_days, policy_version)
);
