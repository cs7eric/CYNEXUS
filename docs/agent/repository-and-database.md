# Repository Layout & File Placement

## Placement

- `cynexus-*`: Java modules only.
- `cynexus-ui`: frontend source and UI artifacts.
- `cynexus-data`: Python data project.
- `deploy`: runtime scripts and PostgreSQL migrations.
- `docs`: durable design, architecture, and release notes.
- `guide`: onboarding and project guides.
- `design`: UI/product design assets and page designs.
- `sql`: long-lived schema references, not one-off repair scripts.
- `db`: product DB contracts and checks.
- `ci`: reusable deterministic checks.
- `artifacts`: intentional checked-in screenshots or release evidence only.
- `.planning`: short-lived task planning; `.planning/scratch`: session-only scratch.
- `var`: tiny local runtime samples only.

## Hygiene

- Never create ad-hoc scratch scripts, logs, jars, decompile directories, replay dumps, provider keys, or one-off SQL in the repository root.
- Use OS temp or an ignored path outside tracked source for temporary work. If an in-repository session file is unavoidable, use `.planning/scratch` and remove it before finishing.
- Logs and build outputs belong in ignored locations. Do not commit logs, crash dumps, packaged jars, or decompiled sources.
- Never write keys or raw API keys to the repository. Use environment variables or existing ignored secret files.
- Before finishing, delete session scratch files and verify `git status` has no root junk.

## SQL

- Schema/data migrations go in `deploy/local/postgres/migrations/YYYYMMDD_*.sql`.
- Durable schema documents go in `sql/<domain>/` or the owning data project SQL directory.
- Never leave `restore_*.sql` or `_tmp.sql` at the root.
