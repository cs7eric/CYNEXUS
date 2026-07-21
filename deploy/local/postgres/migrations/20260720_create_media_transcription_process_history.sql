CREATE TABLE IF NOT EXISTS cynexus_appl_core.media_transcription_process_history (
    id BIGINT PRIMARY KEY,
    owner_key VARCHAR(128) NOT NULL DEFAULT 'default',
    idempotency_key VARCHAR(255) NOT NULL,
    source_url TEXT NOT NULL,
    download_url TEXT,
    title VARCHAR(500),
    platform VARCHAR(128),
    note_type VARCHAR(128),
    worker_task_id VARCHAR(128),
    history_id BIGINT,
    status VARCHAR(32) NOT NULL DEFAULT 'queued',
    phase VARCHAR(32) NOT NULL DEFAULT 'queued',
    progress SMALLINT NOT NULL DEFAULT 0,
    file_name VARCHAR(500),
    transcript TEXT,
    polished TEXT,
    summary TEXT,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    error_message TEXT,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_media_transcription_process_owner_idempotency
    ON cynexus_appl_core.media_transcription_process_history (owner_key, idempotency_key)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS ix_media_transcription_process_status_created
    ON cynexus_appl_core.media_transcription_process_history (status, created_at, id);

CREATE INDEX IF NOT EXISTS ix_media_transcription_process_owner_created
    ON cynexus_appl_core.media_transcription_process_history (owner_key, created_at DESC, id DESC)
    WHERE deleted_at IS NULL;

COMMENT ON TABLE cynexus_appl_core.media_transcription_process_history IS
    '快捷指令视频链接自动下载、转写、AI加工和 MP4 History 入库过程记录';

ALTER TABLE cynexus_appl_core.media_transcription_process_history
    ADD COLUMN IF NOT EXISTS download_url TEXT;
