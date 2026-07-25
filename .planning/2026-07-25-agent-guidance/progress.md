# Progress

## 2026-07-25

- Read project instructions and planning skill.
- Confirmed clean worktree at commit `92b7a7f1`.
- Inspected Handler, Registry, capability repository, DB mapper/service, provider adapter, and existing tool definitions.
- Created the task plan and recorded the current metadata flow.
- Confirmed `AgentCapabilityPO` uses JSONB handlers for schema fields, the capability API currently exposes governance only, and the UI does not edit capability metadata.
- Confirmed provider payload supports only standard function `description` and `parameters`; guidance must be rendered into description.
- Added structured `guidanceJson` through Handler definitions, capability descriptors, persistence, API DTOs, and UI types.
- Added the guidance JSONB migration and release migration registration; built-in synchronization fills empty guidance while preserving existing DB guidance.
- Enriched all 14 tool descriptions and input schemas, added output field descriptions where output schemas are defined, and rendered guidance into provider descriptions.
- Added focused contract, renderer, registry, and migration tests. Maven compile, focused tests, and UI build pass; the development stack was restarted on version `1.1.269-SNAPSHOT`.
