# Job Executor Architecture

`cynexus-data` exposes Python data jobs to Java through a small internal API and CLI entrypoints.

Rules:

- Java owns scheduling policy and UI-facing orchestration.
- Python owns the execution of a requested job.
- Every job receives `JobRequest` and returns `JobResult`.
- Every triggered job goes through `JobRunner`.
- Locks, idempotency, job_run records, and checkpoints are runtime concerns, not job concerns.
- Pipelines receive `PipelineContext`; they do not read env directly.

Default runtime flow:

```text
Trigger API / CLI
  -> JobRunner
  -> PipelineContext
  -> Job function
  -> Pipeline
  -> Sources / Domain / Storage
  -> JobResult
```

