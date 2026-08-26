# API Workflow Process History

API-triggered automation uses a normalized process-history view. The view is an operational projection, not a replacement for a business result aggregate.

## Contract

Each process exposes `workflowType`, `workflowKey`, `sourceRef`, normalized status and phase, progress, attempts, errors, timestamps, an event timeline, and optional `resultRoute`/`resultRef` navigation metadata. It must not expose transcription content, trade candidates, receipt drafts, ledger entries, or other business output.

Normalized workflow types currently include:

- `MEDIA_TRANSCRIPTION`
- `PORTFOLIO_TRADE_IMPORT`
- `FINANCE_AI_CAPTURE`

Normalized statuses are `QUEUED`, `RUNNING`, `WAITING_REVIEW`, `SUCCEEDED`, `FAILED`, and `CANCELLED`.

Public ingest uses a shared prefix:

- `POST /api/workflows/media-transcription`
- `POST /api/workflows/portfolio-trade-import`
- `POST /api/workflows/finance-ai-capture`

Each ingest endpoint only accepts the complete request body, persists its
process/task and durable queue/outbox record, then returns HTTP `202`
immediately. It must never wait for download, ASR, AI parse, result posting,
or worker completion. Multipart endpoints necessarily wait for the client to
finish uploading the file, but never for subsequent processing.

Every accepted ingest response uses this acknowledgement shape:

```json
{
  "accepted": true,
  "message": "queued",
  "processId": "123",
  "status": "queued",
  "workflowType": "MEDIA_TRANSCRIPTION",
  "workflowKey": "media.transcription",
  "pollUrl": "/api/core/media-transcription/processes/123"
}
```

`status=queued` is the acknowledgement state, not an instruction to wait for a
terminal business result. Poll `pollUrl`, the workflow history, or the owning
process API for later status.

Legacy service paths remain valid. In-app finance capture
`POST /api/core/finance/ai-captures` still parses synchronously for the UI. History listing stays on the owning services:
Core `/api/core/api-workflows`, Market `/api/market/api-workflows`. The UI
merges these bounded projections at `/settings/api-workflows`.

These ingest endpoints are not exposed to the autonomous Agent: they start paid
external download/ASR or mutation work and are not bounded reads.

## Ownership

- Media transcription projects its existing process-history row.
- Finance AI Capture projects its existing capture batch status.
- Portfolio Trade Import projects its existing process-history events.
- Business pages remain the source of detailed output and confirmation actions.

When a new workflow is added, implement the same projection contract and keep retry/result navigation delegated to the owning business service.

The workflow history list is an operational UI and is intentionally not exposed to the autonomous Agent: it contains execution metadata, not a bounded business read that improves model decisions. Business result queries remain governed by their owning domain and Agent exposure rules.

Original uploaded images are also intentionally excluded from autonomous Agent exposure. They are private binary inputs for the owning workflow and are only available to the signed-in UI through owner-scoped image preview endpoints.
