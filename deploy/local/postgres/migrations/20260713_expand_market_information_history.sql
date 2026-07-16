DO $$
DECLARE
    v_job_id bigint;
    v_extra jsonb;
    v_params jsonb;
BEGIN
    SELECT id, extra INTO v_job_id, v_extra
      FROM cynexus_appl_core.scheduler_jobs
     WHERE code = 'a_stock_marketwide_daily' AND deleted_at IS NULL
     LIMIT 1;

    IF v_job_id IS NOT NULL THEN
        v_params := COALESCE(v_extra -> 'default_params', '{}'::jsonb)
            || jsonb_build_object(
                'eastmoney_news_page_size', 300,
                'official_page_size', 40,
                'cailian_page_size', 50,
                'cninfo_market_page_size', 300,
                'cninfo_regulatory_page_size', 200
            );

        UPDATE cynexus_appl_core.scheduler_jobs
           SET extra = COALESCE(v_extra, '{}'::jsonb)
                   || jsonb_build_object('default_params', v_params),
               remark = 'migration:20260713_expand_market_information_history',
               updated_at = now()
         WHERE id = v_job_id;
    END IF;
END $$;
