# Owner Authentication

- [x] Inspect Gateway, UI proxies, Redis and current runtime.
- [x] Implement configured owner password and opaque remembered-device sessions.
- [x] Implement login boundary, CSRF transport, logout and logout-all.
- [x] Security tests and production builds.
- [x] Initialize local credentials, deploy Gateway, verify desktop/mobile and runtime invariants.
- [x] Document operation and final checks.

Source version remains synchronized at 2.0.0-SNAPSHOT.

## Release UI Follow-Up

User explicitly approved deployment of the current UI to Release, keeping Tunnel on5173.

- [x] Record pre-deployment environment: develop/HMR5174 online; Release UI5173 stopped; Gateway2.0.0-261017-RELEASE online.
- [x] Build/recreate only Release UI with the same2.0.0-261017-RELEASE version.
- [x] Verify local5173 and main public hostname, browser sessions, assets and API protection.
- [x] Record final runtime and checks.
