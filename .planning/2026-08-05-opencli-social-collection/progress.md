# Progress: OpenCLI Social Collection Integration

## Session Log

- Confirmed the requested OpenCLI command family is from `@jackwener/opencli`, not the unrelated PyPI package named `opencli`.
- Confirmed the installed OpenCLI command surface and identified the Browser Bridge connection requirement.
- Created an isolated task plan because the repository root planning files belong to an unrelated completed UI task.
- Inspected OpenCLI package source: XHS URLs preserve `xsec_token`; Douyin collection is `user-videos` plus embedded top comments; the local agent needs a subprocess-backed login session instead of the old QR screenshot flow.
- Added the OpenCLI client, subprocess login manager, backend selection, raw payload retention for Douyin comment refresh, parser support for tokenized XHS profile-note URLs, and UI instructions for Chrome Browser Bridge.
- `compileall` passed. The first pytest invocation failed during collection because `PYTHONPATH` was not set from the repository root; no test assertions ran.
- Focused Python tests now pass: 7 tests. UI targeted ESLint and `pnpm build` pass.
- Restarted the local agent with `CYNEXUS_SOCIAL_BACKEND=opencli`; health check passes. The login-session API now starts an OpenCLI login process and returns the Chrome/Bridge instructions. The test session was explicitly closed after verification.
- Corrected the daily scope: discovery remains strict on Asia/Shanghai publish date, comment sync now accepts only today's posts, and the local posts API/UI default to a calendar-day query instead of the previous 30-day discovered window.
- Reduced the default XHS probe batch to 15 posts and the detail-page wait to 2 seconds to avoid repeatedly probing large historical lists.
- Focused Python tests and compileall pass. The social-monitoring UI targeted ESLint and production build pass; full UI lint still reports unrelated pre-existing repository errors.
- Deployed the UI Release image `1.1.398-260805-RELEASE`; `/local/social/posts?platform=xiaohongshu&today_only=true` currently returns zero rows, and both Release UI and local agent health checks pass.
- Confirmed the active runtime is CDP (`19221` Xiaohongshu / `19222` Douyin), not OpenCLI. The CDP XHS collector was extracting only visible comments and never clicking `.show-more` reply groups.
- Added repeated XHS reply-group expansion before each DOM extraction. A live CDP check on a note with 87 comments returned 82 text comments, including 23 nested replies linked to 8 parent comments; focused CDP/parser tests pass.
