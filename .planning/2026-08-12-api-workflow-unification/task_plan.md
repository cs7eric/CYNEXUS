# Unified API Workflow Process History

## Goal

Normalize URL transcription, AI trade import, and finance AI capture behind one API workflow process-history contract. The Settings API Workflows page must show only execution history and link to the existing business-result pages.

## Phases

- [completed] Inspect existing lifecycle, persistence, routes, and version/status state
- [completed] Add shared workflow history domain, persistence, API, and workflow projections
- [completed] Refactor the Settings workflow history UI
- [completed] Add focused tests, migration/docs, and verify versions
- [completed] Build and deploy affected Release services, then commit the task changes

## Decisions

- Keep existing business tables and result pages intact.
- Use a platform-level process and event projection with stable workflow type/status/event enums.
- Expose result navigation metadata only; never expose transcription text, trade candidates, or ledger details in the workflow history response.
- Preserve the existing `artifacts/` directory and do not add it to git.

## Errors Encountered

| Error | Attempt | Resolution |
|---|---:|---|
| planning-with-files skill path from repository instructions did not exist | 1 | Loaded the installed skill from `C:\Users\Cs7er\.agents\skills` |
| direct single-module Maven tests could not resolve reactor dependencies | 1 | Re-run with `-am` so dependencies are built in the same reactor |
