# Task Plan: Portfolio import identity repair

## Goal

Show each imported transaction's name and code in the review table, and safely resolve a missing code only when the extracted security name has one exact active market-master match.

## Current Phase

Phase 4

## Phases

### Phase 1: Discovery

- [x] Inspect the latest import payloads and review UI.
- [x] Trace the import processing, review, and stock-master search paths.
- [x] Record the cause of missing symbol and hidden name.
- **Status:** complete

### Phase 2: Design

- [x] Keep unmatched/ambiguous names unresolved for explicit review.
- [x] Resolve only an exact, unique active stock-master match.
- [x] Keep name editable and visible beside the code in the review table.
- **Status:** complete

### Phase 3: Implementation

- [x] Add bounded name-to-symbol lookup in the market domain/infra layers.
- [x] Enrich review candidates without silently persisting inferred values.
- [x] Render and edit security name in the import review UI.
- **Status:** complete

### Phase 4: Verification and Release

- [x] Run focused backend tests, Maven compilation, UI lint/build, and version sync.
- [x] Rebuild/recreate the market and UI Release services.
- [x] Verify the affected review API and UI behavior.
- **Status:** complete

## Decisions

| Decision | Rationale |
|---|---|
| Use exact unique active master-data lookup only | Avoid silently assigning an incorrect code for a fuzzy or ambiguous name. |
| Return a review suggestion rather than mutate stored candidate data | The code was not visible in the uploaded screenshot and must remain reviewable before confirmation. |
| No autonomous Agent tool | This is an owner-bound, mutable import-review workflow; it is unsuitable for model-directed invocation. |

## Errors

| Error | Attempt | Resolution |
|---|---:|---|
| Planning helper could not start because WSL has no `/bin/bash` | 1 | Create the isolated `.planning` files with `apply_patch`. |
| Full `PortfolioImportConfirmationDomainServiceTest` has two pre-existing duplicate-time assertions that fail under the current local-time normalization | 1 | Run the new suggestion test method independently; do not alter unrelated duplicate semantics. |
| Deploying the changed services would package unrelated dirty market/UI worktree changes and run the release market-migration bundle | 1 | Do not deploy shared worktree changes without the user's explicit approval. |
| Release script parsed `market,ui` as one invalid service | 1 | Pass the PowerShell string-array parameter as `-Services market ui`. |
| Market image build could not fetch the `eclipse-temurin:21-jre-jammy` token from Docker Hub | 1 | Diagnose Docker Hub connectivity/cache before retrying the build. |
