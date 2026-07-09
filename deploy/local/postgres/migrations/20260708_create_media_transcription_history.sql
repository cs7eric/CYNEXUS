CREATE TABLE IF NOT EXISTS cynexus_appl_core.media_transcription_history (
    id bigint PRIMARY KEY,
    task_id varchar(128) NOT NULL,
    title varchar(512) NOT NULL,
    status varchar(64) NOT NULL,
    file_name varchar(512),
    record_json jsonb NOT NULL DEFAULT '{}'::jsonb,
    sort_order integer NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_media_transcription_history_task_active
    ON cynexus_appl_core.media_transcription_history (task_id)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS ix_media_transcription_history_active_order
    ON cynexus_appl_core.media_transcription_history (sort_order DESC, created_at DESC)
    WHERE deleted_at IS NULL;
