---
name: cynexus-release
description: Use when building, restarting, checking, or verifying the versioned Cynexus Docker Release stack, migrations, images, or runtime health.
---

# Cynexus Release Workflow

1. Read `deploy/AGENTS.md` and `docs/agent/release.md`.
2. Confirm shared PostgreSQL, Redis, ClickHouse, Nacos, and Data Executor infrastructure remains running.
3. Resolve the synchronized snapshot version and derive the Release version.
4. Use `deploy/release/start-release.ps1 -Version <version>` for application startup or restart.
5. Never start Dev, local Java, Vite, or a parallel application stack.
6. Verify changed services, health, migrations, and the user-facing behavior.
7. Report exact commands and any skipped verification.
