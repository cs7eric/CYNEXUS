# Progress

- 2026-07-29: Inspected project rules, current worktree, scheduler API, job names, categories, and migration patterns.
- 2026-07-29: Confirmed source/UI/Maven versions are currently synchronized at `1.1.326-SNAPSHOT` before this change.
- 2026-07-29: Added the idempotent catalog migration, normalized all 59 job titles, rebuilt each active job into one primary business category, and added a UI fallback for legacy parenthetical titles.
- 2026-07-29: Bumped synchronized Maven/UI versions to `1.1.327-SNAPSHOT` and registered the migration in the Release market migration bundle.
- 2026-07-29: `npm run build` and `mvn -DskipTests compile` passed. Release rebuilt/recreated successfully as `1.1.327-260729-RELEASE`; all six application containers are running.
- 2026-07-29: Verified UI HTTP 200, scheduler API 59 jobs, zero parenthetical titles, zero uncategorized active jobs, and 12 refined categories.
- 2026-07-29: Replaced the long horizontal category tab strip with a compact category selector; category editing is now shown only in a bounded management row when Edit is enabled. Bumped synchronized source/UI versions to `1.1.328-SNAPSHOT`.
- 2026-07-29: Fixed category selection so choosing `全部` (and any category) explicitly updates the controlled scheduler view; rebuilt and redeployed Release after verification.
- 2026-07-29: Restored all category labels as a responsive wrapped navigation list so every category remains directly discoverable without horizontal scrolling. Bumped synchronized source/UI versions to `1.1.329-SNAPSHOT`.
- 2026-07-29: Limited the visible category navigation to six entries including `全部`; remaining categories are available from a compact `更多分类` menu, which also reflects the selected hidden category. Bumped synchronized source/UI versions to `1.1.333-SNAPSHOT`.
