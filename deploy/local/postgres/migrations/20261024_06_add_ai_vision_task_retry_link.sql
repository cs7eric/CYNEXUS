\set ON_ERROR_STOP on

ALTER TABLE cynexus_appl_ai.ai_vision_task
    ADD COLUMN IF NOT EXISTS retry_of_id BIGINT REFERENCES cynexus_appl_ai.ai_vision_task(id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_ai_vision_task_retry_of
    ON cynexus_appl_ai.ai_vision_task (retry_of_id)
    WHERE retry_of_id IS NOT NULL;

COMMENT ON COLUMN cynexus_appl_ai.ai_vision_task.retry_of_id IS
    'Source task for an explicit user retry; each task can be retried at most once, with retries forming a chain.';
