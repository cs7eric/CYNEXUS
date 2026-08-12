CREATE TABLE IF NOT EXISTS cynexus_appl_market.portfolio_review_anchor (
    id bigint PRIMARY KEY,
    principal_key varchar(128) NOT NULL,
    tenant_key varchar(128) NOT NULL,
    symbol varchar(32) NOT NULL,
    instrument_type varchar(16) NOT NULL,
    trade_id bigint REFERENCES cynexus_appl_market.portfolio_trade(id),
    review_id bigint REFERENCES cynexus_appl_market.portfolio_review(id),
    anchor_type varchar(16) NOT NULL,
    side varchar(16) NOT NULL,
    occurred_at timestamptz NOT NULL,
    price numeric(20, 6) NOT NULL,
    note text,
    tags jsonb NOT NULL DEFAULT '[]'::jsonb,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    CONSTRAINT ck_portfolio_review_anchor_instrument CHECK (instrument_type IN ('STOCK', 'ETF')),
    CONSTRAINT ck_portfolio_review_anchor_type CHECK (anchor_type IN ('TRADE', 'OBSERVATION')),
    CONSTRAINT ck_portfolio_review_anchor_side CHECK (side IN ('BUY', 'SELL', 'OBSERVATION', 'EVENT')),
    CONSTRAINT ck_portfolio_review_anchor_trade CHECK ((anchor_type = 'TRADE' AND trade_id IS NOT NULL) OR anchor_type = 'OBSERVATION')
);

CREATE INDEX IF NOT EXISTS idx_portfolio_review_anchor_owner_symbol_time
    ON cynexus_appl_market.portfolio_review_anchor (tenant_key, principal_key, symbol, occurred_at)
    WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_portfolio_review_anchor_trade_active
    ON cynexus_appl_market.portfolio_review_anchor (trade_id)
    WHERE deleted_at IS NULL AND trade_id IS NOT NULL;
