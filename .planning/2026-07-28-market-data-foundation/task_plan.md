# Market Data Foundation

## Goal

Improve market indicator refresh reliability and establish a durable stock profile, financial, and daily trading data foundation for future dynamic stock screening.

## Phases

- [completed] Inspect current scheduler, stock schema, collectors, migrations, and release runtime.
- [completed] Design and implement candidate-pool refresh and scheduler observability/frequency fixes.
- [completed] Add normalized stock profile, financial snapshot, and daily trading fact schema/storage required for screening.
- [completed] Add focused tests and migration/runtime wiring.
- [completed] Version-sync, build, release-stack verification, and cleanup.

## Constraints

- Preserve unrelated existing worktree changes.
- Do not start deprecated Dev, local Java, Vite, or source-tree application runtimes.
- Verify only through the versioned Release stack.
- Keep business rules in the appropriate domain layer.

## Errors Encountered

| Error | Attempt | Resolution |
|---|---:|---|
| PowerShell split `-Dsurefire.failIfNoSpecifiedTests=false` at `.` | 1 | Quote the Maven system-property argument before retrying. |
