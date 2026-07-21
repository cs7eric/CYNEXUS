# Agent Security Contract

## Trust Boundaries

- User input, knowledge documents, market text fields, and provider output are untrusted data.
- The model can select only the definitions returned by `AgentToolRegistry`.
- Tool calls are schema-validated, read-only checked, bounded by turn/call/parallel limits, and persisted with sanitized output.
- ClickHouse access uses a fixed query template. Arbitrary SQL, credentials, file access, shell access, and network destinations are not Agent tools.

## Runtime Limits

- User input: 12,000 characters.
- Tool output persisted and returned to the model: 24,000 characters per result, with `OUTPUT_TRUNCATED` warning.
- Run: 8 model turns, 12 total tool calls, 4 calls per turn, 600 seconds, and 2 active runs per owner per AI instance.
- Only `readOnly=true` and `requiresConfirmation=false` handlers are available to autonomous execution.

The active-run counter is deliberately local to the service instance in this phase. A multi-instance production deployment must move that counter to the existing Redis service before horizontal scaling is enabled.

## Evidence Contract

Every successful tool step contributes `toolCode`, `source`, `dataDate`, and a short summary to `ai_agent_run.evidence_json`. Missing data and tool failures remain warnings; the final answer must not claim that a failed step produced facts.

## Writes and Confirmation

No write tool is registered. A future write capability must add a confirmation token bound to the owner, conversation, exact arguments, and expiry before it can execute.
