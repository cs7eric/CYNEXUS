\set ON_ERROR_STOP on

CREATE SCHEMA IF NOT EXISTS cynexus_appl_core;

CREATE TABLE IF NOT EXISTS cynexus_appl_core.data_source_health_check_run (
    id bigint NOT NULL PRIMARY KEY,
    run_id varchar(64) NOT NULL,
    trigger_type varchar(32) NOT NULL DEFAULT 'manual',
    status varchar(32) NOT NULL DEFAULT 'running',
    started_at timestamptz NOT NULL,
    finished_at timestamptz,
    duration_ms integer,
    include_slow boolean NOT NULL DEFAULT false,
    total_count integer NOT NULL DEFAULT 0,
    healthy_count integer NOT NULL DEFAULT 0,
    degraded_count integer NOT NULL DEFAULT 0,
    failed_count integer NOT NULL DEFAULT 0,
    skipped_count integer NOT NULL DEFAULT 0,
    success_rate numeric(8, 4),
    avg_latency_ms numeric(12, 2),
    error_message text,
    raw_payload jsonb NOT NULL DEFAULT '{}'::jsonb,
    extra jsonb NOT NULL DEFAULT '{}'::jsonb,
    remark text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_data_source_health_check_run_run_id_alive
    ON cynexus_appl_core.data_source_health_check_run (run_id)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_data_source_health_check_run_started_at
    ON cynexus_appl_core.data_source_health_check_run (started_at DESC)
    WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS cynexus_appl_core.data_source_health_check_item (
    id bigint NOT NULL PRIMARY KEY,
    run_id varchar(64) NOT NULL,
    source_code varchar(128) NOT NULL,
    source_name varchar(256) NOT NULL,
    source_type varchar(64) NOT NULL,
    category varchar(64) NOT NULL,
    provider varchar(64) NOT NULL,
    endpoint text,
    status varchar(32) NOT NULL,
    connectivity boolean NOT NULL DEFAULT false,
    authenticity_status varchar(32) NOT NULL DEFAULT 'unknown',
    latency_ms integer,
    sample_count integer,
    success_rate_24h numeric(8, 4),
    checked_at timestamptz NOT NULL,
    error_message text,
    checks jsonb NOT NULL DEFAULT '{}'::jsonb,
    raw_payload jsonb NOT NULL DEFAULT '{}'::jsonb,
    extra jsonb NOT NULL DEFAULT '{}'::jsonb,
    remark text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_data_source_health_check_item_run_source_alive
    ON cynexus_appl_core.data_source_health_check_item (run_id, source_code)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_data_source_health_check_item_source_checked_at
    ON cynexus_appl_core.data_source_health_check_item (source_code, checked_at DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_data_source_health_check_item_status_checked_at
    ON cynexus_appl_core.data_source_health_check_item (status, checked_at DESC)
    WHERE deleted_at IS NULL;

COMMENT ON TABLE cynexus_appl_core.data_source_health_check_run IS
    'One persisted execution of cynexus-data source health checks, owned by Core settings.';
COMMENT ON TABLE cynexus_appl_core.data_source_health_check_item IS
    'Per-source result rows for a data_source_health_check_run, including connectivity, authenticity, latency, and rolling success rate.';
COMMENT ON COLUMN cynexus_appl_core.data_source_health_check_item.success_rate_24h IS
    'Rolling healthy-rate percentage for this source over persisted checks in the latest 24 hours.';
