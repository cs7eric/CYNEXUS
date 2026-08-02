CREATE TABLE IF NOT EXISTS cynexus_appl_market.portfolio_realized_pnl_baseline (
    id bigint PRIMARY KEY,
    principal_key text NOT NULL,
    tenant_key text NOT NULL,
    symbol text NOT NULL,
    realized_pnl numeric(28, 8) NOT NULL,
    reset_at timestamptz NOT NULL DEFAULT now(),
    reset_by text NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    CONSTRAINT uk_portfolio_realized_pnl_baseline_owner_symbol UNIQUE (principal_key, tenant_key, symbol)
);

CREATE INDEX IF NOT EXISTS idx_portfolio_realized_pnl_baseline_owner
    ON cynexus_appl_market.portfolio_realized_pnl_baseline (tenant_key, principal_key)
    WHERE deleted_at IS NULL;

-- Reconciliation point: keep the imported trade history, but start the user's
-- realized-P&L tracking from zero. Future realized P&L is calculated normally.
INSERT INTO cynexus_appl_market.portfolio_realized_pnl_baseline
    (id, principal_key, tenant_key, symbol, realized_pnl, reset_by)
VALUES
    (1785686000001, 'workspace-user', 'default', '159155', 5.50000000, 'portfolio-pnl-reset-20260803'),
    (1785686000002, 'workspace-user', 'default', '159629', -117.50000000, 'portfolio-pnl-reset-20260803'),
    (1785686000003, 'workspace-user', 'default', '159732', -271.50000000, 'portfolio-pnl-reset-20260803'),
    (1785686000004, 'workspace-user', 'default', '159796', -788.51926600, 'portfolio-pnl-reset-20260803'),
    (1785686000005, 'workspace-user', 'default', '588060', 0.00000000, 'portfolio-pnl-reset-20260803'),
    (1785686000006, 'workspace-user', 'default', '588170', 0.00000000, 'portfolio-pnl-reset-20260803')
ON CONFLICT (principal_key, tenant_key, symbol) DO UPDATE
    SET realized_pnl = EXCLUDED.realized_pnl,
        reset_at = now(),
        reset_by = EXCLUDED.reset_by,
        updated_at = now(),
        deleted_at = NULL;
