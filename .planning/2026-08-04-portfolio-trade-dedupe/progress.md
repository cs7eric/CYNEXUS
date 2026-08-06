# Progress

## 2026-08-04

- Started investigation of portfolio AI screenshot import and confirmation flow.
- Inspected the CodeGraph flow and current domain/application/infra/UI implementations.
- Confirmed duplicate protection currently exists only for the same source task/candidate, not symbol + tradedAt.
- Added domain review/confirm duplicate checks, race-safe repository insertion, UI invalid status, and a precise-time unique index migration.
- UI lint/typecheck and market compile passed. The first focused Maven test command failed because PowerShell/Maven parsed an unquoted `-D` property as a lifecycle phase; the corrected focused tests passed.
- Focused domain tests passed: 3 tests, 0 failures. Market starter compile and UI lint/typecheck passed.
- Fixed the PostgreSQL partial-index predicate to use a fixed UTC immutable expression; the duplicate-protection index now applies successfully while preserving legacy midnight-only rows.
- Rebuilt and restarted the Release `market` and `ui` services as `1.1.398-260804-RELEASE`; both containers are running and `http://localhost:5173/` returns HTTP 200.
