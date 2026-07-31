# Findings & Decisions

## Requirements
- Add futures long/short position monitoring under the Market area.
- Existing AkShare dependency/capability exists, but the relevant API is not integrated.
- Add exchange data integration as part of the solution.
- Implement the feature in `cynexus-market` while respecting the repository's DDD layering.
- Persist required data in `cynexus_appl_market` using project table conventions.
- Add backend API integration in business-specific frontend service folders under `cynexus-ui/src/services`, not `lib/api.ts`.
- Add complete UI workflow: page, route, components, and project-consistent styling.
- Analyze the feature as a product user and from competitor-like functionality, covering product logic and business rules.
- If existing technology is insufficient, perform a POC or add appropriate framework/library.
- For implementation later: record progress and create a clear commit after each meaningful feature/sub-feature with completion state and self-test result.
- This turn is analysis only; implementation requires explicit user approval.

## Research Findings
- Worktree is already materially dirty from ongoing market and other changes. The analysis must treat those edits as user-owned and avoid reverting them.
- Existing planning sessions include market data foundation and market indicator work, so the new design should reuse their data-source, scheduler, schema, and UI conventions where applicable.
- Current repository instructions require Release-stack verification and prohibit starting the deprecated Dev application or local Java/Vite runtimes.
- Current repository instructions require synchronized Maven and UI version bumps after every functional implementation change.
- Current repository instructions require focused domain tests and prohibit new repository access/business rules in AppServices.
- FastCtx MCP tools requested by `AGENTS.md` were not available in this Codex tool catalog; read-only PowerShell inspection is being used as a fallback.
- The installed AkShare version is `1.18.46` and exposes futures-oriented functions including `futures_hold_pos_sina`, DCE/GFEX position-rank functions, live quote functions, daily history, settlement, and per-exchange contract metadata.
- Existing data architecture already separates provider adapters, pipelines, storage, jobs, and internal/query consumption. Existing margin work demonstrates official exchange HTTP first with AkShare fallback, scheduler registration, idempotent PostgreSQL persistence, and Java query APIs over the shared market schema.
- A futures feature should not make `cynexus-market` call AkShare directly: external data-source calls belong in `cynexus-data`, while `cynexus-market` should own query/domain/API/UI consumption against persisted market data.
- AkShare's `get_rank_sum(date, vars_list)` combines five exchanges' member rank data into per-contract top-5/top-10/top-15/top-20 aggregates. Its documented caveat is essential: SHFE/CFFEX aggregate within varieties/contracts, and DCE does not publish contract-level ranking in the same way, so the output is not a true all-market ranking.
- AkShare's raw rank functions expose long/short member names, open interest, day-over-day changes, rank, volume, and volume changes. This is enough for a daily member-position monitor and a normalized detail table.
- The installed AkShare wrapper has known upstream fragility: a live DCE position call returned a non-zip response (`BadZipFile`), CFFEX settlement returned an empty frame for the probe date, the generic settlement wrapper does not implement DCE despite its docstring listing it, and Sina real-time lookup requires Sina's symbol mapping rather than a generic code such as `IF`.
- A successful historical probe of `get_rank_sum('20250718', ['IF'])` returned five aggregate rows with 27 fields, including per-scope top-5/10/15/20 long/short open-interest and change values. This confirms the API shape but not current-day reliability.
- Official exchange rank data is daily/EOD; it cannot be presented as intraday member long/short positions. Intraday can provide quotes and aggregate open interest, but not the same member-level directional data. UI copy and API metadata must distinguish these modes.

