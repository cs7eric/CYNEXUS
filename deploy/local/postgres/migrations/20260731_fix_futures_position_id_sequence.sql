CREATE SCHEMA IF NOT EXISTS cynexus_appl_market;

CREATE SEQUENCE IF NOT EXISTS cynexus_appl_market.futures_position_id_seq;

DO $$
DECLARE
    v_next_id bigint;
BEGIN
    SELECT GREATEST(
        COALESCE((SELECT max(id) FROM cynexus_appl_market.futures_position_capture_batch), 0),
        COALESCE((SELECT max(id) FROM cynexus_appl_market.futures_position_member_rank), 0)
    ) + 1
      INTO v_next_id;

    PERFORM setval('cynexus_appl_market.futures_position_id_seq', v_next_id, false);
END $$;
