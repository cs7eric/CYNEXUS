CREATE TABLE IF NOT EXISTS cynexus_appl_market.timeshare_archive_batch (
    id bigint PRIMARY KEY,
    trade_date date NOT NULL,
    status varchar(16) NOT NULL,
    selection_version varchar(32) NOT NULL,
    config_snapshot jsonb NOT NULL DEFAULT '{}'::jsonb,
    planned_target_count integer NOT NULL DEFAULT 0,
    ready_target_count integer NOT NULL DEFAULT 0,
    partial_target_count integer NOT NULL DEFAULT 0,
    failed_target_count integer NOT NULL DEFAULT 0,
    cache_published_count integer NOT NULL DEFAULT 0,
    started_at timestamptz NOT NULL DEFAULT now(),
    completed_at timestamptz,
    error_summary jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_timeshare_archive_batch_date_version
    ON cynexus_appl_market.timeshare_archive_batch (trade_date, selection_version);

CREATE TABLE IF NOT EXISTS cynexus_appl_market.timeshare_archive_target (
    id bigint PRIMARY KEY,
    batch_id bigint NOT NULL REFERENCES cynexus_appl_market.timeshare_archive_batch(id),
    trade_date date NOT NULL,
    target_type varchar(24) NOT NULL,
    market varchar(4) NOT NULL,
    symbol varchar(32) NOT NULL,
    canonical_symbol varchar(32) NOT NULL,
    name varchar(128),
    priority integer NOT NULL DEFAULT 100,
    status varchar(16) NOT NULL DEFAULT 'PLANNED',
    point_count integer NOT NULL DEFAULT 0,
    expected_point_count integer NOT NULL DEFAULT 240,
    completeness_pct numeric(6,2),
    prev_close numeric(20,6),
    close_price numeric(20,6),
    change_pct numeric(12,6),
    volume_unit_multiplier integer NOT NULL DEFAULT 100,
    price_source varchar(128),
    amount_source varchar(128),
    avg_price_source varchar(128),
    cache_status varchar(16),
    attempt_count integer NOT NULL DEFAULT 0,
    last_error_code varchar(64),
    last_error_message text,
    fetched_at timestamptz,
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_timeshare_archive_target_key
    ON cynexus_appl_market.timeshare_archive_target (batch_id, target_type, market, canonical_symbol);

CREATE TABLE IF NOT EXISTS cynexus_appl_market.timeshare_archive_reason (
    id bigint PRIMARY KEY,
    target_id bigint NOT NULL REFERENCES cynexus_appl_market.timeshare_archive_target(id),
    reason_type varchar(32) NOT NULL,
    industry_code varchar(64),
    industry_name varchar(128),
    rank_direction varchar(16),
    rank_position integer,
    change_pct numeric(12,6),
    source_record_date date NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_timeshare_archive_target_date_status
    ON cynexus_appl_market.timeshare_archive_target (trade_date, status);