## Technical Decisions
| Decision | Rationale |
|----------|-----------|
| No code changes before approval | Explicit user sequencing requirement |
| Continue discovery from existing `cynexus-market` and recent market-data work | Reduces divergence and identifies reusable contracts before proposing new tables or frameworks |
| Keep source ingestion in `cynexus-data`, query/business presentation in `cynexus-market` | Matches the repository's established source -> pipeline -> storage -> Java query architecture and prevents an AkShare/Python dependency from leaking into Java domain/application layers |
| Use normalized rank-type facts as the canonical persisted model | Volume, long, and short are independent exchange ranking lists; `rank_type` preserves that structure without incorrectly binding unrelated members at the same rank |
| Treat daily member position ranking as MVP; defer intraday quote/OI and directional signals | This matches what official exchange endpoints and AkShare actually provide and avoids presenting heuristics as source facts |
| Persist only the selected canonical provider rows in the query fact table | Prevents official and AkShare fallback rows from being double-counted while capture batches retain provider provenance and failure details |
| Reuse the existing self-selected list for futures monitoring | The backend model stores `targetType` as text; extending the shared contract to `future_instrument` covers both contract and variety scopes without a parallel watchlist table |

## Issues Encountered
| Issue | Resolution |
|-------|------------|
| Large pre-existing worktree diff | Scope all inspection carefully and do not clean or rewrite unrelated changes |
| One compact Python live-probe command had mismatched parentheses | Do not repeat it; use separate small probes if live endpoint validation is needed |
| AkShare `get_rank_sum` calls across all exchanges stop on DCE `BadZipFile` | Keep exchange calls isolated; one source failure must not hide other exchange results |
| PostgreSQL numeric `NaN` from SHFE source rows broke Java `BigDecimal` mapping | Filter non-finite decimals in the data domain and correct the 18 affected persisted rows |
| Default all-exchange query rejected the UI sentinel `ALL` | The UI omits `ALL` from the request; the domain keeps exchange filtering optional for the aggregate view |

## Final Runtime Coverage (2026-07-31)

- Trade date `2026-07-30` is the latest published date available in the shared runtime.
- Successful selected snapshots: CFFEX 22 instruments, SHFE 59, CZCE 125, GFEX 8, and INE 4; the combined query exposes 218 instruments and 12,890 member-rank facts.
- DCE is intentionally represented as `EMPTY` rather than fabricated or stale data. AkShare `futures_dce_position_rank` received HTTP 412 from the official batch-download endpoint; a separate DCE connector is required before DCE can be promoted to selected coverage.
- The UI now exposes coverage per exchange and renders common futures abbreviations with Chinese names, including `IF · 沪深300股指期货`.

## Resources
- Repository instructions: `E:/project-repo/cynexus/AGENTS.md`
- Target service: `E:/project-repo/cynexus/cynexus-market`
- Frontend: `E:/project-repo/cynexus/cynexus-ui`
- Database migrations: `E:/project-repo/cynexus/deploy/local/postgres/migrations`
- Existing planning context: `.planning/2026-07-28-market-data-foundation/`, `.planning/2026-07-27-market-indicator/`

## Visual/Browser Findings
- No browser or visual inspection performed yet.

## Proposed Product Scope

### MVP outcome
- Product definition: a daily/EOD exchange member-position monitor, not personal account positions and not intraday member positions.
- Target exchanges for the POC are CFFEX, SHFE, CZCE, DCE, GFEX, and INE. An exchange enters the MVP only after meeting the source-reliability gate; INE must remain a separate exchange code even when an upstream library groups its products with SHFE.
- Support both `CONTRACT` and `VARIETY` instrument scopes. DCE variety-level ranking must not be presented as contract-level data.
- Instrument scope, exchange, variety, contract, trade-date, and top-N filters (top 5, 10, 15, 20).
- Summary metrics: long-position sum, short-position sum, long-minus-short net, long/short ratio, day-over-day long/short changes, contract count, and source coverage.
- Instrument ranking table with long/short aggregates, net change, publication state, and a data-age/source badge.
- Instrument detail drawer with independent volume, long, and short member-rank tabs and rank-level changes.
- Historical trend for a selected instrument: top-N long/short position aggregates, net structure, and source/coverage state.
- Persisted monitoring loop: pin/unpin futures instruments and save the active monitor filters by extending the existing self-selected contract with `target_type=future_instrument` and a canonical symbol such as `CONTRACT:CFFEX:IF2609` or `VARIETY:DCE:M`.
- Clear definitions in UI: values are exchange-published member rank aggregates for the selected top-N scope, not the entire market's total long/short account positions.

