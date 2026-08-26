-- Repair v1 summaries whose board stayed sealed for the whole session.
-- v1 discarded delayed capture intervals, but these rows still retain the
-- first and last sealed observations needed for an exact continuous duration.
UPDATE cynexus_appl_market.market_indicator_limit_board_session_summaries
   SET sealed_duration_seconds = GREATEST(
           0,
           ROUND(EXTRACT(EPOCH FROM (last_sealed_at - first_sealed_at)))::integer + 5
       ),
       algorithm_version = 'v2',
       updated_at = now()
 WHERE deleted_at IS NULL
   AND algorithm_version = 'v1'
   AND closing_status = 'sealed'
   AND is_sealed_at_close = TRUE
   AND open_count = 0
   AND reseal_count = 0
   AND first_sealed_at IS NOT NULL
   AND last_sealed_at IS NOT NULL
   AND last_sealed_at >= first_sealed_at;
