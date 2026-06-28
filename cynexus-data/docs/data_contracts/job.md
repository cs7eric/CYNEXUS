# Job Contracts

`JobRequest` fields:

- `job_key`: registered job key.
- `trigger_request_id`: Java-provided idempotency key, optional.
- `triggered_by`: caller name, optional.
- `dry_run`: when true, no durable writes except optional run tracking.
- `params`: job-specific dictionary.

`JobResult` fields:

- `run_id`, `job_key`, `success`, `status`.
- `started_at`, `finished_at`.
- `rows_read`, `rows_written`.
- `warnings`, `errors`, `metadata`.

