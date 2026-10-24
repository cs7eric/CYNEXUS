# Workout Agent Exposure

Workout records are intentionally excluded from autonomous Agent tools for the
current release. The snapshot contains health and body-measurement data, and
the Agent runtime does not yet have a product-level consent and retention
contract for this sensitive personal data. Exposing the Core CRUD routes would
also bypass the UI's explicit owner-authenticated context.

The Core API remains owner-scoped and read/write-capable for the Workout UI.
When Agent access is introduced, add one bounded read-only snapshot capability
through an owning API contract with explicit owner context, date/row limits,
source/freshness metadata, and privacy tests. Workout mutations must remain
outside the autonomous tool registry and use an explicit preview/confirmation
workflow.
