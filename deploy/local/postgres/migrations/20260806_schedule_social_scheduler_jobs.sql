\set ON_ERROR_STOP on

DO $$
DECLARE
    v_platform text;
    v_category_code text;
    v_category_label text;
    v_job_code text;
    v_job_name text;
    v_data_job_key text;
    v_category_id bigint;
    v_job_id bigint;
    v_status_id bigint;
    v_mapping_id bigint;
    v_schedule jsonb := '{"run_time":"02:00","workday_only":false,"trading_day_only":false,"run_once_per_day":true}'::jsonb;
    v_params jsonb;
BEGIN
    FOR v_platform, v_category_code, v_category_label, v_job_code, v_job_name, v_data_job_key IN
        SELECT * FROM (VALUES
            ('xiaohongshu', 'social_xiaohongshu', '小红书采集', 'social_xiaohongshu_collection', '小红书帖子与评论采集', 'social_xiaohongshu_collection'),
            ('douyin', 'social_douyin', '抖音采集', 'social_douyin_collection', '抖音帖子与评论采集', 'social_douyin_collection')
        ) AS definitions(platform, category_code, category_label, job_code, job_name, data_job_key)
    LOOP
        SELECT id INTO v_category_id
          FROM cynexus_appl_core.scheduler_job_categories
         WHERE code = v_category_code AND deleted_at IS NULL
         ORDER BY id LIMIT 1;

        IF v_category_id IS NULL THEN
            SELECT COALESCE(MAX(id) + 1, 910106000000000200)
              INTO v_category_id
              FROM cynexus_appl_core.scheduler_job_categories;
            INSERT INTO cynexus_appl_core.scheduler_job_categories (
                id, code, label, icon_hint, sort_order, description, status,
                remark, created_at, updated_at
            ) VALUES (
                v_category_id, v_category_code, v_category_label, 'database',
                CASE WHEN v_platform = 'xiaohongshu' THEN 120 ELSE 130 END,
                v_category_label || '的帖子和评论采集任务。', 'active',
                'migration:20260806_schedule_social_scheduler_jobs', now(), now()
            );
        ELSE
            UPDATE cynexus_appl_core.scheduler_job_categories
               SET label = v_category_label,
                   icon_hint = 'database',
                   sort_order = CASE WHEN v_platform = 'xiaohongshu' THEN 120 ELSE 130 END,
                   description = v_category_label || '的帖子和评论采集任务。',
                   status = 'active',
                   deleted_at = NULL,
                   remark = 'migration:20260806_schedule_social_scheduler_jobs',
                   updated_at = now()
             WHERE id = v_category_id;
        END IF;

        v_params := jsonb_build_object(
            'platform', v_platform,
            'scope_key', 'default',
            'profile_name', 'default',
            'create_table', true,
            'collection_purpose', 'market_sentiment'
        );

        SELECT id INTO v_job_id
          FROM cynexus_appl_core.scheduler_jobs
         WHERE code = v_job_code AND deleted_at IS NULL
         ORDER BY id LIMIT 1;

        IF v_job_id IS NULL THEN
            SELECT COALESCE(MAX(id) + 1, 910110000000001000)
              INTO v_job_id
              FROM cynexus_appl_core.scheduler_jobs;
            INSERT INTO cynexus_appl_core.scheduler_jobs (
                id, code, name, description, service_module, service_class, config_file,
                is_enabled, registered_at, extra, remark, created_at, updated_at
            ) VALUES (
                v_job_id, v_job_code, v_job_name,
                v_job_name || '：每日发现当天帖子并完整采集可见评论，帖子和评论在同一次 Job 中完成。',
                'cynexus-data', v_data_job_key, 'jobs.yaml', TRUE, now(),
                jsonb_build_object(
                    'job_name', v_job_code,
                    'data_job_key', v_data_job_key,
                    'description', v_job_name || '：每日发现当天帖子并完整采集可见评论，帖子和评论在同一次 Job 中完成。',
                    'schedule', v_schedule,
                    'default_params', v_params
                ),
                'migration:20260806_schedule_social_scheduler_jobs', now(), now()
            );
        ELSE
            UPDATE cynexus_appl_core.scheduler_jobs
               SET name = v_job_name,
                   description = v_job_name || '：每日发现当天帖子并完整采集可见评论，帖子和评论在同一次 Job 中完成。',
                   service_module = 'cynexus-data',
                   service_class = v_data_job_key,
                   config_file = 'jobs.yaml',
                   is_enabled = TRUE,
                   extra = jsonb_build_object(
                       'job_name', v_job_code,
                       'data_job_key', v_data_job_key,
                       'description', v_job_name || '：每日发现当天帖子并完整采集可见评论，帖子和评论在同一次 Job 中完成。',
                       'schedule', v_schedule,
                       'default_params', v_params
                   ),
                   remark = 'migration:20260806_schedule_social_scheduler_jobs',
                   updated_at = now()
             WHERE id = v_job_id;
        END IF;

        SELECT id INTO v_status_id
          FROM cynexus_appl_core.scheduler_job_statuses
         WHERE job_id = v_job_id AND deleted_at IS NULL
         ORDER BY id LIMIT 1;

        IF v_status_id IS NULL THEN
            SELECT COALESCE(MAX(id) + 1, 910109000000001000)
              INTO v_status_id
              FROM cynexus_appl_core.scheduler_job_statuses;
            INSERT INTO cynexus_appl_core.scheduler_job_statuses (
                id, job_id, is_enabled, is_running, schedule, timezone_offset_hours,
                tick_seconds, extra, progress, remark, created_at, updated_at
            ) VALUES (
                v_status_id, v_job_id, TRUE, FALSE, v_schedule, 8, 60,
                jsonb_build_object('job_name', v_job_code, 'lastDataJobKey', v_data_job_key),
                '{}'::jsonb, 'migration:20260806_schedule_social_scheduler_jobs', now(), now()
            );
        ELSE
            UPDATE cynexus_appl_core.scheduler_job_statuses
               SET is_enabled = TRUE,
                   is_running = FALSE,
                   schedule = v_schedule,
                   timezone_offset_hours = 8,
                   tick_seconds = 60,
                   extra = COALESCE(extra, '{}'::jsonb)
                       || jsonb_build_object('job_name', v_job_code, 'lastDataJobKey', v_data_job_key),
                   remark = 'migration:20260806_schedule_social_scheduler_jobs',
                   updated_at = now()
             WHERE id = v_status_id;
        END IF;

        SELECT id INTO v_mapping_id
          FROM cynexus_appl_core.scheduler_job_category_mappings
         WHERE job_id = v_job_id AND category_id = v_category_id
         ORDER BY deleted_at NULLS FIRST, updated_at DESC, id DESC LIMIT 1;

        IF v_mapping_id IS NULL THEN
            SELECT COALESCE(MAX(id) + 1, 910107000000001000)
              INTO v_mapping_id
              FROM cynexus_appl_core.scheduler_job_category_mappings;
            INSERT INTO cynexus_appl_core.scheduler_job_category_mappings (
                id, job_id, category_id, remark, sort_order, created_at, updated_at
            ) VALUES (
                v_mapping_id, v_job_id, v_category_id,
                'migration:20260806_schedule_social_scheduler_jobs', 10, now(), now()
            );
        ELSE
            UPDATE cynexus_appl_core.scheduler_job_category_mappings
               SET deleted_at = NULL,
                   remark = 'migration:20260806_schedule_social_scheduler_jobs',
                   sort_order = 10,
                   updated_at = now()
             WHERE id = v_mapping_id;
        END IF;
    END LOOP;
END $$;
