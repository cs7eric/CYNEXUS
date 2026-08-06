# Futures Position Historical Backfill

## Goal

Backfill as much member ranking history as available for all supported futures data, targeting four years while preserving source fallback and data integrity.

## Phases

- [x] Inspect the existing sync/backfill implementation, schemas, and operational entrypoints
- [ ] Determine a bounded four-year backfill strategy and implement or invoke it safely
- [ ] Run the backfill with progress/error capture and verify coverage by exchange/product
- [ ] Update synchronized versions if code changes are required and verify the Release stack

## Constraints

- Do not start the deprecated Dev application or local Java/Vite runtime.
- Preserve valid existing data when a source returns empty/failed data.
- Do not leave scratch files in the repository root.
- Backfill is allowed to produce data changes; schema/code changes require version synchronization.

## Errors Encountered

| Error | Attempt | Resolution |
|---|---:|---|

