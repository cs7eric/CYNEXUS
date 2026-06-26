\set ON_ERROR_STOP on

BEGIN;

CREATE SCHEMA IF NOT EXISTS app_migration;
CREATE SCHEMA IF NOT EXISTS cynexus_appl_ai;
CREATE SCHEMA IF NOT EXISTS cynexus_appl_market;

CREATE TABLE IF NOT EXISTS app_migration.uuid_bigint_id_map (
    migration_name varchar(128) NOT NULL,
    source_schema varchar(128) NOT NULL,
    table_name varchar(128) NOT NULL,
    old_id uuid NOT NULL,
    new_id bigint NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (migration_name, source_schema, table_name, old_id),
    UNIQUE (migration_name, new_id)
);

DELETE FROM app_migration.uuid_bigint_id_map
WHERE migration_name = '20260626_app_uuid_to_bigint';

-- Existing rows receive stable, table-scoped BIGINT values for migration only.
-- Future rows must be assigned by the application distributed-ID/Snowflake layer.
INSERT INTO app_migration.uuid_bigint_id_map (migration_name, source_schema, table_name, old_id, new_id)
SELECT '20260626_app_uuid_to_bigint', 'app', 'ai_providers', id,
       910001000000000000 + row_number() OVER (ORDER BY created_at, id)
FROM app.ai_providers;

INSERT INTO app_migration.uuid_bigint_id_map (migration_name, source_schema, table_name, old_id, new_id)
SELECT '20260626_app_uuid_to_bigint', 'app', 'ai_usage_bindings', id,
       910002000000000000 + row_number() OVER (ORDER BY created_at, id)
FROM app.ai_usage_bindings;

INSERT INTO app_migration.uuid_bigint_id_map (migration_name, source_schema, table_name, old_id, new_id)
SELECT '20260626_app_uuid_to_bigint', 'app', 'application_analysis_configs', id,
       910003000000000000 + row_number() OVER (ORDER BY created_at, id)
FROM app.application_analysis_configs;

INSERT INTO app_migration.uuid_bigint_id_map (migration_name, source_schema, table_name, old_id, new_id)
SELECT '20260626_app_uuid_to_bigint', 'app', 'application_analysis_daily_snapshots', id,
       910004000000000000 + row_number() OVER (ORDER BY created_at, id)
FROM app.application_analysis_daily_snapshots;

INSERT INTO app_migration.uuid_bigint_id_map (migration_name, source_schema, table_name, old_id, new_id)
SELECT '20260626_app_uuid_to_bigint', 'app', 'application_analysis_result_current', id,
       910005000000000000 + row_number() OVER (ORDER BY created_at, id)
FROM app.application_analysis_result_current;

INSERT INTO app_migration.uuid_bigint_id_map (migration_name, source_schema, table_name, old_id, new_id)
SELECT '20260626_app_uuid_to_bigint', 'app', 'application_analysis_result_history', id,
       910006000000000000 + row_number() OVER (ORDER BY created_at, id)
FROM app.application_analysis_result_history;

INSERT INTO app_migration.uuid_bigint_id_map (migration_name, source_schema, table_name, old_id, new_id)
SELECT '20260626_app_uuid_to_bigint', 'app', 'application_analysis_targets', id,
       910007000000000000 + row_number() OVER (ORDER BY created_at, id)
FROM app.application_analysis_targets;

INSERT INTO app_migration.uuid_bigint_id_map (migration_name, source_schema, table_name, old_id, new_id)
SELECT '20260626_app_uuid_to_bigint', 'app', 'market_limit_daily_snapshots', id,
       910101000000000000 + row_number() OVER (ORDER BY created_at, id)
FROM app.market_limit_daily_snapshots;

INSERT INTO app_migration.uuid_bigint_id_map (migration_name, source_schema, table_name, old_id, new_id)
SELECT '20260626_app_uuid_to_bigint', 'app', 'market_limit_daily_stocks', id,
       910102000000000000 + row_number() OVER (ORDER BY created_at, id)
FROM app.market_limit_daily_stocks;

INSERT INTO app_migration.uuid_bigint_id_map (migration_name, source_schema, table_name, old_id, new_id)
SELECT '20260626_app_uuid_to_bigint', 'app', 'market_overview_snapshots', id,
       910103000000000000 + row_number() OVER (ORDER BY created_at, id)
FROM app.market_overview_snapshots;

INSERT INTO app_migration.uuid_bigint_id_map (migration_name, source_schema, table_name, old_id, new_id)
SELECT '20260626_app_uuid_to_bigint', 'app', 'market_pulse_capture_batches', id,
       910104000000000000 + row_number() OVER (ORDER BY created_at, id)
