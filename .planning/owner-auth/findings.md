# Findings

- User approved single-owner authentication, no user table, 30-day idle expiry.
- Vite /data-api bypassed Gateway; now all data API requests use Gateway.
- Redis operations use common primitives, with atomic Lua in gateway infra.
- Login is rate limited; authenticated mutations require same Origin and session CSRF token.
- Local HTTP is supported only for localhost/127.0.0.1. Public cookie is Secure, HttpOnly, SameSite=Strict.
- Password hash fingerprint namespaces sessions, so configuration rotation revokes old sessions.
- Auth APIs are explicitly excluded from Agent tools: credentials and session administration are security-sensitive.
- Preserve uncommitted navigation split and unrelated artifacts.
- Preserve ClickHouse/Data Executor off and 67 market jobs disabled.
- Cloudflare Access configuration has not been changed.
- Tunnel ingress verified via localhost:20241/config: ui-dev.nooops.makeup -> 5174, api/wechat -> Gateway18000; main Cynexus/dev hosts -> stopped5173. Main hostname returns Access redirect; ui-dev is reachable.
- Exact GET/POST WeChat callback must preserve provider signature verification, while never receiving signed owner headers. Other webhook/management paths remain protected.
