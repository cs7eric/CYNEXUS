# Progress

## 2026-07-26

- Read project operating rules and existing cache optimization plan.
- Started inspection of Agent tool schemas, handlers, and registry.
- Queried persisted Agent runs and confirmed the failure is not a schema definition typo: the parsed tool calls are empty objects before registry validation.
- Identified the Responses streaming item-id/call-id correlation bug; preparing a parser fix and regression test.
- Fixed Responses streaming correlation by using `item_id` as the accumulator key while preserving `call_id` for execution.
- Added a regression test covering `response.output_item.added`, argument delta, and argument done events.
- Focused adapter tests passed: 12 tests; tool contract, registry, and market handler tests passed: 19 tests.
- Synchronized Maven and UI versions to `1.1.282-SNAPSHOT`.
- Hardened the correlation helper for providers that emit only one of `item_id` or `call_id`; the focused adapter suite still passes 12 tests.
- Rebuilt and redeployed Release image `1.1.282-260726-RELEASE`.
- All five application actuator health endpoints returned HTTP 200; the Agent binding remains configured with `wire_api=responses`.
