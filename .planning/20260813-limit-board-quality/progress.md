# Progress

- 2026-08-13: Reproduced the abnormal data from PostgreSQL and verified that persisted rows have sparse samples and large gaps.
- 2026-08-13: Confirmed CodeGraph impact for the existing summary query and UI consumer.
- 2026-08-13: Added trusted-gap duration semantics, quality metadata, and regression coverage.
- 2026-08-13: Added a sequential limit-board session job and migration; the standalone five-second scheduler job is disabled.
- 2026-08-13: Added the persisted sealed-amount trend chart with gap/coverage warnings to the monitor UI.
- 2026-08-13: Rebuilt the data executor, Core, and Market Release services; verified the summary API returns trend points and conservative historical duration estimates.
- 2026-08-13: Rebuilt the data executor after the final duration semantics change; the container is healthy and the session job dry-run is registered successfully.
- 2026-08-13: Renamed the UI metric to `已确认封板时长` and clarified that intervals outside observation gaps are excluded; UI build succeeded, but the Release image is still blocked by Docker Hub nginx token/network access.
- 2026-08-13: Wired the existing 30-second full-market refresh to publish limit-up/down discoveries into the limit-board Redis session. Removed the obsolete local discovery interval from the session scheduler configuration, excluded the priority-only refresh from publishing, applied market migrations, rebuilt the data executor, and verified its health plus a session dry-run. Focused tests: 26 passed. Live discovery verification is pending market hours.
- 2026-08-13: Added compact persisted trend arrays (`smallint[]` offsets + `bigint[]` amounts with one origin timestamp), API normalization, and UI chart parsing with legacy fallback. PostgreSQL columns and alignment constraint verified; focused data tests passed previously (18 trend-related checks included), UI ESLint/build passed, Market compile passed, data executor and Market Release were rebuilt and are healthy, and the 3-second session dry-run succeeded.
