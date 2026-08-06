# Task Plan: CDP Social Collection Integration

## Goal

Replace the OpenCLI primary path with platform-specific collectors connected to two local Chrome CDP endpoints while preserving the existing Information social-monitoring UI, daily incremental storage, and read-only safety boundary.

## Phases

- [completed] Phase 1: Inspect current social agent, UI contract, storage, and OpenCLI runtime
- [in_progress] Phase 2: Implement CDP local adapter and platform-specific login sessions
- [pending] Phase 3: Connect discovery/comment jobs and preserve incremental writes
- [pending] Phase 4: Update UI guidance and runtime scripts/configuration
- [pending] Phase 5: Run focused tests and local CDP/agent verification
- [pending] Phase 6: Deploy affected Release services and hand off setup steps

## Decisions

- Use two local Chrome profiles with CDP ports 19221 (Xiaohongshu) and 19222 (Douyin); do not use the paid SocialDataX plugin.
- Keep collection read-only; never invoke OpenCLI write commands.
- The local agent owns Chrome startup and reuses the profile cookies through Playwright `connect_over_cdp`.
- Keep the existing database schema and idempotent upsert behavior; adapt OpenCLI JSON into existing social models.
- OpenCLI remains available only as an explicit diagnostic/backup backend.

## Errors Encountered

| Error | Attempt | Resolution |
|---|---:|---|
| OpenCLI Browser Bridge was unavailable | 1 | Make CDP-connected dedicated Chrome the primary path; keep OpenCLI only when explicitly selected |
| A terminated run left a lock/run record | 1 | Add lock heartbeat, shorter lease, scoped interrupted-run recovery, and owner lock cleanup |
