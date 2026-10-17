# Owner authentication

Cynexus has a single configured owner, without a user table. Gateway enforces authentication before forwarding any route or adding trusted identity headers. Personal and development navigation spaces share this login and existing workspace-user/default identity.

## Sessions

- Passwords are verified with PBKDF2-HMAC-SHA256 (310,000 iterations, random 128-bit salt, 256-bit result).
- Each successful login creates an independent random 256-bit opaque credential and CSRF token. Redis stores a SHA-256 digest of the credential, never the raw cookie.
- Remembered devices use a host-only HttpOnly, Secure, SameSite=Strict cookie. Every authenticated request renews Redis and cookie expiry to 30 days. Localhost HTTP development omits Secure.
- Unchecking remember uses a browser-session cookie and a rolling 12-hour Redis expiry.
- Sessions are independent of IP addresses. Mobile network changes do not invalidate a session.
- Current-device logout deletes its Redis record. Logout-all atomically rotates the owner epoch and invalidates all existing devices. Password configuration changes create a different session namespace.
- Redis failures fail closed. Redis data loss requires reauthentication; the session store is intentionally authoritative for revocation, unlike an ordinary data cache.
- Login attempts are limited to 10 per TCP peer and 100 total in a 15-minute fixed window. Proxied traffic shares a peer limit, appropriate for this single-owner deployment. Client-supplied forwarding headers do not bypass limits.
- All mutation requests require an exact same Origin; authenticated mutations also require the session's X-Cynexus-CSRF header. Public HTTP is rejected for mutations. Same-origin fetch covers ordinary APIs, uploads and streaming requests. SSE GET uses the same session cookie.

## Local operation

Run `pwsh -File deploy/release/initialize-owner-auth.ps1` before the first Gateway deployment. This generates ignored, Windows ACL-restricted files:

- `deploy/release/.owner-auth.json`: password hash, loaded by start-release.ps1.
- `deploy/release/.owner-initial-password.txt`: initial random password for the owner. Treat this as a local secret; remove after storing in a password manager.

Deploy Gateway with `deploy/release/start-release.ps1 -Services gateway -Build -Version 2.0.0-261017-RELEASE`. Initialization is idempotent. To rotate the password, use `initialize-owner-auth.ps1 -ResetPassword`, then recreate Gateway. An explicitly configured `CYNEXUS_OWNER_PASSWORD_HASH` takes precedence, so update/unset that override when rotating.

UI remains on develop/HMR port 5174 unless a Release UI deployment is requested. Both Vite and Release Nginx preserve the original Host and send /api and /data-api through Gateway. Never point external tunnels directly at backend ports, Nacos, Redis, data executor or other infrastructure. The only public health exception is GET /actuator/health; other management endpoints require login. The exact GET/POST /api/wechat/callback endpoint retains its existing provider signature authentication, which rejects missing/invalid configuration and signatures before parsing or writing messages. This callback does not receive an owner identity assertion; all WeChat management routes still require owner login.

Cloudflare Tunnel, HTTPS and WAF remain in place. This code does not modify Cloudflare configuration. Local cloudflared metrics /config inspection confirms ui-dev.nooops.makeup targets develop port 5174, while cynexus.nooops.makeup and dev.nooops.makeup target Release UI port 5173. Following the user's explicit Release deployment request on 2026-10-17, Release UI was rebuilt/recreated as cynexus-ui:2.0.0-261017-RELEASE. The main hostname now returns the application directly without an Access redirect. Keep its tunnel target at http://localhost:5173; port 5174 is a separate development preview.

The auth endpoints are explicitly excluded from autonomous Agent tools because they handle secrets and session administration. Existing trusted-principal ownership remains unchanged after successful browser authentication.

## Browser behavior

Use the same normal Safari/Chrome profile and hostname. Closing the browser preserves remembered sessions; private mode, clearing site data, cookie eviction, a different embedded browser or password/session revocation can require login again. Login preserves the current URL, query and hash. No password or session credential is written to localStorage.