FROM app.market_pulse_capture_batches;

INSERT INTO app_migration.uuid_bigint_id_map (migration_name, source_schema, table_name, old_id, new_id)
SELECT '20260626_app_uuid_to_bigint', 'app', 'market_pulse_sector_daily_snapshots', id,
       910105000000000000 + row_number() OVER (ORDER BY created_at, id)
FROM app.market_pulse_sector_daily_snapshots;

INSERT INTO app_migration.uuid_bigint_id_map (migration_name, source_schema, table_name, old_id, new_id)
SELECT '20260626_app_uuid_to_bigint', 'app', 'scheduler_job_categories', id,
       910106000000000000 + row_number() OVER (ORDER BY created_at, id)
FROM app.scheduler_job_categories;

INSERT INTO app_migration.uuid_bigint_id_map (migration_name, source_schema, table_name, old_id, new_id)
SELECT '20260626_app_uuid_to_bigint', 'app', 'scheduler_job_category_mappings', id,
       910107000000000000 + row_number() OVER (ORDER BY created_at, id)
FROM app.scheduler_job_category_mappings;

INSERT INTO app_migration.uuid_bigint_id_map (migration_name, source_schema, table_name, old_id, new_id)
SELECT '20260626_app_uuid_to_bigint', 'app', 'scheduler_job_run_history', id,
       910108000000000000 + row_number() OVER (ORDER BY created_at, id)
FROM app.scheduler_job_run_history;

INSERT INTO app_migration.uuid_bigint_id_map (migration_name, source_schema, table_name, old_id, new_id)
SELECT '20260626_app_uuid_to_bigint', 'app', 'scheduler_job_statuses', id,
       910109000000000000 + row_number() OVER (ORDER BY created_at, id)
FROM app.scheduler_job_statuses;

INSERT INTO app_migration.uuid_bigint_id_map (migration_name, source_schema, table_name, old_id, new_id)
SELECT '20260626_app_uuid_to_bigint', 'app', 'scheduler_jobs', id,
       910110000000000000 + row_number() OVER (ORDER BY created_at, id)
FROM app.scheduler_jobs;

INSERT INTO app_migration.uuid_bigint_id_map (migration_name, source_schema, table_name, old_id, new_id)
SELECT '20260626_app_uuid_to_bigint', 'app', 'sector_fund_flow_capture_batches', id,
       910111000000000000 + row_number() OVER (ORDER BY created_at, id)
FROM app.sector_fund_flow_capture_batches;

INSERT INTO app_migration.uuid_bigint_id_map (migration_name, source_schema, table_name, old_id, new_id)
SELECT '20260626_app_uuid_to_bigint', 'app', 'sector_fund_flow_daily_snapshots', id,
       910112000000000000 + row_number() OVER (ORDER BY created_at, id)
FROM app.sector_fund_flow_daily_snapshots;

INSERT INTO app_migration.uuid_bigint_id_map (migration_name, source_schema, table_name, old_id, new_id)
SELECT '20260626_app_uuid_to_bigint', 'app', 'self_selected_list_items', id,
       910113000000000000 + row_number() OVER (ORDER BY created_at, id)
FROM app.self_selected_list_items;

INSERT INTO app_migration.uuid_bigint_id_map (migration_name, source_schema, table_name, old_id, new_id)
SELECT '20260626_app_uuid_to_bigint', 'app', 'self_selected_lists', id,
       910114000000000000 + row_number() OVER (ORDER BY created_at, id)
FROM app.self_selected_lists;

CREATE OR REPLACE FUNCTION app_migration.uuid_to_bigint(p_old_id uuid, p_table_name text)
RETURNS bigint
LANGUAGE plpgsql
STABLE
AS $$
DECLARE
    v_new_id bigint;
BEGIN
    IF p_old_id IS NULL THEN
        RETURN NULL;
    END IF;

    SELECT new_id
      INTO v_new_id
      FROM app_migration.uuid_bigint_id_map
     WHERE migration_name = '20260626_app_uuid_to_bigint'
       AND source_schema = 'app'
       AND table_name = p_table_name
       AND old_id = p_old_id;

    IF v_new_id IS NULL THEN
        RAISE EXCEPTION 'No BIGINT mapping for %.%', p_table_name, p_old_id;
    END IF;

    RETURN v_new_id;
END;
$$;

ALTER TABLE app.ai_providers
    ALTER COLUMN id DROP DEFAULT,
    ALTER COLUMN id TYPE bigint USING app_migration.uuid_to_bigint(id, 'ai_providers');

