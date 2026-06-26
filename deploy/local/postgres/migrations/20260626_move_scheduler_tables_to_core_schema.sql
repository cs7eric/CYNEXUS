\set ON_ERROR_STOP on

BEGIN;

CREATE SCHEMA IF NOT EXISTS cynexus_appl_core;

ALTER TABLE cynexus_appl_market.scheduler_job_categories SET SCHEMA cynexus_appl_core;
ALTER TABLE cynexus_appl_market.scheduler_jobs SET SCHEMA cynexus_appl_core;
ALTER TABLE cynexus_appl_market.scheduler_job_category_mappings SET SCHEMA cynexus_appl_core;
ALTER TABLE cynexus_appl_market.scheduler_job_run_history SET SCHEMA cynexus_appl_core;
ALTER TABLE cynexus_appl_market.scheduler_job_statuses SET SCHEMA cynexus_appl_core;

COMMIT;

SELECT table_schema, table_name
FROM information_schema.tables
WHERE table_schema IN ('cynexus_appl_core', 'cynexus_appl_market')
  AND table_name LIKE 'scheduler_%'
ORDER BY table_schema, table_name;
