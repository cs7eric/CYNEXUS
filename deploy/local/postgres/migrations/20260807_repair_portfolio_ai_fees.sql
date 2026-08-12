\set ON_ERROR_STOP on

BEGIN;

-- Earlier vision runs could copy gross_amount into total_fee. Repair only the
-- exact column-copy signature; other user-entered fees remain untouched.
UPDATE cynexus_appl_market.portfolio_trade
   SET total_fee = CASE
       WHEN quantity * price < 10000 THEN CASE WHEN instrument_type = 'ETF' THEN 0.1 ELSE 0.5 END
       ELSE round(quantity * price * 0.854 / 10000, 8)
   END,
       updated_at = now()
 WHERE total_fee = gross_amount
   AND gross_amount > 0
   AND deleted_at IS NULL;

UPDATE cynexus_appl_market.portfolio_import_candidate candidate
   SET normalized_payload = candidate.normalized_payload || jsonb_build_object(
       'fee', CASE
           WHEN (candidate.normalized_payload->>'quantity') ~ '^[0-9]+(\\.[0-9]+)?$'
            AND (candidate.normalized_payload->>'price') ~ '^[0-9]+(\\.[0-9]+)?$'
            AND (candidate.normalized_payload->>'fee') ~ '^[0-9]+(\\.[0-9]+)?$'
            AND (candidate.normalized_payload->>'fee')::numeric =
                (candidate.normalized_payload->>'quantity')::numeric
                * (candidate.normalized_payload->>'price')::numeric
           THEN to_jsonb(CASE
               WHEN (candidate.normalized_payload->>'quantity')::numeric
                    * (candidate.normalized_payload->>'price')::numeric < 10000
               THEN CASE WHEN candidate.normalized_payload->>'instrumentType' = 'ETF' THEN '0.1' ELSE '0.5' END
               ELSE round(((candidate.normalized_payload->>'quantity')::numeric
                    * (candidate.normalized_payload->>'price')::numeric * 0.854 / 10000), 8)::text
           END)
           ELSE candidate.normalized_payload->'fee'
       END)
 WHERE candidate.review_status = 'PENDING';

COMMIT;
