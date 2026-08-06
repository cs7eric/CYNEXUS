-- The original reconciliation point is 2026-08-03. Keep it stable across
-- repeated Release deployments so performance history is not reset to today.
UPDATE cynexus_appl_market.portfolio_realized_pnl_baseline
SET reset_at = TIMESTAMPTZ '2026-08-03 00:00:00+08',
    updated_at = now()
WHERE principal_key = 'workspace-user'
  AND tenant_key = 'default'
  AND reset_by = 'portfolio-pnl-reset-20260803'
  AND reset_at <> TIMESTAMPTZ '2026-08-03 00:00:00+08';
