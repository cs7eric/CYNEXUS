# Cynexus Data Agent Guide

## Role

`cynexus-data` is the Python Job Executor. It produces market and media-transcription data for Java to consume. It is not the UI API, AI provider manager, scheduler configuration owner, or Java business layer.

It owns data-source connections, collection, parsing, normalization, calculations, storage, quality checks, repair/backfill, job execution, and the internal trigger API.

## Boundaries

```text
jobs -> runtime/job_runner -> pipelines -> sources/domain/storage -> hooks -> connections
```

- `sources` adapt external providers and never write databases or manage jobs.
- `domain` contains pure data calculations and must not access the network, databases, environment variables, or files.
- `pipelines` orchestrate source, normalization, domain, quality, and storage work; they support `dry_run`.
- `storage` owns database reads/writes and does not hardcode connection parameters.
- `connections` resolve configuration and secrets; `hooks` create usable clients.
- `jobs` receive a `JobRequest`, call a pipeline, and return a `JobResult`.
- All jobs go through `runtime/job_runner`; jobs do not own locks, run records, checkpoints, or scheduler configuration.
- Python does not own Java scheduler cron, enable/disable state, UI response assembly, AI provider management, polish, summary, or ask/Q&A.
- Media work stops at transcription and persistence of the transcript; later AI processing remains in Java.

## Placement

- Python package: `src/cynexus_data/`
- Tests: `tests/unit` and `tests/integration`
- Durable data design: `docs/`
- ClickHouse/PostgreSQL SQL: `sql/`
- Operator scripts: `scripts/`
- Runtime state and logs: `var/` (ignored)

Read `cynexus-data/docs/architecture/layering.md`, `cynexus-data/INFRA.md`, and `cynexus-data/migration.md` for detailed data architecture and migration context. Use `$cynexus-data` for multi-step data workflows.
