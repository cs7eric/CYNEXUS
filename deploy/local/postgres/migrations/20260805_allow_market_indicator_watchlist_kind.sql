DO $$
BEGIN
    ALTER TABLE cynexus_appl_market.self_selected_lists
        DROP CONSTRAINT IF EXISTS ck_self_selected_lists_list_kind;

    ALTER TABLE cynexus_appl_market.self_selected_lists
        ADD CONSTRAINT ck_self_selected_lists_list_kind
        CHECK (list_kind IN ('manual', 'system', 'market_indicator_review'));
END $$;
