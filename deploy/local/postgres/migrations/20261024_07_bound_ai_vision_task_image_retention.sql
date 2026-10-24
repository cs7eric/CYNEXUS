\set ON_ERROR_STOP on

BEGIN;

-- Move legacy referenced inputs to the unfinished or retryable leaf before scrubbing ancestors.
WITH RECURSIVE image_chain(task_id, source_id, images_json, retry_of_id, depth) AS (
    SELECT task.id, task.id, task.images_json, task.retry_of_id, 0
      FROM cynexus_appl_ai.ai_vision_task task
     WHERE task.images_json = '[]'::jsonb
       AND (task.task_status IN ('queued', 'running')
            OR (task.task_status = 'failed' AND task.completed_at > now() - interval '7 days'))
       AND NOT EXISTS (SELECT 1 FROM cynexus_appl_ai.ai_vision_task child WHERE child.retry_of_id = task.id)
    UNION ALL
    SELECT image_chain.task_id, source.id, source.images_json, source.retry_of_id, image_chain.depth + 1
      FROM image_chain
      JOIN cynexus_appl_ai.ai_vision_task source ON source.id = image_chain.retry_of_id
     WHERE image_chain.images_json = '[]'::jsonb AND source.id < image_chain.source_id
), resolved_images AS (
    SELECT DISTINCT ON (task_id) task_id, images_json
      FROM image_chain
     WHERE jsonb_array_length(images_json) > 0
     ORDER BY task_id, depth
)
UPDATE cynexus_appl_ai.ai_vision_task task
   SET images_json = resolved_images.images_json
  FROM resolved_images
 WHERE task.id = resolved_images.task_id AND task.images_json = '[]'::jsonb;

UPDATE cynexus_appl_ai.ai_vision_task task
   SET images_json = '[]'::jsonb
 WHERE jsonb_array_length(task.images_json) > 0
   AND (task.task_status = 'succeeded'
        OR (task.task_status = 'failed'
            AND coalesce(task.completed_at, task.updated_at, task.created_at) <= now() - interval '7 days')
        OR EXISTS (SELECT 1 FROM cynexus_appl_ai.ai_vision_task child WHERE child.retry_of_id = task.id));

CREATE INDEX IF NOT EXISTS ix_ai_vision_task_input_retention
    ON cynexus_appl_ai.ai_vision_task (completed_at)
    WHERE task_status IN ('succeeded', 'failed') AND jsonb_array_length(images_json) > 0;

COMMENT ON TABLE cynexus_appl_ai.ai_vision_task IS
    'Asynchronous vision queue. Successful images are cleared immediately; failed images are retryable for seven days and scrubbed by hourly cleanup. Explicit retries transfer inputs to the child.';
COMMENT ON COLUMN cynexus_appl_ai.ai_vision_task.images_json IS
    'Private execution inputs, omitted from list/detail reads. Removed on success, transferred to an explicit retry, or scrubbed within the next hourly cleanup after seven days of failure.';

COMMIT;