ALTER TABLE app.ai_usage_bindings
    ALTER COLUMN id DROP DEFAULT,
    ALTER COLUMN id TYPE bigint USING app_migration.uuid_to_bigint(id, 'ai_usage_bindings'),
    ALTER COLUMN provider_id TYPE bigint USING app_migration.uuid_to_bigint(provider_id, 'ai_providers');

ALTER TABLE app.application_analysis_configs
    ALTER COLUMN id DROP DEFAULT,
    ALTER COLUMN id TYPE bigint USING app_migration.uuid_to_bigint(id, 'application_analysis_configs');

ALTER TABLE app.application_analysis_targets
    ALTER COLUMN id DROP DEFAULT,
    ALTER COLUMN id TYPE bigint USING app_migration.uuid_to_bigint(id, 'application_analysis_targets'),
    ALTER COLUMN self_selected_item_id TYPE bigint USING app_migration.uuid_to_bigint(self_selected_item_id, 'self_selected_list_items');

ALTER TABLE app.application_analysis_daily_snapshots
    ALTER COLUMN id DROP DEFAULT,
    ALTER COLUMN id TYPE bigint USING app_migration.uuid_to_bigint(id, 'application_analysis_daily_snapshots'),
    ALTER COLUMN target_id TYPE bigint USING app_migration.uuid_to_bigint(target_id, 'application_analysis_targets');

ALTER TABLE app.application_analysis_result_current
    ALTER COLUMN id DROP DEFAULT,
    ALTER COLUMN id TYPE bigint USING app_migration.uuid_to_bigint(id, 'application_analysis_result_current'),
    ALTER COLUMN target_id TYPE bigint USING app_migration.uuid_to_bigint(target_id, 'application_analysis_targets');

ALTER TABLE app.application_analysis_result_history
    ALTER COLUMN id DROP DEFAULT,
    ALTER COLUMN id TYPE bigint USING app_migration.uuid_to_bigint(id, 'application_analysis_result_history'),
    ALTER COLUMN target_id TYPE bigint USING app_migration.uuid_to_bigint(target_id, 'application_analysis_targets');

ALTER TABLE app.market_limit_daily_snapshots
    ALTER COLUMN id DROP DEFAULT,
    ALTER COLUMN id TYPE bigint USING app_migration.uuid_to_bigint(id, 'market_limit_daily_snapshots');

ALTER TABLE app.market_limit_daily_stocks
    ALTER COLUMN id DROP DEFAULT,
    ALTER COLUMN id TYPE bigint USING app_migration.uuid_to_bigint(id, 'market_limit_daily_stocks');

ALTER TABLE app.market_overview_snapshots
    ALTER COLUMN id DROP DEFAULT,
    ALTER COLUMN id TYPE bigint USING app_migration.uuid_to_bigint(id, 'market_overview_snapshots');

ALTER TABLE app.market_pulse_capture_batches
    ALTER COLUMN id DROP DEFAULT,
    ALTER COLUMN id TYPE bigint USING app_migration.uuid_to_bigint(id, 'market_pulse_capture_batches');

ALTER TABLE app.market_pulse_sector_daily_snapshots
    ALTER COLUMN id DROP DEFAULT,
    ALTER COLUMN id TYPE bigint USING app_migration.uuid_to_bigint(id, 'market_pulse_sector_daily_snapshots'),
    ALTER COLUMN batch_id TYPE bigint USING app_migration.uuid_to_bigint(batch_id, 'market_pulse_capture_batches');

ALTER TABLE app.scheduler_job_categories
    ALTER COLUMN id DROP DEFAULT,
    ALTER COLUMN id TYPE bigint USING app_migration.uuid_to_bigint(id, 'scheduler_job_categories');

ALTER TABLE app.scheduler_jobs
    ALTER COLUMN id DROP DEFAULT,
    ALTER COLUMN id TYPE bigint USING app_migration.uuid_to_bigint(id, 'scheduler_jobs');

ALTER TABLE app.scheduler_job_category_mappings
    ALTER COLUMN id DROP DEFAULT,
    ALTER COLUMN id TYPE bigint USING app_migration.uuid_to_bigint(id, 'scheduler_job_category_mappings'),
    ALTER COLUMN job_id TYPE bigint USING app_migration.uuid_to_bigint(job_id, 'scheduler_jobs'),
    ALTER COLUMN category_id TYPE bigint USING app_migration.uuid_to_bigint(category_id, 'scheduler_job_categories');

