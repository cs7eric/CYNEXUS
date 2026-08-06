# ETF Redesign Plan

## Goal
Upgrade `/market/etf` into a scan-first ETF research workbench without breaking the existing API contract.

## Phases
- [completed] Audit current ETF UI, data contract, shell conventions, and competitor patterns
- [completed] Define the redesign direction and prioritized capability gaps
- [completed] Implement the ETF workbench UI in the existing React/Tailwind stack
- [completed] Verify build, responsive layout, and changed-file scope
- [completed] Synchronize Maven/UI versions and record release verification limits

## Implemented P0/P1
- Opportunity screening for active volume, inflow, premium anomalies, and share/price divergence.
- Compact research table with identity, price, change, volume, premium, turnover, share delta, and main flow.
- 5D/10D trend range switch and stronger theme activity bars.
- Detail sheet retains order-size flow and subscription proxy context.

## Full Workbench Redesign
- [completed] Replace the stacked dashboard with a table-first two-pane research workspace
- [completed] Implement persistent selected-instrument context and insight tabs
- [completed] Add sortable research columns and saved local view state
- [completed] Verify responsive behavior, lint, build, and Release stack

## Decisions
- Preserve the current `MarketEtfResponse` contract; add no backend capability unless required by the existing payload.
- Use the established neutral workbench tokens and A-share red-up/green-down convention.
- Favor progressive disclosure and scanability over adding more top-level cards.
- Keep long-term ETF quality data (fees, scale, tracking quality, holdings, dividends) as a backend data-expansion phase instead of presenting invented proxy values.

## Errors Encountered
| Error | Attempt | Resolution |
|---|---:|---|
| ui-ux-pro-max search script missing | 1 | Use the installed guidance plus repository/design context instead |
| impeccable context path missing under repo | 1 | Run the bundled global skill path |
| Full UI build briefly failed in a concurrently edited futures-position component | 1 | The concurrent edit completed; rerun passed without changing that feature |
| Release version advanced during verification | 1 | Rechecked all 41 POM files and UI version, then rebuilt the matching `1.1.385-260731-RELEASE` UI image |
