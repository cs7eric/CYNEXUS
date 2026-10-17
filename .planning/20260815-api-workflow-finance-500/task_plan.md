# Task Plan: Diagnose Finance API Workflow 500

## Goal
Identify why the Finance upload through API Workflows returned HTTP 500, fix it if the cause is in the repository, and verify the relevant behavior without disturbing existing worktree changes.

## Current Phase
Phase 1: Requirements & Discovery

## Phases

### Phase 1: Requirements & Discovery
- [x] Understand user intent
- [x] Identify relevant entrypoints and constraints
- [x] Document initial findings
- **Status:** in_progress

### Phase 2: Root Cause Analysis
- [ ] Inspect CodeGraph call flow and exact source
- [ ] Inspect Release/runtime logs and request evidence
- [ ] Confirm root cause
- **Status:** pending

### Phase 3: Implementation
- [ ] Apply the smallest scoped fix if required
- [ ] Add or update focused tests if required
- **Status:** pending

### Phase 4: Testing & Verification
- [ ] Run focused tests/compile
- [ ] Verify runtime behavior or report unavailable runtime evidence
- **Status:** pending

### Phase 5: Delivery
- [ ] Review diff and worktree impact
- [ ] Report cause, fix, and verification clearly
- **Status:** pending

## Key Questions
1. Which controller and backend service handled the submitted Finance workflow request?
2. What exception produced the HTTP 500 in the running Release stack?
3. Is the defect code, deployment/configuration, payload, or external dependency related?

## Decisions Made
| Decision | Rationale |
|----------|-----------|
| Keep investigation scoped to API Workflow Finance paths | The worktree contains unrelated user changes that must be preserved |
| Use existing Release runtime/logs as primary evidence | Repository guidance forbids starting deprecated/local backend runtimes |

## Errors Encountered
| Error | Attempt | Resolution |
|-------|---------|------------|
| FastCtx and CodeGraph MCP tools were not exposed in this session | 1 | Use the repository CodeGraph CLI if available, otherwise the documented CLI fallback |

## Notes
- Do not deploy or restart services until the cause and affected boundary are established.
- Do not edit unrelated existing changes.
