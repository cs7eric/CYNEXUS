# Data Collector Navigation

## Objective

Move Market Indicator into a new Data Collector navigation group and add a dedicated limit-board session monitor page backed by the persisted session summaries.

## Phases

- [completed] Inspect existing UI/API/data contracts and preserve unrelated worktree changes
- [completed] Migrate Market Indicator page folders and update route/navigation references
- [completed] Add limit-board summary API, UI service, and monitor page
- [completed] Update frontend design documentation and run focused verification

## Errors Encountered

| Error | Attempt | Resolution |
| --- | --- | --- |
| FastCtx/CodeGraph MCP tools are unavailable in this session | 1 | Used local PowerShell inspection and `rg` fallback as permitted by project instructions |
