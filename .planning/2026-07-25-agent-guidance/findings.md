# Findings

- `AgentToolHandler` requires only `definition()` and `execute()`; `capabilityDescriptor()` is a default adapter from `AgentToolDefinition`.
- `AgentToolRegistry` synchronizes Handler descriptors on startup, preserves DB governance fields (`enabled`, `governanceAvailability`, `priority`), and loads active runtime descriptors from DB.
- The model provider adapter currently sends only tool name, description, and `inputSchemaJson` as function parameters. `outputSchemaJson` is not sent to the model.
- The 14 concrete Handler definitions are in `cynexus-ai-infra/.../agent/{market,finance,knowledge,application/analysis}`. Existing schemas mostly lack property-level descriptions and usage examples.
- DB schema uses JSONB type handlers for `input_schema_json` and `output_schema_json`; a guidance JSONB column fits the existing persistence pattern.
- Built-in synchronization currently overwrites code-owned metadata while preserving only governance fields. Guidance needs an explicit merge rule so seeded/default guidance is persisted without allowing DB to alter executable safety fields.
