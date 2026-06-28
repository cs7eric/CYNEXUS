CREATE SCHEMA IF NOT EXISTS cynexus_data;

CREATE TABLE IF NOT EXISTS cynexus_data.task_job_run (
    run_id text PRIMARY KEY,
    job_key text NOT NULL,
    trigger_request_id text,
    triggered_by text,
    dry_run boolean NOT NULL DEFAULT false,
    status text NOT NULL,
    success boolean NOT NULL DEFAULT false,
    started_at timestamptz NOT NULL,
    finished_at timestamptz,
    rows_read bigint NOT NULL DEFAULT 0,
    rows_written bigint NOT NULL DEFAULT 0,
    warnings jsonb NOT NULL DEFAULT '[]'::jsonb,
    errors jsonb NOT NULL DEFAULT '[]'::jsonb,
    params jsonb NOT NULL DEFAULT '{}'::jsonb,
    metadata jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_task_job_run_trigger_request
    ON cynexus_data.task_job_run (job_key, trigger_request_id)
    WHERE trigger_request_id IS NOT NULL;

CREATE TABLE IF NOT EXISTS cynexus_data.task_checkpoint (
    checkpoint_key text PRIMARY KEY,
    job_key text NOT NULL,
    checkpoint_value jsonb NOT NULL DEFAULT '{}'::jsonb,
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS cynexus_data.task_job_lock (
    lock_key text PRIMARY KEY,
    owner text NOT NULL,
    expires_at timestamptz NOT NULL,
    acquired_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS cynexus_data.media_transcript (
    transcript_id text PRIMARY KEY,
    source_uri text NOT NULL,
    transcript text NOT NULL,
    language text,
    duration_seconds numeric(12, 3),
    metadata jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_at timestamptz NOT NULL DEFAULT now()
);
