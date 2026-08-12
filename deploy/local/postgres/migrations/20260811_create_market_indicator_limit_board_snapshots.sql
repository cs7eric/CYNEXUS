CREATE SCHEMA IF NOT EXISTS cynexus_appl_market;

CREATE TABLE IF NOT EXISTS cynexus_appl_market.market_indicator_limit_board_snapshots (
    id bigint PRIMARY KEY,
    trade_date date NOT NULL,
    code varchar(6) NOT NULL,
    name text,
    market varchar(8),
    direction varchar(8) NOT NULL,
    board_status varchar(16) NOT NULL,
    is_sealed boolean NOT NULL DEFAULT FALSE,
    last_price numeric(20,6),
    prev_close numeric(20,6),
    change_pct numeric(12,6),
    limit_price numeric(20,6),
    queue_side varchar(8),
    queue_price numeric(20,6),
    queue_volume numeric(24,6),
    sealed_amount numeric(24,6),
    bid_price_1 numeric(20,6),
    bid_volume_1 numeric(24,6),
    ask_price_1 numeric(20,6),
    ask_volume_1 numeric(24,6),
    provider_time_raw integer,
    source text NOT NULL,
    captured_at timestamptz NOT NULL,
    payload jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    CONSTRAINT ck_market_indicator_limit_board_direction
        CHECK (direction IN ('up', 'down')),
    CONSTRAINT ck_market_indicator_limit_board_status
        CHECK (board_status IN ('touch', 'sealed', 'opened', 'resealed')),
    CONSTRAINT ck_market_indicator_limit_board_queue_side
        CHECK (queue_side IS NULL OR queue_side IN ('buy', 'sell'))
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_market_indicator_limit_board_capture
    ON cynexus_appl_market.market_indicator_limit_board_snapshots (trade_date, captured_at, code)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_market_indicator_limit_board_code_time
    ON cynexus_appl_market.market_indicator_limit_board_snapshots (trade_date, code, captured_at DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_market_indicator_limit_board_status_time
    ON cynexus_appl_market.market_indicator_limit_board_snapshots (trade_date, board_status, captured_at DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_market_indicator_limit_board_direction_time
    ON cynexus_appl_market.market_indicator_limit_board_snapshots (trade_date, direction, captured_at DESC)
    WHERE deleted_at IS NULL;
