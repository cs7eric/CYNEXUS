# Progress

- Gateway compile passed.
- Focused frontend ESLint passed.
- Security regression tests, build and deployment completed.
- Initial test run: one counter assertion expected 1 authentication lookup, but same-origin CSRF rejection correctly authenticates before checking its token (2 lookups total). Corrected assertion.
- Local ignored, ACL-restricted owner credential configuration initialized without printing secrets.
- OwnerAuthTest: 7 tests passed. Workspace regression tests: 7 passed. UI build passed with existing font/chunk warnings. Source versions match 2.0.0-SNAPSHOT.
- Gateway-only Release deployment started (2.0.0-261017-RELEASE); UI stays develop.
- Browser checks passed through independent-device logout and logout-all. Final cookie assertion was case sensitive (Netty emits HTTPOnly); corrected to case-insensitive, revoked that test credential, continuing final tests without repeating completed scenarios or exhausting the login limiter.
- Remaining browser tests passed: temporary-cookie TTL, missing-CSRF denial, secure tunnel cookie, server expiry returns to login. Desktop/mobile screenshots inspected; no overflow.
- Final local UI checks passed: Core, AI config, Knowledge Base and Market API reads return200 after login; logout cancellation preserves session and confirmation revokes it. Authenticated data-api reaches Gateway route and returns500 because executor remains intentionally stopped.
- Preserved exact WeChat GET/POST callback with downstream signature verification; forged or unauthenticated callbacks receive no owner identity assertion. Added two focused regression tests; all9 OwnerAuthTest tests pass.
- Rebuilt/recreated Gateway with latest callback/identity changes at 2.0.0-261017-RELEASE. Gateway healthUP; UI develop remains PID28852/port5174, Release UI remains stopped.
- Real public HTTPS mobile checks passed on ui-dev.nooops.makeup: login, reload, Secure/HttpOnly/Strict persistent cookie, anonymous401 vs authenticated200, CSRF logout. Eleventh login returned429/Retry-After900; removed only the two setup-test rate keys after verifying all11 attempts were from setup tests.
- Existing67 Market jobs remain disabled; ClickHouse/Data Executor remain stopped. No Cloudflare configuration changed. Main Cynexus hostname still targets5173 and redirects to Access.
- Final UI build and focused ESLint/TypeScript passed. Version synchronization passed2.0.0-SNAPSHOT. No commit/push requested or performed.

## Release UI Deployment

- User authorized Release UI deployment; current mode recorded as develop before deployment. Release UI was stopped, Vite5174 and Gateway18000 were running.
- Version verification passed2.0.0-SNAPSHOT. UI-only Release build/recreate started with start-release.ps1 -Services ui -Build -Version2.0.0-261017-RELEASE; backend services and shared infrastructure remain untouched.
- Before UI startup, both main Cynexus and dev public hostnames return502, with no Access redirect. Tunnel configuration stays on5173.
- UI build and image build passed; cynexus-release-ui is running cynexus-ui:2.0.0-261017-RELEASE on5173. Nginx configuration test passed. Main public hostname now returns200 without an Access redirect. Browser session verification in progress.
- First Release browser harness verified local login, persistent cookies and browser restoration, then waited for UI logout after calling the raw logout HTTP endpoint. Corrected harness to use the actual user-menu logout command, which also updates the UI state. No application change was needed.
- Final Playwright checks passed on localhost5173 desktop and https://cynexus.nooops.makeup mobile: built production assets, login preserving URL, remembered device, reload and restored browser context, authenticated API200, anonymous API/stream/data-api401, actual user-menu logout. No page errors. Mobile login/sidebar screenshots inspected.
- Final runtime: Release UI and Gateway both2.0.0-261017-RELEASE, Tunnel unchanged on5173. Vite5174 remains an existing separate preview. Backend containers were not recreated; PostgreSQL/Redis/RabbitMQ healthy, Nacos running. ClickHouse/Data Executor remain stopped and67/70 scheduler jobs remain disabled.
- Final version synchronization and git diff --check passed. Existing font/bundle build warnings remain. No application source changes, commit, push or Cloudflare configuration writes were performed for this deployment follow-up.
