# Portfolio trade deduplication

## Goal

Prevent confirmed portfolio trades from being inserted more than once for the same symbol and traded time. Duplicate OCR candidates must remain visible in the confirm flow as invalid instead of making confirmation fail.

## Phases

- [complete] Inspect the import candidate lifecycle, persistence, and confirm flow.
- [complete] Implement domain-owned duplicate detection and persistence safeguards.
- [complete] Map duplicate candidates to invalid status and expose the reason in confirm responses/UI.
- [complete] Add focused tests and run backend/UI verification.
- [complete] Deploy and verify the Release stack if the functional checks pass.

## Decisions

- Deduplication key: normalized symbol + tradedAt.
- Existing persisted trades are authoritative; duplicates in a new import are invalid candidates and are not inserted.
- Confirmation remains successful when invalid candidates are present, as long as there is at least one valid candidate to confirm.

## Errors Encountered

| Error | Attempt | Resolution |
|---|---:|---|
| Maven treated the unquoted `-Dsurefire.failIfNoSpecifiedTests=false` argument as a lifecycle phase | 1 | Re-run with quoted `-D...` arguments |
