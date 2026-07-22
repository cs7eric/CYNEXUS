# Agent UI Release Workflow Acceptance (2026-07-23)

## Scope and constraints

- Scope was limited to the `cynexus-ui` Agent page, UI acceptance evidence, and the synchronized UI/Maven version state.
- No Java, database, deployment, notification, dead-letter, security, or dynamic-capability files were changed by this acceptance.
- The tested worktree version state was synchronized at `1.1.183-SNAPSHOT`.
- Docker commands were stopped after the environment requested approval for Docker API access. No new Release image was built or started by this run.
- Existing Release UI/API endpoints were used where reachable. No mocked Provider, fixture, or fake API response was used.

## Evidence

| Check | Result | Evidence |
| --- | --- | --- |
| Agent route shell | PASS | `GET /ai/agent` returned HTTP 200 from the existing Release UI. |
| Agent tools catalog | PASS | `GET /api/ai/agent/tools` returned HTTP 200, result code `0`, and 14 tools. |
| Conversation history | PASS | `GET /api/ai/agent/conversations?limit=30` returned HTTP 200, result code `0`, and 29 conversations. |
| Empty state | PASS by source/build contract | The page contains the empty transcript state and compiles in the production build. |
| Thinking marker | PASS by source/build contract | Thinking is rendered as a `Marker` row outside the assistant `Bubble`, with `role="status"`. |
| Composer | PASS by source/build contract | The composer uses a fixed-height textarea, native scrolling, hidden scrollbar, wrapping text, and a stable send/stop footer. |
| Desktop/mobile layout | PASS by source/build contract | Responsive grid and width constraints compile successfully; browser pixel checks were unavailable. |
| Streaming | BLOCKED | A real POST to `/api/ai/agent/runs/stream` timed out after 60 seconds. No successful Provider stream is claimed. |
| Export/retry/regenerate/follow-up/stop | BLOCKED for new Release evidence | These require a completed real Provider run, which was unavailable. |
| Error recovery | PARTIAL | Error and terminal failed-run rendering exists, but the timeout did not produce a browser-captured terminal error state. |
| History reopen | PARTIAL | The history endpoint and page data are reachable; browser reopening was not verified in this environment. |

## Build verification

```text
pnpm install --frozen-lockfile  PASS
pnpm build                    PASS
```

Existing Vite font-path/chunk-size warnings remain. Repository-wide lint remains blocked by pre-existing errors outside the Agent page.

## Final disposition

The UI build and reachable Release API surface pass. Real Provider-dependent workflow gates remain open because the Release stream timed out and rebuilding the Docker Release image required unavailable approval. This document intentionally records those gates as blocked.
