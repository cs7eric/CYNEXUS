# Cynexus Deployment Agent Guide

## Runtime Policy

- The Dev application environment is deprecated and must not be started.
- Do not start local Java processes, Vite, `cmd.exe`, or source-tree application runtimes.
- Only the Release application stack may be online.
- PostgreSQL, Redis, ClickHouse, Nacos, and Data Executor are shared infrastructure and must remain running while the Release application stack is switched or restarted.
- Use `deploy/release/start-release.ps1 -Version <version>` for Release startup and `deploy/release/stop-release.ps1` to stop the application stack.
- Release runs from versioned Docker images and the built UI image.
- After any functional, configuration, migration, or UI change, the running Release stack must be rebuilt and recreated before the work is considered complete. Deploy every service that could be affected, including dependent services; if the impact cannot be proven narrowly, deploy the full application stack. Do not treat compilation or tests as deployment.

## Version Contract

Root `pom.xml`, every Cynexus module parent version, and `cynexus-ui/package.json` must use the same `x.y.z-SNAPSHOT`. Release images use `x.y.z-YYMMDD-RELEASE`.

Never update only one version location. After a functional change, verify all Maven parent/project versions and the UI package version before Release verification.

## SQL Placement

- PostgreSQL schema/data migrations and AI prompt seeds go in `deploy/local/postgres/migrations/YYYYMMDD_*.sql`.
- Long-lived schema references go in `sql/` or the relevant data project `sql/` directory.
- Do not leave restore scripts, temporary SQL, or credentials in the repository root.
- Agent-related migrations must also be listed in `apply-agent-migrations.ps1`; market migrations must follow the corresponding release migration bundle.

Read `docs/agent/release.md` and `docs/agent/repository-and-database.md` for the complete operational and file-placement rules. Use `$cynexus-release` or `$cynexus-version-sync` for repeatable workflows.
