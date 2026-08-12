# Optimize Release UI delivery and startup performance

## Goal

Keep Release UI as a Dockerized build artifact served by Nginx, reduce initial JavaScript work with route-level lazy loading, and improve static/API proxy behavior without starting Dev or source-tree runtimes.

## Phases

- [completed] Inspect current UI bundle, routes, and Nginx behavior
- [completed] Implement route splitting and Nginx delivery optimizations
- [completed] Run UI lint/build checks
- [completed] Rebuild and recreate the Release UI Docker service
- [completed] Verify headers, routing, runtime health, and worktree scope

## Scope guard

- Preserve unrelated user changes in the dirty worktree.
- Limit code changes to `cynexus-ui/src/router/index.tsx` and Release UI delivery configuration unless verification proves another file is required.
- Do not start Vite, local Java, or the deprecated Dev application.

## Errors Encountered

| Error | Attempt | Resolution |
|---|---:|---|
| Full `pnpm lint` fails on 111 existing UI errors outside this change, including `set-state-in-effect` and ref access in existing components | 1 | Run targeted lint for the changed router and rely on the successful production build; do not modify unrelated UI code. |

## Verification

- `pnpm exec eslint src/router/index.tsx` passed.
- `pnpm run build` passed and generated route-level chunks; the main JS chunk is approximately 321 KB (gzip approximately 104 KB).
- `deploy/release/start-release.ps1 -Build -Version 1.1.398-260807-RELEASE -Services ui` completed successfully.
- Nginx configuration test passed inside `cynexus-release-ui`.
- Hashed assets return gzip content encoding and immutable cache headers.
- `index.html` and SPA fallback routes return no-cache headers.
- `cynexus-release-ui` is running with restart count 0 on `cynexus-ui:1.1.398-260807-RELEASE`.
