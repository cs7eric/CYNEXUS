# Agent UI Release Workflow Acceptance

## Scope and runtime

- Date: 2026-07-22 (Asia/Shanghai)
- Independent workspace: `E:\project-repo\cynexus-agent-ui-current`
- Branch/commit at test start: `codex/agent-ui-browser-acceptance` / `0bbe54573139b26befb9b002bfe38ff61538d93f`
- Release under test at the start of the browser run: `1.1.177-260722-RELEASE`
- Release UI: `http://127.0.0.1:5173/ai/agent`
- Browser: Playwright Chromium 1.61.1, desktop `1440x900`, mobile target `390x844`
- No Java, UI source, Maven, version, database, or deployment files were changed. This document is the only requested repository change.

The Release stack was already online through the documented versioned Docker images. At the beginning of the run, `cynexus-release-{ui,ai,gateway,core,market,knowledgebase}` all reported `1.1.177-260722-RELEASE`. The shared PostgreSQL, Redis, ClickHouse, Nacos, and Data Executor containers were left running.

## Gate results

| Workflow | Release browser result | Evidence / limitation |
| --- | --- | --- |
| Empty state | Pass | Agent page loaded and `Start with a question` was visible. |
| History | Pass for read/reopen surface | `GET /api/ai/agent/conversations?limit=30` returned HTTP 200 and the page rendered recent conversations. Full new-run reopen was not counted as real-provider success because the new run did not reach a terminal answer. |
| Thinking | Observed | The real submit entered the loading path; the Stop/Thinking state was available while the provider request was pending. |
| Streaming | Blocked | No successful real token stream was produced. The AI container recorded `HttpTimeoutException` for `gpt-5.4-mini`, followed by a TLS handshake retry. |
| Source / evidence | Blocked for new real run | Existing persisted history displayed source-bearing answers, but the new Release request did not finish, so no new source card is claimed for this run. |
| Export | Blocked for new real run | The Export action exists, but a new completed Release run was unavailable to export. |
| Retry | Blocked for new real run | Requires a persisted terminal run from the new browser flow; the provider did not supply one. |
| Regenerate | Blocked for new real run | Same provider prerequisite as Retry. |
| Follow-up | Blocked for new real run | Requires a completed conversation turn; the real first turn remained provider-blocked. |
| Stop / cancel | Not completed as real Release evidence | The pending provider request exposed the Stop path, but no terminal `CANCELLED` result was captured before the Release stack was replaced. |
| Error | Pass for UI error surface only | The UI renders `Agent unavailable` for a failed request. A provider-originated terminal error could not be isolated from the timeout/restart window. |
| Desktop layout | Pass | Route loaded at `1440x900`; no horizontal overflow was observed in the Release smoke page. |
| Mobile layout | Not counted for Release 1.1.177 | The mobile viewport was prepared, but the run was interrupted by the Release/provider window before a complete mobile workflow could be captured. |

## API and identity checks

- `GET /api/ai/agent/conversations?limit=30` through the Release UI proxy returned HTTP 200.
- `GET /api/ai/agent/tools` through the Release UI proxy returned HTTP 200 and returned the available Agent tool catalog.
- The browser did not supply client identity headers. The Release Gateway supplied the trusted identity assertion; no `401 UNAUTHORIZED` identity failure was observed on these 1.1.177 requests.
- The real stream reached the AI runtime and attempted the configured provider/model (`custom` provider, `gpt-5.4-mini`). This confirms the request was not stopped at the browser or identity gate.

## Provider block, recorded exactly

The real Release request was not successful. AI logs recorded:

```text
OpenAiCompatibleChatAdapter: AI provider custom agent failed for model gpt-5.4-mini at https://am.xtyooo.top/v1/chat/completions: HttpTimeoutException
Retrying AI provider stream after TLS handshake failure (attempt 2/2)
```

Therefore this acceptance does not claim real-provider success for streaming, sources, export, retry, regenerate, follow-up, or cancellation. The correct disposition is provider-blocked, not a fabricated pass.

During the verification window the running Release stack was subsequently replaced by `1.1.178-260722-RELEASE` outside this documentation-only change. No 1.1.178 results are included in this 1.1.177 acceptance record.

## Existing UI-only regression evidence

The preceding isolated UI browser evidence remains useful for the rendering contract, but is explicitly not Release-provider evidence:

- The current UI build's deterministic request fixtures covered thinking, stream completion, source rendering, export download, Retry, Regenerate, follow-up, Stop/error rendering, history reopen, and no-console-error checks.
- Fixture layout checks passed at `1440x900` and `390x844`; mobile `scrollWidth` equaled `clientWidth`, and the composer remained within the viewport.
- Fixture screenshots were captured outside Git at `C:\Users\Cs7er\AppData\Local\Temp\cynexus-agent-desktop-fixture.png` and `C:\Users\Cs7er\AppData\Local\Temp\cynexus-agent-mobile-fixture.png`.

Those fixture results are retained as UI regression evidence only. They do not close the Release real-provider gate above.
