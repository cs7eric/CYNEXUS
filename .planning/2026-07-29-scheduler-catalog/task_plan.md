# Scheduler catalog cleanup

## Goal

Normalize all scheduler job titles and regroup the full job catalog by business responsibility. Keep execution timing and implementation details in the existing schedule/description fields, not in the title.

## Phases

- [x] Inspect the current 59-job catalog, category schema, and migration conventions.
- [x] Add an idempotent scheduler catalog migration with normalized titles and refined categories.
- [x] Add a small UI title fallback so future legacy parenthetical titles do not leak into cards.
- [x] Synchronize versions, build, and verify the Release stack and API output.
- [x] Compact the category navigation and keep category editing available without horizontal overflow.

## Decisions

- Keep stable existing category codes and IDs where possible; add business-focused categories for uncovered jobs.
- A job receives one primary category so counts and navigation remain predictable.
- Historical schedule descriptions stay intact; only the display title is normalized.

## Errors Encountered

| Error | Attempt | Resolution |
|---|---:|---|
| Initial API property lookup returned no rows | 1 | The envelope is under `.data`; queried `.data.items` explicitly. |