### Follow-up scope
- Intraday quotes and aggregate open interest snapshots, visually separated from EOD member positions.
- Directional interpretation and server-side alert rules for ratio/net-change/price-OI combinations after quote/OI data is reliable.
- Cross-contract comparison and spread/term-structure views.
- Member-level history and provider comparison/reconciliation.
- Notifications through the existing notification infrastructure after alert semantics and ownership are agreed.

## Business Semantics

- `long_open_interest` and `short_open_interest` refer to the sum of the published member ranks within the selected top-N scope.
- `long_short_net = long_open_interest - short_open_interest` is a structure indicator, not a tradable signal and not the market's net position.
- `long_short_ratio = long_open_interest / nullif(short_open_interest, 0)` is only shown when the denominator is valid.
- `long_open_interest_chg` and `short_open_interest_chg` are changes against the previous trading day from the exchange report.
- MVP does not emit `long_add`, `short_add`, `short_cover`, or other directional labels because price and total-open-interest context is not part of the MVP source contract.
- `VOLUME`, `LONG`, and `SHORT` are independent rank lists. A member at rank 1 in one list must never be assumed to be the rank-1 member in another list.
- `scope_type=VARIETY` and `scope_type=CONTRACT` are different facts and must never be merged into one ranking or trend series.
- Missing provider data, non-trading dates, delayed publication, and partial exchange coverage must be represented as states. Empty data must not overwrite an existing valid snapshot.
- Source priority is explicit per exchange/date: official connector first, AkShare wrapper fallback, then no-data. Only one selected provider is written to the canonical fact set for that exchange/date; capture batches retain attempted providers, errors, and fallback reasons.
- The latest market view resolves one coherent trade date and reports exchange coverage for that date. It must not silently mix newer rows from one exchange with older rows from another.

## Proposed Data Model

### `futures_contract_catalog`
- Canonical contract identity and metadata: `exchange_code`, `variety_code`, `contract_code`, `contract_name`, `product_name`, `contract_month`, `multiplier`, `price_tick`, `currency`, `status`, `selected_source`, `extra`, timestamps, soft-delete.
- Active uniqueness: `exchange_code + contract_code`; provider identity is provenance, not part of the contract identity.

### `futures_position_capture_batch`
- Audit and completeness boundary: `id`, `trade_date`, `exchange_code`, `provider`, `status` (`RUNNING`, `SUCCEEDED`, `PARTIAL`, `FAILED`, `EMPTY`), `selected`, expected/actual instrument counts, row count, started/completed timestamps, retry count, warnings, error, and `extra`.
- At most one selected publishable batch (`SUCCEEDED` or explicitly accepted `PARTIAL`) per `trade_date + exchange_code`. Batch selection and canonical fact replacement occur atomically.

### `futures_position_member_rank`
- Canonical fact: `trade_date`, `exchange_code`, `scope_type` (`CONTRACT`/`VARIETY`), `instrument_code`, `variety_code`, optional `contract_code`, `rank_type` (`VOLUME`/`LONG`/`SHORT`), `rank_no`, `member_name`, `rank_value`, `rank_change`, `selected_source`, `capture_batch_id`, `extra`, timestamps, soft-delete.
- Active uniqueness: `trade_date + exchange_code + scope_type + instrument_code + rank_type + rank_no`. `selected_source` is intentionally excluded so fallback data cannot coexist as a second canonical fact.
- AkShare synthetic total rows such as `rank=999` are excluded from member facts.

### Top-N aggregation
- MVP calculates top-5/10/15/20 summaries from canonical member facts in PostgreSQL queries/domain results. No derived scope table is created until measured query latency justifies it.
- If materialization is later required, it must be keyed to the selected `capture_batch_id` and a calculation version so it cannot drift from its source facts.

