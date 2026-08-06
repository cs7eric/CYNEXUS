# Findings

- Release API reports 59 active scheduler jobs.
- 26 current titles contain a parenthetical schedule or implementation suffix.
- Existing category codes are `intraday`, `ai`, `market_sentiment`, `data_collection`, `eod_backfill`, and `test`.
- Many jobs have no category, including minute K-line, auction, fund-flow, margin, market information, and TDX industry jobs.
- Scheduler data is stored in `cynexus_appl_core.scheduler_jobs`, `scheduler_job_categories`, and `scheduler_job_category_mappings`.
- Category IDs are bigint and job IDs are bigint. Existing migrations use idempotent PL/pgSQL lookups and soft-deletable mappings.
- The API exposes job `name` directly, so the durable cleanup belongs in a SQL migration; the UI can add a defensive display fallback.
