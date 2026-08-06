---
name: cynexus-data
description: Use when modifying the Python data executor, sources, hooks, connections, pipelines, storage, jobs, data quality, migrations, or market-data tests.
---

# Cynexus Data Workflow

1. Read `cynexus-data/AGENTS.md`, `cynexus-data/INFRA.md`, and the relevant data design document.
2. Inspect existing source, pipeline, storage, job, and test patterns.
3. Keep connections and credentials in configuration, clients in hooks, external adaptation in sources, business calculations in domain, writes in storage, orchestration in pipelines, and lifecycle in JobRunner.
4. Keep jobs dry-run capable and return JobResult.
5. Do not migrate Java scheduler state or AI post-processing into Python.
6. When a new data operation becomes a stable user-facing query/read/write capability, load `$cynexus-agent-tools`; expose it through a bounded Java API/gateway and Agent tool rather than letting the Agent call Python internals, raw storage, or arbitrary jobs directly. Document exclusions for unsafe/admin-only operations.
7. Run focused pytest checks and the relevant dry-run command.
