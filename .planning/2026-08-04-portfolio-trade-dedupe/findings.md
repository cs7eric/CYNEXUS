# Findings

Research notes for the portfolio screenshot import deduplication task.

## Current flow

- `PortfolioImportMessageListener` invokes `PortfolioImportProcessingDomainService.complete` after vision parsing.
- Candidates are persisted as `PENDING` in `portfolio_import_candidate` and reviewed through `PortfolioImportAppService.review`.
- `PortfolioImportConfirmationDomainService.confirm` converts selected pending candidates to `PortfolioTradeBO` and calls `PortfolioTradeRepository.save`.
- Existing database protection only deduplicates the same `(source_task_id, source_candidate_id)`, so the same symbol/time from a new screenshot can be inserted again.
- Review candidates already support `EXCLUDED`, and the UI only selects `PENDING`; warnings are rendered in the review table.

## Implementation direction

- Keep duplicate detection in the domain layer.
- Use normalized symbol + parsed `OffsetDateTime tradedAt` as the business key, scoped by principal and tenant.
- Enrich review candidates dynamically as `EXCLUDED` with a duplicate warning, so the confirm screen shows them as invalid without failing the import.
- Re-check duplicates during confirm to handle edits and concurrent imports.
- Add a database unique index and make the insert idempotent to protect against races.
