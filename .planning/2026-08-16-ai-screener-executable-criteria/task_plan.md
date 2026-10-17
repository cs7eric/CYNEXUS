# AI Screener Executable Criteria

## Goal

Make the AI Screener follow a governed two-step flow: natural-language input is first converted into a visible, editable, executable criteria plan; screening runs only after the user applies that plan. Expand the typed screener catalogue beyond the existing basic and one price-volume pattern condition, without allowing arbitrary AI-generated queries.

## Phases

- [completed] 1. Audit the current AI intent-plan API, criteria schema, Screener UI flow, and market data coverage.
- [completed] 2. Define and implement bounded typed criteria for additional price action, volume, trend, and technical patterns supported by trusted data.
- [completed] 3. Render AI-proposed criteria as editable controls with assumptions, unsupported phrases, and explicit Apply/Run transitions.
- [completed] 4. Complete focused contract verification, runtime validation, and release-readiness checks.
- [completed] 5. Build, deploy affected Release services, and verify the end-to-end flow.
- [completed] 6. Make the Screener time-consistent: derive the history window from active conditions, enforce visible as-of semantics, and prevent mixed-date pattern results.
- [completed] 7. Add the investable-universe defaults (exclude ChiNext and STAR) across UI, typed Market API, strategy replay, and AI criteria.
- [completed] 8. Replace rigid volume-multiple interpretation with bounded AI-proposed ranges and add a data-backed candlestick-pattern catalogue.
- [completed] 9. Run focused verification, UI checks, version checks, and deploy the affected Release services.

## Constraints

- AI may propose only server-defined criteria; it cannot generate SQL or opaque rules.
- User edits must be reflected in the exact criteria used by the trusted Market Screener.
- Preserve current unrelated worktree changes.
- Any AI capability change must retain prompt, binding, provider routing, execution log, migration, and UI metadata consistency.
- Time-based filters must report one actual as-of trading date. A technical condition cannot silently use an older date than the displayed quote snapshot.
- The daily fact currently has only verified short historical coverage, so the first candlestick catalogue is restricted to one- and two-session forms and existing 20-session breakout/trend measures.

## Errors

| Error | Handling |
| --- | --- |
| Tool output truncates large source reads | Use focused symbol reads and record findings after each inspection batch. |
