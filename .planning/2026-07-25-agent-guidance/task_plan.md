# Agent Tool Guidance

## Goal

Add structured, persisted guidance for the existing 14 Agent tools, expose the guidance to the model through the provider adapter, and keep executable schemas, safety flags, and runtime bindings controlled by code.

## Phases

- [complete] Inspect current capability persistence, schema, adapter payload, and tool definitions.
- [complete] Add guidance contract, DB JSONB persistence, synchronization semantics, and adapter rendering.
- [complete] Enrich all 14 tool descriptions and input schemas with actionable usage details.
- [complete] Add focused tests and synchronize Maven/UI versions.
- [complete] Run verification and clean planning scratch state.

## Decisions

- `inputSchemaJson` remains the machine-validated argument contract.
- `outputSchemaJson`, `readOnly`, `requiresConfirmation`, and `runtimeEntry` remain code-controlled and must match the bound Handler.
- Guidance is model-facing metadata and may be persisted as JSONB; provider adapters flatten it into the standard function `description` field.
- No arbitrary DB-defined runtime execution is introduced.

## Errors Encountered

None.