### Monitoring persistence
- Reuse `self_selected_list_items`; extend API/UI constants from the current stock/HK/ETF/index/other set to `future_instrument`.
- Store `market=exchange_code`, a scope-qualified symbol (`CONTRACT:<exchange>:<contract>` or `VARIETY:<exchange>:<variety>`), and display metadata. Stock-only warmup jobs must not run for futures items.

### Storage choice
- PostgreSQL in `cynexus_appl_market` is sufficient for EOD rank facts, capture batches, and query-time Top-N aggregation. Expected volume is modest compared with the existing daily stock data.
- ClickHouse is only justified for high-frequency quote/OI history or tick-level data; it is not needed for the initial daily member-position workflow.

## Proposed Service Flow

```text
Official exchange endpoints / AkShare
        -> cynexus-data source adapters
        -> canonical futures domain normalization
        -> sync_futures_position pipeline + idempotent PG storage
        -> core scheduler / trigger API
        -> cynexus-market domain repository
        -> thin AppService DTO mapping
        -> REST API
        -> UI service + Market page
```

### `cynexus-data`
- Add `sources/akshare/futures_position.py` and explicit official adapters under `sources/exchange_futures/` for each target exchange. Each adapter returns the same canonical rank contract while preserving exchange-specific scope semantics.
- Add canonical normalization under `domain/futures/`.
- Add `pipelines/futures/sync_position.py` and a registered `sync_futures_position` job.
- Add job parameters for `trade_date`, `exchanges`, `varieties`, `contracts`, `providers`, `create_table`, `allow_empty_replace`, and `dry_run`. Quote parameters are excluded from the MVP job contract.
- Provider selection is per exchange/date. Official data is attempted first; AkShare is used only when the official adapter fails validation or returns no publishable batch.
- Use bounded retries, per-exchange results, atomic canonical replacement, and no empty overwrite. A partially available market date is retained as `PARTIAL` with explicit coverage.
- Resolve futures trading dates through a futures calendar and exchange publication watermark. Night-session timestamps are assigned to the correct next trading date.
- Schedule each exchange after its POC-observed publication window, retry until a configured cutoff, then finalize coverage. Add a rate-limited date-range backfill mode.

### `cynexus-market`
- Domain owns validation, allowed sort/filter values, coherent-date fallback, scope semantics, Top-N calculation, source/coverage warnings, and monitor-item identity. MVP has no directional signal policy.
- Infrastructure owns MyBatis mapper/PO/converters and PostgreSQL access.
- Application owns request parsing and BO-to-DTO mapping only.
- API should expose at minimum:
  - `GET /api/market/futures-position/latest`
  - `GET /api/market/futures-position/instruments`
  - `GET /api/market/futures-position/instruments/{scopeType}/{exchangeCode}/{instrumentCode}/history`
  - `GET /api/market/futures-position/instruments/{scopeType}/{exchangeCode}/{instrumentCode}/members`
  - `GET /api/market/futures-position/coverage`
- Ranking/list APIs use server-side paging, filter and sort whitelists. All responses return `tradeDate`, `requestedTradeDate`, `fallbackTradeDate`, `scopeType`, selected source, update time, coverage, and warnings.
- Extend the existing self-selected API contract with `target_type=future_instrument`; do not add a second futures-only watchlist repository.

### Frontend
- Add `cynexus-ui/src/services/market/futures-position.ts`; do not add these calls to `lib/api.ts`.
- Add a semantic page folder such as `src/views/market/market-futures-position/` with header, saved filter bar, summary panel, pinned-instrument view, instrument table, trend panel, independent rank-type drawer, and coverage/source panel.
- Add route `/market/futures-position` and a Market navigation entry.
- Use the existing Market page shell, tables, tabs, drawers, loading/empty/error states, and icon-button conventions.
- Default to the latest coherent trade date, show an explicit EOD/partial badge, preserve filters in URL/local preferences, and avoid a misleading one-minute auto-refresh. Manual refresh is enough for MVP.

