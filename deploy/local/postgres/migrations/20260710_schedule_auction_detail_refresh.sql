DO $$
DECLARE
    v_item record;
    v_job_id bigint;
    v_status_id bigint;
    v_schedule jsonb;
    v_params jsonb;
BEGIN
    FOR v_item IN
        SELECT *
          FROM (VALUES
              (
                  'auction_opening_refresh',
                  '集合竞价早盘同步',
                  '交易日 09:26 同步早盘集合竞价明细到 ClickHouse。',
                  '09:26',
                  'opening',
                  'scheduled opening auction detail sync'
              ),
              (
                  'auction_closing_refresh',
                  '集合竞价尾盘同步',
                  '交易日 15:02 同步尾盘集合竞价明细到 ClickHouse。',
                  '15:02',
                  'closing',
                  'scheduled closing auction detail sync'
              )
          ) AS t(code, name, description, run_time, auction_phase, message)
    LOOP
        v_schedule := jsonb_build_object(
            'run_time', v_item.run_time,
            'workday_only', true,
            'trading_day_only', true,
            'run_once_per_day', true
        );
        v_params := jsonb_build_object(
            'datasets', jsonb_build_array('auction'),
            'auction_phase', v_item.auction_phase,
            'create_table', true,
            'ignore_errors', true
        );

        SELECT id INTO v_job_id
          FROM cynexus_appl_core.scheduler_jobs
         WHERE code = v_item.code
           AND deleted_at IS NULL
         LIMIT 1;

        IF v_job_id IS NULL THEN
            SELECT COALESCE(MAX(id) + 1, 910110000000000091)
              INTO v_job_id
              FROM cynexus_appl_core.scheduler_jobs;
            INSERT INTO cynexus_appl_core.scheduler_jobs (
                id, code, name, description, service_module, service_class, config_file,
                is_enabled, registered_at, extra, remark, created_at, updated_at
            ) VALUES (
                v_job_id,
                v_item.code,
                v_item.name,
                v_item.description,
                'cynexus-data',
                'sync_tdx_intraday_detail',
                'jobs.yaml',
                TRUE,
                now(),
                jsonb_build_object(
                    'job_name', v_item.code,
                    'data_job_key', 'sync_tdx_intraday_detail',
                    'description', v_item.description,
                    'schedule', v_schedule,
                    'default_params', v_params
                ),
                'migration:20260710_schedule_auction_detail_refresh',
                now(),
                now()
            );
        ELSE
            UPDATE cynexus_appl_core.scheduler_jobs
               SET name = v_item.name,
                   description = v_item.description,
                   service_module = 'cynexus-data',
                   service_class = 'sync_tdx_intraday_detail',
                   config_file = 'jobs.yaml',
                   is_enabled = TRUE,
                   extra = COALESCE(extra, '{}'::jsonb)
                       || jsonb_build_object(
                           'job_name', v_item.code,
                           'data_job_key', 'sync_tdx_intraday_detail',
                           'description', v_item.description,
                           'schedule', v_schedule,
                           'default_params', v_params
                       ),
                   remark = 'migration:20260710_schedule_auction_detail_refresh',
                   updated_at = now()
             WHERE id = v_job_id;
        END IF;

        SELECT id INTO v_status_id
          FROM cynexus_appl_core.scheduler_job_statuses
         WHERE job_id = v_job_id
           AND deleted_at IS NULL
         LIMIT 1;

        IF v_status_id IS NULL THEN
            SELECT COALESCE(MAX(id) + 1, 910109000000000191)
              INTO v_status_id
              FROM cynexus_appl_core.scheduler_job_statuses;
            INSERT INTO cynexus_appl_core.scheduler_job_statuses (
                id, job_id, is_enabled, is_running, schedule, timezone_offset_hours, tick_seconds,
                extra, progress, remark, created_at, updated_at
            ) VALUES (
                v_status_id,
                v_job_id,
                TRUE,
                FALSE,
                v_schedule,
                8,
                60,
                jsonb_build_object(
                    'job_name', v_item.code,
                    'lastDataJobKey', 'sync_tdx_intraday_detail',
                    'lastMessage', v_item.message
                ),
                '{}'::jsonb,
                'migration:20260710_schedule_auction_detail_refresh',
                now(),
                now()
            );
        ELSE
            UPDATE cynexus_appl_core.scheduler_job_statuses
               SET is_enabled = TRUE,
                   is_running = FALSE,
                   schedule = v_schedule,
                   timezone_offset_hours = 8,
                   tick_seconds = 60,
                   extra = COALESCE(extra, '{}'::jsonb)
                       || jsonb_build_object(
                           'job_name', v_item.code,
                           'lastDataJobKey', 'sync_tdx_intraday_detail',
                           'lastMessage', v_item.message
                       ),
                   remark = 'migration:20260710_schedule_auction_detail_refresh',
                   updated_at = now()
             WHERE id = v_status_id;
        END IF;
    END LOOP;
END $$;
