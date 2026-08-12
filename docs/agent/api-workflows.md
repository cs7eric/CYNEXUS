# API Workflow Process History

API-triggered automation uses a normalized process-history view. The view is an operational projection, not a replacement for a business result aggregate.

## Contract

Each process exposes `workflowType`, `workflowKey`, `sourceRef`, normalized status and phase, progress, attempts, errors, timestamps, an event timeline, and optional `resultRoute`/`resultRef` navigation metadata. It must not expose transcription content, trade candidates, receipt drafts, ledger entries, or other business output.

Normalized workflow types currently include:

- `MEDIA_TRANSCRIPTION`
- `PORTFOLIO_TRADE_IMPORT`
- `FINANCE_AI_CAPTURE`

Normalized statuses are `QUEUED`, `RUNNING`, `WAITING_REVIEW`, `SUCCEEDED`, `FAILED`, and `CANCELLED`.

Core exposes `/api/core/api-workflows`; Market exposes `/api/market/api-workflows`. The UI merges these bounded projections at `/settings/api-workflows`.

## Ownership

- Media transcription projects its existing process-history row.
- Finance AI Capture projects its existing capture batch status.
- Portfolio Trade Import projects its existing process-history events.
- Business pages remain the source of detailed output and confirmation actions.

When a new workflow is added, implement the same projection contract and keep retry/result navigation delegated to the owning business service.

The workflow history list is an operational UI and is intentionally not exposed to the autonomous Agent: it contains execution metadata, not a bounded business read that improves model decisions. Business result queries remain governed by their owning domain and Agent exposure rules.
