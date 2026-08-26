ALTER TABLE cynexus_appl_market.market_indicator_limit_board_session_summaries
    ADD COLUMN IF NOT EXISTS last_turnover_amount numeric(24,6),
    ADD COLUMN IF NOT EXISTS last_sealed_turnover_ratio numeric(12,6),
    ADD COLUMN IF NOT EXISTS max_sealed_turnover_ratio numeric(12,6);

UPDATE cynexus_appl_market.market_indicator_limit_board_session_summaries summary
   SET last_turnover_amount = latest.amount,
       last_sealed_turnover_ratio = CASE
           WHEN latest.amount > 0 AND summary.last_sealed_amount IS NOT NULL
               THEN summary.last_sealed_amount / latest.amount
       END,
       max_sealed_turnover_ratio = CASE
           WHEN latest.amount > 0 AND summary.max_sealed_amount IS NOT NULL
               THEN summary.max_sealed_amount / latest.amount
       END,
       updated_at = now()
  FROM (
        SELECT s.id, snap.amount
          FROM cynexus_appl_market.market_indicator_limit_board_session_summaries s
          JOIN LATERAL (
                SELECT amount
                  FROM cynexus_appl_market.market_indicator_snapshots snap
                 WHERE snap.deleted_at IS NULL
                   AND snap.trade_date = s.trade_date
                   AND snap.code = s.code
                   AND snap.amount IS NOT NULL
                   AND snap.amount > 0
                 ORDER BY snap.captured_at DESC
                 LIMIT 1
          ) snap ON TRUE
         WHERE s.deleted_at IS NULL
           AND s.last_turnover_amount IS NULL
       ) latest
 WHERE summary.id = latest.id;
