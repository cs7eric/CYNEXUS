# Portfolio trade image analysis API

## Goal

Add an API-driven image analysis flow equivalent to the existing financial analysis upload flow, but route it through the AI trade-recognition endpoint and return the recognized transaction candidates for review/import.

## Phases

- [complete] Inspect the existing financial image upload flow and trade-recognition API contract.
- [complete] Implement the API/backend orchestration and response mapping.
- [complete] Confirm no frontend entry is needed for this API-only request; the existing portfolio UI remains unchanged.
- [complete] Reuse the existing AI capability metadata, prompt, and binding; add focused domain tests.
- [complete] Run focused checks, version synchronization, and Release verification.
- [complete] Make malformed AI responses retryable and re-verify the Release worker.

## Decisions

- Reuse the existing image upload/storage and AI vision infrastructure wherever possible.
- Keep the stable capability identifier aligned across backend, prompt, binding, execution log, and AI Provider UI.
- Return transaction candidates into the existing portfolio import review flow rather than creating a second confirmation implementation.

## Errors Encountered

| Error | Attempt | Resolution |
|---|---:|---|
| `JsonNode` unavailable in market API module | 1 | Return the validated AI JSON as the same string `content` shape used by the generic vision API; keep Jackson parsing in domain only. |
