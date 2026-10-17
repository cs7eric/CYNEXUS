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

## Known Operational Pitfalls

### `docker compose` subcommand missing on the host

Some Docker Desktop installs expose only the `docker-compose` standalone binary and an
extension-less `docker` shim, which PowerShell cannot use mid-pipeline. Symptoms:
`docker: unknown command: docker compose`, or `Cannot run a document in the middle of a
pipeline` for `docker exec` inside migration scripts.

Fix by installing the CLI plugin, then verify `docker compose version`:

```bash
mkdir -p ~/.docker/cli-plugins
cp "/c/Program Files/Docker/Docker/resources/bin/docker-compose.exe" ~/.docker/cli-plugins/docker-compose.exe
```

If the plugin cannot be installed, run the equivalent `docker-compose -f <file>` commands
directly and apply migrations with `docker exec -i cynexus-postgres psql -U postgres -d cynexus -v ON_ERROR_STOP=1 < <migration.sql>` (lists live in `deploy/release/apply-*-migrations.ps1`).

### Nacos fails to start: `load derby-schema.sql error` / `Login timeout exceeded`

The embedded Derby store under `deploy/local/nacos/data/derby-data` becomes unusable after
an unclean Docker shutdown. Nacos exits on startup, never registers services, and every
gateway route returns 502 while backend containers restart-loop.

Diagnose with `docker logs cynexus-nacos | grep -i derby`, then recover:

1. `docker stop cynexus-nacos` (graceful, never force).
2. Move the store aside: `mv derby-data derby-data.bak-<date>`.
3. `docker compose -f deploy/local/docker-compose.yml up -d nacos`; wait for `/nacos/actuator/health` = `UP`.
4. Re-publish the Nacos config that the fresh store lacks (`cynexus-core.yml`, DEFAULT_GROUP,
   incl. `cynexus.scheduler.runtime.enabled: true`) through `POST /nacos/v1/cs/configs`.
   The old content can be extracted from the Derby backup with a byte scan for `max-file-size`.
5. Recreate `core` so it loads the restored config.

### AI service crash-loops on `RAG embedding API key is missing for DASHSCOPE_API_KEY`

The Release compose reads `DASHSCOPE_API_KEY` from the shell environment, but the value only
exists in `deploy/local/.env`. Export it before recreating `ai`:

```bash
export DASHSCOPE_API_KEY="$(grep -m1 '^DASHSCOPE_API_KEY=' deploy/local/.env | cut -d= -f2- | tr -d '\r')"
```
