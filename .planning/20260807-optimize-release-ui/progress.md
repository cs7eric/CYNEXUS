# Progress

## 2026-08-07

- Confirmed Release UI already uses a built `dist` directory inside an Nginx Docker image.
- Added route-level `React.lazy` loading with a shared loading fallback.
- Added Nginx compression, immutable asset caching, no-cache SPA entry handling, buffered ordinary API proxying, and a streaming-only unbuffered proxy location.
- `pnpm run build` passed; output now has route chunks and an approximately 321 KB main JavaScript chunk instead of the previous approximately 5.4 MB entry file.
- Full `pnpm run lint` is currently blocked by 111 pre-existing UI errors outside this change.
- Targeted lint for `src/router/index.tsx` passed with no findings.
- Docker rebuilt and recreated `cynexus-release-ui` successfully as `cynexus-ui:1.1.398-260807-RELEASE`.
- Nginx syntax, gzip, cache headers, SPA fallback, and streaming/ordinary API proxy settings were verified inside the running container.
