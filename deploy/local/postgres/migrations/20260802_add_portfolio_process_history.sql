CREATE TABLE IF NOT EXISTS cynexus_appl_market.portfolio_import_process_history (
    id bigint PRIMARY KEY,
    task_id bigint NOT NULL REFERENCES cynexus_appl_market.portfolio_import_task(id),
    principal_key text NOT NULL,
    tenant_key text NOT NULL,
    attempt_no integer NOT NULL DEFAULT 0,
    event_type text NOT NULL,
    from_status text,
    to_status text,
    message_id text,
    correlation_id text,
    provider_code text,
    model_code text,
    error_code text,
    error_message text,
    details jsonb NOT NULL DEFAULT '{}'::jsonb,
    occurred_at timestamptz NOT NULL DEFAULT now(),
    duration_ms bigint,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_portfolio_process_history_attempt CHECK (attempt_no >= 0),
    CONSTRAINT ck_portfolio_process_history_duration CHECK (duration_ms IS NULL OR duration_ms >= 0)
);
CREATE INDEX IF NOT EXISTS idx_portfolio_process_history_task_time
    ON cynexus_appl_market.portfolio_import_process_history (tenant_key, principal_key, task_id, occurred_at, id);
CREATE INDEX IF NOT EXISTS idx_portfolio_process_history_event
    ON cynexus_appl_market.portfolio_import_process_history (event_type, occurred_at DESC);

INSERT INTO cynexus_appl_market.portfolio_import_process_history
    (id, task_id, principal_key, tenant_key, attempt_no, event_type, to_status, details, occurred_at)
SELECT (task.id * 1000000) + 1, task.id, task.principal_key, task.tenant_key, task.attempt_count,
       'TASK_CREATED', task.status, '{"backfilled":true}'::jsonb, task.created_at
  FROM cynexus_appl_market.portfolio_import_task task
 WHERE NOT EXISTS (
     SELECT 1 FROM cynexus_appl_market.portfolio_import_process_history history
      WHERE history.task_id = task.id AND history.event_type = 'TASK_CREATED'
 )
ON CONFLICT DO NOTHING;
