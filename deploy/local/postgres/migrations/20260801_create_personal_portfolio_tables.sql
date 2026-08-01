CREATE SCHEMA IF NOT EXISTS cynexus_appl_market;

CREATE TABLE IF NOT EXISTS cynexus_appl_market.portfolio_import_asset (
    id bigint PRIMARY KEY,
    principal_key text NOT NULL,
    tenant_key text NOT NULL,
    file_name text,
    content_type text NOT NULL,
    byte_size bigint NOT NULL,
    content_sha256 text NOT NULL,
    content_bytes bytea NOT NULL,
    retention_until timestamptz NOT NULL,
    purged_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    CONSTRAINT ck_portfolio_asset_size CHECK (byte_size > 0),
    CONSTRAINT ck_portfolio_asset_content_type CHECK (content_type IN ('image/png', 'image/jpeg', 'image/webp'))
);
CREATE INDEX IF NOT EXISTS idx_portfolio_asset_owner
    ON cynexus_appl_market.portfolio_import_asset (tenant_key, principal_key, created_at DESC)
    WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS cynexus_appl_market.portfolio_import_task (
    id bigint PRIMARY KEY,
    principal_key text NOT NULL,
    tenant_key text NOT NULL,
    asset_id bigint NOT NULL REFERENCES cynexus_appl_market.portfolio_import_asset(id),
    status text NOT NULL,
    version bigint NOT NULL DEFAULT 0,
    attempt_count integer NOT NULL DEFAULT 0,
    idempotency_key text NOT NULL,
    capability text NOT NULL DEFAULT 'portfolio_trade_screenshot_parse',
    prompt_version text,
    provider_code text,
    model_code text,
    queued_at timestamptz,
    started_at timestamptz,
    completed_at timestamptz,
    confirmed_at timestamptz,
    last_error_code text,
    last_error_message text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    CONSTRAINT ck_portfolio_task_status CHECK (status IN (
        'PENDING_DISPATCH', 'QUEUED', 'PROCESSING', 'RETRY_WAITING',
        'READY_FOR_REVIEW', 'CONFIRMED', 'FAILED', 'CANCELLED'
    )),
    CONSTRAINT ck_portfolio_task_version CHECK (version >= 0),
    CONSTRAINT ck_portfolio_task_attempt CHECK (attempt_count >= 0)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_portfolio_task_idempotency_active
    ON cynexus_appl_market.portfolio_import_task (tenant_key, principal_key, idempotency_key)
    WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_portfolio_task_owner_status
    ON cynexus_appl_market.portfolio_import_task (tenant_key, principal_key, status, created_at DESC)
    WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS cynexus_appl_market.portfolio_import_candidate (
    id bigint PRIMARY KEY,
    task_id bigint NOT NULL REFERENCES cynexus_appl_market.portfolio_import_task(id),
    row_no integer NOT NULL,
    raw_payload jsonb NOT NULL DEFAULT '{}'::jsonb,
    normalized_payload jsonb NOT NULL DEFAULT '{}'::jsonb,
    confidence numeric(8,6),
    warnings jsonb NOT NULL DEFAULT '[]'::jsonb,
    review_status text NOT NULL DEFAULT 'PENDING',
    version bigint NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    CONSTRAINT ck_portfolio_candidate_row_no CHECK (row_no > 0),
    CONSTRAINT ck_portfolio_candidate_confidence CHECK (confidence IS NULL OR (confidence >= 0 AND confidence <= 1)),
    CONSTRAINT ck_portfolio_candidate_review_status CHECK (review_status IN ('PENDING', 'ACCEPTED', 'EXCLUDED'))
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_portfolio_candidate_task_row_active
    ON cynexus_appl_market.portfolio_import_candidate (task_id, row_no)
    WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_portfolio_candidate_task
    ON cynexus_appl_market.portfolio_import_candidate (task_id, review_status)
    WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS cynexus_appl_market.portfolio_trade (
    id bigint PRIMARY KEY,
    principal_key text NOT NULL,
    tenant_key text NOT NULL,
    source_task_id bigint NOT NULL REFERENCES cynexus_appl_market.portfolio_import_task(id),
    source_candidate_id bigint NOT NULL REFERENCES cynexus_appl_market.portfolio_import_candidate(id),
    symbol text NOT NULL,
    security_name text,
    side text NOT NULL,
    traded_at timestamptz NOT NULL,
    quantity numeric(24,8) NOT NULL,
    price numeric(24,8) NOT NULL,
    gross_amount numeric(28,8) NOT NULL,
    total_fee numeric(24,8) NOT NULL DEFAULT 0,
    external_trade_no text,
    status text NOT NULL DEFAULT 'ACTIVE',
    replaces_trade_id bigint REFERENCES cynexus_appl_market.portfolio_trade(id),
    confirmed_at timestamptz NOT NULL,
    confirmed_by text NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    CONSTRAINT ck_portfolio_trade_side CHECK (side IN ('BUY', 'SELL')),
    CONSTRAINT ck_portfolio_trade_status CHECK (status IN ('ACTIVE', 'REPLACED', 'VOIDED')),
    CONSTRAINT ck_portfolio_trade_values CHECK (quantity > 0 AND price > 0 AND gross_amount > 0 AND total_fee >= 0),
    CONSTRAINT ck_portfolio_trade_not_self_replace CHECK (replaces_trade_id IS NULL OR replaces_trade_id <> id)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_portfolio_trade_source_active
    ON cynexus_appl_market.portfolio_trade (source_task_id, source_candidate_id)
    WHERE deleted_at IS NULL AND status = 'ACTIVE';
CREATE INDEX IF NOT EXISTS idx_portfolio_trade_owner_symbol_time
    ON cynexus_appl_market.portfolio_trade (tenant_key, principal_key, symbol, traded_at, id)
    WHERE deleted_at IS NULL AND status = 'ACTIVE';

CREATE TABLE IF NOT EXISTS cynexus_appl_market.portfolio_review (
    id bigint PRIMARY KEY,
    principal_key text NOT NULL,
    tenant_key text NOT NULL,
    symbol text NOT NULL,
    period_start date NOT NULL,
    period_end date NOT NULL,
    thesis text,
    plan text,
    execution text,
    lessons text,
    next_action text,
    tags jsonb NOT NULL DEFAULT '[]'::jsonb,
    rating smallint,
    metrics_snapshot jsonb NOT NULL DEFAULT '{}'::jsonb,
    status text NOT NULL DEFAULT 'DRAFT',
    completed_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    CONSTRAINT ck_portfolio_review_period CHECK (period_end >= period_start),
    CONSTRAINT ck_portfolio_review_rating CHECK (rating IS NULL OR (rating BETWEEN 1 AND 5)),
    CONSTRAINT ck_portfolio_review_status CHECK (status IN ('DRAFT', 'COMPLETED'))
);
CREATE INDEX IF NOT EXISTS idx_portfolio_review_owner_status
    ON cynexus_appl_market.portfolio_review (tenant_key, principal_key, status, period_end DESC)
    WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS cynexus_appl_market.portfolio_outbox_event (
    id bigint PRIMARY KEY,
    message_id text NOT NULL,
    aggregate_type text NOT NULL,
    aggregate_id text NOT NULL,
    message_type text NOT NULL,
    event_name text NOT NULL,
    schema_version integer NOT NULL,
    routing_key text NOT NULL,
    payload jsonb NOT NULL,
    headers jsonb NOT NULL DEFAULT '{}'::jsonb,
    status text NOT NULL DEFAULT 'PENDING',
    attempt_count integer NOT NULL DEFAULT 0,
    available_at timestamptz NOT NULL DEFAULT now(),
    claimed_at timestamptz,
    claimed_by text,
    published_at timestamptz,
    last_error_code text,
    last_error_message text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_portfolio_outbox_status CHECK (status IN ('PENDING', 'PUBLISHING', 'PUBLISHED', 'FAILED')),
    CONSTRAINT ck_portfolio_outbox_attempt CHECK (attempt_count >= 0)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_portfolio_outbox_message_id
    ON cynexus_appl_market.portfolio_outbox_event (message_id);
CREATE INDEX IF NOT EXISTS idx_portfolio_outbox_ready
    ON cynexus_appl_market.portfolio_outbox_event (status, available_at, id)
    WHERE status IN ('PENDING', 'PUBLISHING');
