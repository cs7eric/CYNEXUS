CREATE SCHEMA IF NOT EXISTS cynexus_appl_market;

CREATE TABLE IF NOT EXISTS cynexus_appl_market.futures_contract_catalog (
    id bigint PRIMARY KEY,
    exchange_code text NOT NULL,
    variety_code text NOT NULL,
    contract_code text NOT NULL,
    contract_name text,
    product_name text,
    contract_month text,
    status text NOT NULL DEFAULT 'active',
    selected_source text NOT NULL,
    extra jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_futures_contract_catalog_active
    ON cynexus_appl_market.futures_contract_catalog (exchange_code, contract_code) WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS cynexus_appl_market.futures_position_capture_batch (
    id bigint PRIMARY KEY,
    trade_date date NOT NULL,
    exchange_code text NOT NULL,
    provider text NOT NULL,
    status text NOT NULL,
    selected boolean NOT NULL DEFAULT false,
    expected_instrument_count integer NOT NULL DEFAULT 0,
    actual_instrument_count integer NOT NULL DEFAULT 0,
    row_count integer NOT NULL DEFAULT 0,
    retry_count integer NOT NULL DEFAULT 0,
    started_at timestamptz NOT NULL DEFAULT now(),
    completed_at timestamptz,
    warnings jsonb NOT NULL DEFAULT '[]'::jsonb,
    error text,
    extra jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_futures_position_capture_batch_active
    ON cynexus_appl_market.futures_position_capture_batch (trade_date, exchange_code, provider) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_futures_position_capture_batch_selected
    ON cynexus_appl_market.futures_position_capture_batch (trade_date, exchange_code) WHERE deleted_at IS NULL AND selected IS TRUE;
CREATE INDEX IF NOT EXISTS idx_futures_position_capture_batch_date
    ON cynexus_appl_market.futures_position_capture_batch (trade_date, exchange_code, status) WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS cynexus_appl_market.futures_position_member_rank (
    id bigint PRIMARY KEY,
    trade_date date NOT NULL,
    exchange_code text NOT NULL,
    scope_type text NOT NULL,
    instrument_code text NOT NULL,
    variety_code text NOT NULL,
    contract_code text,
    rank_type text NOT NULL,
    rank_no integer NOT NULL,
    member_name text,
    rank_value numeric(24,4) NOT NULL,
    rank_change numeric(24,4),
    selected_source text NOT NULL,
    capture_batch_id bigint NOT NULL REFERENCES cynexus_appl_market.futures_position_capture_batch(id),
    extra jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    CONSTRAINT ck_futures_position_member_scope CHECK (scope_type IN ('CONTRACT', 'VARIETY')),
    CONSTRAINT ck_futures_position_member_rank_type CHECK (rank_type IN ('VOLUME', 'LONG', 'SHORT')),
    CONSTRAINT ck_futures_position_member_rank_no CHECK (rank_no > 0)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_futures_position_member_rank_active
    ON cynexus_appl_market.futures_position_member_rank (trade_date, exchange_code, scope_type, instrument_code, rank_type, rank_no) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_futures_position_member_rank_instrument
    ON cynexus_appl_market.futures_position_member_rank (exchange_code, scope_type, instrument_code, trade_date, rank_type, rank_no) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_futures_position_member_rank_date
    ON cynexus_appl_market.futures_position_member_rank (trade_date, exchange_code) WHERE deleted_at IS NULL;
