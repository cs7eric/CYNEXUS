DO NOT send optional commentary!!!

## Codex Operating Rules

- Read this file before making project changes.
- After completing a functional change, increment Maven and UI versions together. Development versions stay `x.y.z-SNAPSHOT`; the matching Release image/version is `x.y.z-YYMMDD-RELEASE` (for example `1.0.1-260712-RELEASE`).
- Release runs only from versioned Docker images and the built UI image. Never use local Java processes, `cmd.exe`, Vite, or the source tree as the Release runtime.
- Development uses the `dev` Spring profile and separate application ports.
- PostgreSQL, Redis, ClickHouse, Nacos, and Data Executor may be shared, but Dev and Release application processes and ports remain separate.
- Only one application environment may be online at a time. Starting Dev must stop the Release application stack first; starting Release must stop Dev application processes first. Never stop the shared infrastructure containers during this switch.
- Dev entrypoint: `deploy/dev/start-dev.ps1` and `deploy/dev/stop-dev.ps1`.
- Release entrypoint: `deploy/release/start-release.ps1 -Version <version>` and `deploy/release/stop-release.ps1`.