## POC and Acceptance Gates

1. Validate 20 consecutive trading dates for each target exchange through the direct official adapter and AkShare fallback. Include at least two varieties, main/non-main instruments, a near-expiry instrument, a non-trading date, and INE as a separate source path.
2. Record response shape, scope type, instrument count, rank count, empty/blocked rate, elapsed time, retry behavior, member-name encoding, and completeness of value/change fields.
3. Compare normalized Top-N totals for sampled instruments where official and AkShare both succeed. Any unexplained difference above 0.5% blocks provider promotion.
4. Official-primary success must be at least 95% across the sample; official-plus-fallback success must be at least 99%. An exchange below 90% combined success is excluded from MVP until repaired.
5. Core rank fields must be at least 99% complete for selected successful batches. No empty or partial response may replace a previously selected successful batch.
6. Verify futures trade-date/night-session attribution, publication windows, endpoint terms/rate limits, and Release-container network reachability.
7. Do not begin schema or UI implementation until the POC report records supported exchanges, supported scopes, exclusions, and acceptance results.

## Implementation and Commit Plan After Approval

- Commit 1: source POC, exchange coverage report, and canonical rank contract; include fixtures/source tests. This is non-functional research and does not bump the application version.
- Commit 2: PostgreSQL migration, capture batches, canonical storage, `sync_futures_position`, scheduler registration, and Release migration wiring; include idempotency, fallback, atomicity, and empty-data tests. Synchronize Maven/UI versions in this commit.
- Commit 3: Java domain/repository/infra query path and REST DTOs; include domain semantics, coherent-date, provider-selection, pagination, and query tests. Synchronize Maven/UI versions in this commit.
- Commit 4: UI service, route, page, saved filters, pinned instruments, and coverage/freshness states; extend self-selected `future_instrument` support and add UI checks. Synchronize Maven/UI versions in this commit.
- Commit 5: member detail and historical trend workflow, full Release-stack verification, and regression evidence. Synchronize versions only if this commit contains functional code changes or fixes.
- Before every commit, inspect the staged file list and use path-scoped staging so the current unrelated worktree changes are not included. Every commit message/body records completed scope, status, and self-test results.

## Baseline Risks Before Implementation

- The current worktree contains extensive unrelated/in-progress edits. Future commits must be scoped carefully and should not include those changes accidentally.
- The current baseline is already version-inconsistent in the inspected files: root `pom.xml` and UI package are `1.1.354-SNAPSHOT`, while several `cynexus-market` child parent POMs still show `1.1.353-SNAPSHOT`. This is pre-existing and must be resolved deliberately as part of the eventual version-sync step, not silently overwritten during analysis.
- AkShare's `get_rank_sum` is a useful adapter but not a durable SLA. Official exchange connectors, provider priority, and health metadata are required for a production workflow.

## Implementation POC Update (2026-07-30)

- Direct AkShare probes on `2025-07-18` returned member-ranking tables for CFFEX (`IF`, 4 contracts), SHFE (`RB`, 4 contracts), and CZCE (104 product/contract tables). GFEX returned an empty result for sampled `PG`. DCE failed with `BadZipFile` because its upstream batch-download response was not a zip archive.
- The first 20-date aggregate probe called AkShare's combined `get_rank_sum` for all exchanges; every date was aborted by the DCE exception. This is a source-isolation failure in the wrapper, not evidence that CFFEX/SHFE/CZCE are unavailable.
- Added a provider boundary at `cynexus_data.sources.akshare.futures_position` and a domain normalization contract that expands each exchange report row into independent `VOLUME`, `LONG`, and `SHORT` facts and excludes synthetic rank `999` totals.
- POC gate status: not yet accepted for production ingestion. CFFEX/SHFE/CZCE still need the required 20-date per-exchange sample and official-source comparison; DCE is currently excluded pending a repaired official/fallback adapter.