ALTER TABLE app.scheduler_job_run_history
    ALTER COLUMN id DROP DEFAULT,
    ALTER COLUMN id TYPE bigint USING app_migration.uuid_to_bigint(id, 'scheduler_job_run_history'),
    ALTER COLUMN job_id TYPE bigint USING app_migration.uuid_to_bigint(job_id, 'scheduler_jobs');

ALTER TABLE app.scheduler_job_statuses
    ALTER COLUMN id DROP DEFAULT,
    ALTER COLUMN id TYPE bigint USING app_migration.uuid_to_bigint(id, 'scheduler_job_statuses'),
    ALTER COLUMN job_id TYPE bigint USING app_migration.uuid_to_bigint(job_id, 'scheduler_jobs');

ALTER TABLE app.sector_fund_flow_capture_batches
    ALTER COLUMN id DROP DEFAULT,
    ALTER COLUMN id TYPE bigint USING app_migration.uuid_to_bigint(id, 'sector_fund_flow_capture_batches');

ALTER TABLE app.sector_fund_flow_daily_snapshots
    ALTER COLUMN id DROP DEFAULT,
    ALTER COLUMN id TYPE bigint USING app_migration.uuid_to_bigint(id, 'sector_fund_flow_daily_snapshots'),
    ALTER COLUMN batch_id TYPE bigint USING app_migration.uuid_to_bigint(batch_id, 'sector_fund_flow_capture_batches');

ALTER TABLE app.self_selected_lists
    ALTER COLUMN id DROP DEFAULT,
    ALTER COLUMN id TYPE bigint USING app_migration.uuid_to_bigint(id, 'self_selected_lists');

ALTER TABLE app.self_selected_list_items
    ALTER COLUMN id DROP DEFAULT,
    ALTER COLUMN id TYPE bigint USING app_migration.uuid_to_bigint(id, 'self_selected_list_items'),
    ALTER COLUMN list_id TYPE bigint USING app_migration.uuid_to_bigint(list_id, 'self_selected_lists');

ALTER TABLE app.ai_providers SET SCHEMA cynexus_appl_ai;
ALTER TABLE app.ai_usage_bindings SET SCHEMA cynexus_appl_ai;
ALTER TABLE app.application_analysis_configs SET SCHEMA cynexus_appl_ai;
ALTER TABLE app.application_analysis_targets SET SCHEMA cynexus_appl_ai;
ALTER TABLE app.application_analysis_daily_snapshots SET SCHEMA cynexus_appl_ai;
ALTER TABLE app.application_analysis_result_current SET SCHEMA cynexus_appl_ai;
ALTER TABLE app.application_analysis_result_history SET SCHEMA cynexus_appl_ai;

ALTER TABLE app.market_limit_daily_snapshots SET SCHEMA cynexus_appl_market;
ALTER TABLE app.market_limit_daily_stocks SET SCHEMA cynexus_appl_market;
ALTER TABLE app.market_overview_snapshots SET SCHEMA cynexus_appl_market;
ALTER TABLE app.market_pulse_capture_batches SET SCHEMA cynexus_appl_market;
ALTER TABLE app.market_pulse_sector_daily_snapshots SET SCHEMA cynexus_appl_market;
ALTER TABLE app.scheduler_job_categories SET SCHEMA cynexus_appl_market;
ALTER TABLE app.scheduler_jobs SET SCHEMA cynexus_appl_market;
ALTER TABLE app.scheduler_job_category_mappings SET SCHEMA cynexus_appl_market;
ALTER TABLE app.scheduler_job_run_history SET SCHEMA cynexus_appl_market;
ALTER TABLE app.scheduler_job_statuses SET SCHEMA cynexus_appl_market;
ALTER TABLE app.sector_fund_flow_capture_batches SET SCHEMA cynexus_appl_market;
ALTER TABLE app.sector_fund_flow_daily_snapshots SET SCHEMA cynexus_appl_market;
ALTER TABLE app.self_selected_lists SET SCHEMA cynexus_appl_market;
ALTER TABLE app.self_selected_list_items SET SCHEMA cynexus_appl_market;

COMMIT;

SELECT table_schema, table_name, column_name, data_type, is_nullable, column_default
FROM information_schema.columns
WHERE table_schema IN ('cynexus_appl_ai', 'cynexus_appl_market')
  AND column_name = 'id'
ORDER BY table_schema, table_name;

SELECT table_schema, table_name, column_name, data_type
FROM information_schema.columns
WHERE table_schema IN ('cynexus_appl_ai', 'cynexus_appl_market')
  AND data_type = 'uuid'
ORDER BY table_schema, table_name, column_name;
