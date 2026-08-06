# Agent Tool Completion Plan

## Goal

Expose the newly added bounded project reads to the autonomous Agent and bring
the existing futures AI tool into the handler/schema/test contract.

## Phases

- [completed] 1. Inspect current contracts, Feign APIs, guidance, and tests
- [completed] 2. Add missing read-only Agent handlers and schemas
- [completed] 3. Complete futures AI tool contract and legacy catalog decision
- [completed] 4. Add focused tests and verify compilation/version consistency

## Scope

- `market.indicator`
- `market.etf`
- `market.futures_position`
- margin industry history operation
- futures AI tool metadata/schema/error handling/tests
- keep admin/CRUD/data-pipeline operations excluded from autonomous tools

## Constraints

- Preserve unrelated dirty worktree changes.
- Do not start source-tree applications or restart infrastructure.
- Use existing Feign/domain contracts and governed Agent registry patterns.
