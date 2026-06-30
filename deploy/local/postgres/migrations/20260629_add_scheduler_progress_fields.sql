ALTER TABLE cynexus_appl_core.scheduler_job_statuses
    ADD COLUMN IF NOT EXISTS progress jsonb NOT NULL DEFAULT '{}'::jsonb;

ALTER TABLE cynexus_appl_core.scheduler_job_run_history
    ADD COLUMN IF NOT EXISTS result_summary jsonb NOT NULL DEFAULT '{}'::jsonb;

COMMENT ON COLUMN cynexus_appl_core.scheduler_job_statuses.progress IS
    'Current runtime progress payload for scheduler UI polling. Use stable envelope fields and job-specific payload.';

COMMENT ON COLUMN cynexus_appl_core.scheduler_job_run_history.result_summary IS
    'Final per-run structured result payload for scheduler history/detail rendering.';
