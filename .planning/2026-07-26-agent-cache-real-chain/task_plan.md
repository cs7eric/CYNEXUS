# Agent cache real-chain investigation

## Goal
Find why the real Agent Responses requests remain fixed at 6,656 cached tokens and become unusable after the second turn, then implement and deploy a verified fix against the real Agent path.

## Phases
- [complete] 1. Capture latest run messages, provider outputs, 502 evidence, and container state
- [complete] 2. Trace request construction and SSE response-state persistence end to end
- [complete] 3. Add safe prefix diagnostics and focused tests if existing evidence is insufficient
- [complete] 4. Fix the confirmed divergence or protocol defect
- [complete] 5. Synchronize versions, compile/test, commit, deploy Release, and verify with a real Agent run

### Current subphase
- [complete] Exclude failed-Run tails and collapse completed prior Runs into provider-safe visible history
- [complete] Keep native reasoning/function-call/Tool replay scoped to the active Run
- [complete] Verify a new Run in the same conversation succeeds and retains cache growth above the old 6,656 plateau

## Constraints
- Do not treat a simplified direct-provider demo as real Agent verification.
- Do not persist raw prompts or secrets in diagnostics.
- Preserve unrelated untracked planning directories.
- Use only the versioned Release stack for runtime verification.

## Errors Encountered
| Error | Attempt | Resolution |
| --- | --- | --- |
| Planning patch context mismatch | 1 | Re-read the file and used the full exact sentence. |
| PowerShell parsed the comma-separated Maven `-Dtest` value | 1 | Quote the entire `-Dtest=...` argument. |
| PowerShell stripped the `surefire` system-property prefix | 2 | Quote all Maven `-D...` arguments in this shell. |
| Initial health probes returned 502 at 19 seconds | 1 | Containers were still starting; wait for Spring startup and probe again. |
| Knowledge-base verification query had ambiguous `extra` | 1 | Qualify the column as `d.extra`. |
| Final cache-event verification used column `type` | 1 | Use the persisted column name `event_type`; runtime SSE field remains `type`. |
