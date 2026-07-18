ALTER TABLE cynexus_appl_core.scheduler_job_statuses
    DROP CONSTRAINT IF EXISTS ck_scheduler_job_statuses_last_run_status;

ALTER TABLE cynexus_appl_core.scheduler_job_statuses
    ADD CONSTRAINT ck_scheduler_job_statuses_last_run_status
    CHECK (
        last_run_status IS NULL
        OR last_run_status IN ('pending', 'running', 'success', 'failed', 'skipped', 'partial')
    );

ALTER TABLE cynexus_appl_core.scheduler_job_run_history
    DROP CONSTRAINT IF EXISTS ck_scheduler_job_run_history_status;

ALTER TABLE cynexus_appl_core.scheduler_job_run_history
    ADD CONSTRAINT ck_scheduler_job_run_history_status
    CHECK (status IN ('success', 'failed', 'skipped', 'running', 'processing', 'partial'));
