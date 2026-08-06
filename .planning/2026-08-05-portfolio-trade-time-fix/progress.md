# Progress

- 2026-08-05: Loaded AI, backend, frontend, and CodeGraph guidance. CodeGraph is available but reports pending changes; root worktree is already heavily modified.
- 2026-08-05: Added domain normalization and review UI fallback, a regression test, prompt v6, and release migration registration.
- 2026-08-05: Focused Maven test passed (7 tests), changed-file ESLint passed, UI build passed, version sync passed. Full UI lint remains blocked by pre-existing unrelated errors. Market, AI, and UI Release services were rebuilt/recreated; prompt v6 is active and both market/AI health endpoints plus UI returned 200/UP.
- 2026-08-05: Added and applied the legacy candidate time backfill migration; it was idempotent and updated 0 rows in the current database.
