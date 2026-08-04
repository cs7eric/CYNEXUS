ALTER TABLE cynexus_appl_market.portfolio_account
    ADD COLUMN IF NOT EXISTS total_assets numeric(24,8);

COMMENT ON COLUMN cynexus_appl_market.portfolio_account.total_assets IS
    'User-maintained total account assets; available cash is derived as total_assets minus current holdings market value.';
