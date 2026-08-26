ALTER TABLE cynexus_appl_market.stock_selection_strategy_definition
    ADD COLUMN IF NOT EXISTS tenant_id varchar(128),
    ADD COLUMN IF NOT EXISTS owner_id varchar(128),
    ADD COLUMN IF NOT EXISTS version bigint NOT NULL DEFAULT 0;

ALTER TABLE cynexus_appl_market.stock_selection_strategy_definition
    DROP CONSTRAINT IF EXISTS ck_stock_selection_strategy_definition_type;
ALTER TABLE cynexus_appl_market.stock_selection_strategy_definition
    ADD CONSTRAINT ck_stock_selection_strategy_definition_type
    CHECK (strategy_type IN ('SCREENER_PRESET', 'USER_SCREENER'));
ALTER TABLE cynexus_appl_market.stock_selection_strategy_definition
    DROP CONSTRAINT IF EXISTS ck_stock_selection_strategy_definition_scope;
ALTER TABLE cynexus_appl_market.stock_selection_strategy_definition
    ADD CONSTRAINT ck_stock_selection_strategy_definition_scope CHECK (
        (strategy_type = 'SCREENER_PRESET' AND tenant_id IS NULL AND owner_id IS NULL)
        OR (strategy_type = 'USER_SCREENER' AND tenant_id IS NOT NULL AND owner_id IS NOT NULL)
    );

CREATE INDEX IF NOT EXISTS idx_stock_selection_strategy_definition_owner_active
    ON cynexus_appl_market.stock_selection_strategy_definition (tenant_id, owner_id, updated_at DESC)
    WHERE deleted_at IS NULL AND status = 'ACTIVE' AND strategy_type = 'USER_SCREENER';
