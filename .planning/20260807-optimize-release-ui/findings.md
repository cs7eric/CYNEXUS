# Findings

- Release UI already uses the intended architecture: `deploy/release/Dockerfile.ui` copies `cynexus-ui/dist` into `nginx:1.27-alpine`; Compose exposes Nginx on `127.0.0.1:5173`.
- Nginx currently proxies all `/api/` requests to the gateway, disables buffering for every API request, disables cache, and has no gzip or static cache headers.
- Current built output has an approximately 5.4 MB entry JavaScript file, approximately 408 KB CSS, and about 7.0 MB total assets.
- `cynexus-ui/src/router/index.tsx` eagerly imports all page modules, including heavy chart/editor pages, so the first bundle includes unrelated routes.
- Current UI image is `cynexus-ui:1.1.398-260806-RELEASE`; the next UI build should use the synchronized current snapshot version and a current Release tag.
- Route modules now load through `React.lazy` behind a shared `Suspense` loading state.
- Nginx now compresses text-like assets, gives hashed assets immutable one-year caching, keeps `index.html` revalidating, buffers ordinary API responses, and disables buffering only for `/api/**/stream` endpoints.
