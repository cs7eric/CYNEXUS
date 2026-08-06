CREATE INDEX IF NOT EXISTS idx_futures_position_member_rank_variety_history
    ON cynexus_appl_market.futures_position_member_rank (
        exchange_code, scope_type, variety_code, trade_date DESC, rank_type, rank_no
    ) INCLUDE (capture_batch_id, rank_value, rank_change, selected_source)
    WHERE deleted_at IS NULL AND scope_type = 'CONTRACT';

CREATE INDEX IF NOT EXISTS idx_futures_position_capture_batch_selected_exchange_date
    ON cynexus_appl_market.futures_position_capture_batch (exchange_code, trade_date DESC)
    WHERE deleted_at IS NULL AND selected IS TRUE AND status IN ('SUCCEEDED', 'PARTIAL');
