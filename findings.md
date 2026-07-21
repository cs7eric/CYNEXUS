# Findings

## Current Task: EastMoney Market Fund Flow

- The manual UI copies five rows from the EastMoney market fund-flow page, but the same data is available from `push2his.eastmoney.com/api/qt/stock/fflow/daykline/get`.
- The market-wide request uses `secid=1.000001`, `secid2=0.399001`, `klt=101`, and returns daily rows containing main, super-large, large, medium, and small net flows plus ratios and index values.
- `AkshareClient.fetch_market_overview()` already calls this endpoint indirectly, but only summarizes the latest row; it does not persist history.
- Existing scheduler category code is `data_collection` with label `数据采集`.
- The data executor container has no source bind mount, so it was rebuilt after the code change; its `/internal/jobs` registry now exposes `sync_market_fund_flow`.
- EastMoney responses are intermittently closed by the upstream/network path. The source uses the existing retry/direct-session pattern, but live availability remains an external dependency.

## EastMoney Fallback Follow-up

- `EastMoneyBrowserSession` currently only refreshes cookies; it does not issue the market fund-flow request from a browser context.
- `run_pipeline_job` currently assumes every pipeline payload is successful, so a pipeline-level stale/error result needs explicit mapping into `JobResult` to become an operational alert.
- `market_overview_snapshots.extra` is JSONB and can carry source-attempt metadata without changing the table schema.
- A failed fetch must not call `upsert_fields`; the repository can update only the latest existing snapshot's metadata so prior numeric values remain unchanged.
- The latest overview row can exist without fund-flow values, so failure metadata must select the latest row with `main_net_inflow IS NOT NULL`; otherwise it falsely reports a non-flow date as the last successful value.
- In the current container network, both requests were closed upstream: HTTP reported `RemoteDisconnected` and Playwright reported `socket hang up`. This confirms the fallback path is operational even though the external source remains unavailable from that egress.

## Market Information Refresh Error

- `market_information_media_refresh` and `market_information_official_refresh` both dispatch to `sync_a_stock_dataset`.
- `extract_and_persist()` returns status counters such as `ready`, `failed`, `summary_only`, and `skipped_cached`, but no `rows_normalized` key.
- The pipeline adds that return value directly to `dataset_results`, then totals `item["rows_normalized"]`; this causes the observed `KeyError` after content extraction.
- The fix treats content extraction as post-processing with `rows_normalized: 0`; source dataset counts remain the only contributor to `rows_read`, while aggregation now safely handles derived result entries.

## Application Analysis Fund Flow

- The existing 120-day action was inside `FundFlowTrendChart`, which returns `null` when no history exists; that made the action disappear exactly when it was needed.
- `backfillStockFundFlowHistory(symbol)` already triggers `sync_a_stock_dataset` with `datasets: ["eastmoney_fund_flow_120d"]` and `fund_flow_mode: "history"`; no backend endpoint is needed for this UI fix.

- `LangfuseGenerationReporter` already sends `statusMessage` with the provider error, truncated to 800 characters.
- `LangfuseUsageRepositoryImpl` already parses `statusMessage` into the domain observation and the backend DTO returns it.
- The AI Usage table currently only uses `statusMessage` as the status badge `title`, so the reason is hidden behind hover.
- Current runtime observations include DeepSeek failures with `402 Insufficient Balance`; Custom provider observations succeed.
- Provider pricing values are currently zero, which is unrelated to failure visibility.
- The UI change will render `statusMessage` under failed rows instead of relying only on a hover title.

## Trigger Audit

- `market_information_pipeline_refresh` runs every 15 minutes during its configured window.
- Before the fix, `refreshPipeline()` excluded only `PENDING` and `RUNNING` tasks. Historical `FAILED` tasks were eligible for insertion again on every refresh.
- Database evidence before the fix: 880 `EVENT_ANALYSIS` task rows for 242 clusters; 638 failed rows; one cluster had 60 task rows.
- The worker retries each task up to 3 times. A single task can also produce multiple provider calls due to JSON repair and fallback handling. The execution log contained 2,916 event-analysis calls for 880 task rows.
- 320 failed task rows were `Event has no source content`; these were orphaned clusters with no current event members and should never have been enqueued.
- The fix makes automatic pipeline enqueue idempotent for all existing task history by cluster/content hash and requires at least one event member. Manual force analysis remains available.
- Failed AI event-analysis tasks are recovered once into an `automatic-retry` cycle. The cycle resets the attempt counter and uses the existing `max_attempts = 3` limit; source-less failures, successful analyses, and active duplicate tasks are excluded.
- Dev runtime verification on 2026-07-16: 38 eligible failures entered the retry queue, 0 source-less failures entered it, and 0 duplicate active task groups were present.
- Market Information previously generated event and brief prompts inside `MarketInformationAiWorker`; it only used usage binding parameters for provider/model selection and prompt version labels. It did not query `ai_prompt`.
- The worker now resolves active prompts by capability and binding `params.prompt_version`, renders `{{event_title}}`, `{{category}}`, `{{sources}}`, and `{{analysis_input}}`, and records the actual prompt key/version in execution logs. A service fallback remains for missing prompt rows.
- Code review found `params.prompt_version` was an implicit association with no referential integrity. `ai_usage_bindings.prompt_id` is now the canonical prompt association, with a database foreign key and application-level active-prompt validation. The provider routing page exposes prompt selection beside provider/model selection.
- Market Information, Knowledge Base, application analysis, and media transcription runtime paths prefer the explicitly bound active prompt. Existing prompt-key/version fallback remains only for compatibility with older bindings.

## Limit-Up Fund Flow Minute Archive Redesign

- The full sweep design was operationally bad: all 1298 historical limit-up stocks ran in one invocation, retry backoff dominated runtime, and ClickHouse writes happened only at the end.
- The redesigned job should be checkpointed round-robin with small shards, bounded runtime, shorter per-stock retry/backoff, and partial ClickHouse flushes.
- Scheduler/Core defaults now favor daily bounded accumulation instead of one all-stock sweep: shard_size 80, max_runtime_seconds 1200, fetch_retries 1, retry backoff 2-6s, provider timeout 8s, flush every 10 successful stocks.

## easy_tdx Integration

- easy_tdx exposes direct online 1m K-line through `MacClient.get_stock_kline(..., Period.MIN_1)` and direct qfq/hfq daily K-line through `Adjust.QFQ`/`Adjust.HFQ`; no local TDX download is required for these requests.
- easy_tdx standard `TdxClient` exposes current fund-flow classification and historical daily fund-flow with a direct Category 22 path plus historical transaction recomputation fallback.
- easy_tdx MAC exposes stock capital-flow data, board list/members/belong/summary/ranking/change-ranking, and historical/current transaction/tick data.
- Cynexus local minute ingestion remains the preferred bulk-file path. Network fallback is appropriate for explicit codes or when no local files are discovered.
- Existing `sources.yaml` labels K-line providers as `tdx`, but `provider_fallback.py` removes `tdx` from network fetch lists. The new adapter must use an explicit provider name so online easy_tdx is actually invoked.
- The adapter is optional-import based, so environments without the live extra retain existing local TDX and other-provider behavior.
- Local TDX files remain the bulk ingestion path; easy_tdx is an online fallback/provider and does not require users to download/import a 1m file for an explicit stock request.
- The local data executor starts from the installed `site-packages/cynexus_data` package, not `/app/src`; copying only `/app/src` does not update the running API routes. Runtime hotfixes must update the installed package or rebuild the image.
- The easy_tdx historical fund-flow API returns real rows for `600519` (120 rows tested). The POC originally loaded it only after switching tabs and shared loading/error state with K-line requests, which made valid flow data appear absent; the UI now preloads and renders flow independently with an empty-response retry.

## easy_tdx Full-Market Fund-Flow Benchmark

- easy_tdx 1.20.4 has no documented provider-side request-rate limiter in the client; it has connection retry/backoff and a 30-second throttle on repeated full-host failover probing.
- `TdxClient.get_fund_flow()` is not a server-side batch API. It fetches L1 transaction pages for one stock and classifies them locally. The Cynexus batch helper only reuses one connection while looping stocks; it still performs per-stock fund-flow and quote requests.
- Live benchmark against 300 real active stocks: 300/300 current flow rows succeeded in two consecutive rounds, with no empty rows or exceptions. The complete Cynexus path took 31.82s cold and 12.77s warm, estimating roughly 4-10 minutes for 5,530 active stocks before adding operational pauses/retries.
- Historical benchmark: `sz000001` returned 120 daily rows in 15.6s. Historical mode first tries Category 22, but can fall back to daily K-lines plus historical transaction pages for every date; full-market historical backfill is therefore roughly a day at the observed direct-path speed and potentially much longer with fallback.
- Current `sync_stock_fund_flow` easy_tdx execution passes all resolved codes to one helper call and does not apply its `batch_size` chunking or per-symbol error isolation. A single exception can abort the whole full-market run, so it should be checkpointed and sharded before production use.

## 2026-07-18 Market ETF Flow Workspace

- `ak.fund_etf_spot_em()` is available in the running Data Executor and returned 1,549 ETF rows during live inspection. It exposes latest price, IOPV estimate, discount rate, volume, amount, inside/outside volume, latest shares, turnover, and EastMoney stock-style flow fields.
- `ak.fund_etf_scale_sse()` reads the SSE public ETF share table by date; `ak.fund_etf_scale_szse()` reads the SZSE public ETF list/share workbook. These are suitable for daily share history and an estimated net subscription/redemption proxy, but not gross subscription and redemption counts.
- `eltdx 1.0.2` exposes `get_etf_codes_all`, quote amount/volume, five-level depth, and ETF K-lines. A live probe returned 1,580 ETF codes and quote snapshots for `sh510300` and `sz159919`. It does not expose ETF IOPV, share history, or creation/redemption counts.
- `easy_tdx 1.20.4` exposes ETF/security categories, quotes, transactions, K-lines, and stock-style fund-flow methods. `get_market_stat()` exists for breadth but the current live endpoint probe failed; existing project breadth capture remains eltdx-backed.
- ETF data must remain separate from stock fund-flow semantics. `main_net_inflow` from EastMoney ETF spot is a secondary-market classification proxy, while daily share delta is the stronger creation/redemption proxy.
- Java should read persisted ETF facts through a dedicated Market ETF domain repository. The UI should use `src/services/market/etf.ts` and `src/views/market/market-etf/*`, not `lib/api.ts`.

## 2026-07-18 ETF classification audit

- The current classifier in `cynexus-data/src/cynexus_data/domain/market/etf.py` uses only substring keywords from the ETF display name. Broad keywords are checked before sector keywords; unmatched names become `other`.
- The live catalog contains 1,549 active ETFs: 361 `broad`, 563 `sector`, and 625 `other`. The current Java query filters to only `broad` and `sector`, so the 625 `other` products are absent from the ETF page.
- The current sector keyword list also incorrectly places overseas, commodity, bond, and currency products such as Nasdaq, S&P, gold, crude oil, bonds, and money-market ETFs into `sector`.
- Examples of likely omitted but meaningful products include A50, CSI 180, Chinese medicine, cloud computing, internet, ESG, specialized innovation, cash-flow, enhanced, and value ETFs.
- There is no single universal official ETF industry taxonomy. Exchange/fund metadata and index-provider classification are authoritative inputs, while a code-keyed registry with source and effective dates is needed to produce stable product categories.
- The EastMoney `fund_info_index_em` catalog is a fund/index catalog rather than an ETF listing: the live Data Executor probe returned 3,934 rows but zero code intersections with the 1,549 ETF catalog. It is therefore optional enrichment only, and current ETF rows remain explicitly marked `fallback.name_keyword`.
- After the expanded fallback rules, the live catalog is 428 broad, 743 sector/industry, and 378 other; the API exposes 1,171 broad/industry instruments and keeps the 378 non-target products out of the direction/rotation aggregates.
- Follow-up audit of the 378 `other` rows found 136 intentionally excluded by overseas/commodity/bond/currency/style keywords, but 242 do not match any current exclusion rule. Clear likely omissions include `深证100`/`深成`/`科创200`/`科创综指` broad indexes and `5G`/`科技`/`碳中和`/`稀有金属`/`粮食`/`绿电`/`航天`/`高端制造` industry/theme ETFs. The mapping is therefore improved but not complete.

## 2026-07-18 ETF mapping registry

- The normalized mapping table uses `(etf_code, mapping_group, mapping_code, source)` as its active business key, allowing one ETF to have multiple style/theme/index mappings while retaining one primary universe/asset/region mapping.
- Full seed analysis produced 6,213 rows for 1,549 ETF codes. The seed is generated from `domain/market/etf_mapping.py`, so the seed and future `sync_market_etf` writes share the same policy.
- Mapping provenance is `analysis.name_rule_v3` for the current catalog; official code-level ETF classification is not claimed because the available EastMoney fund-index catalog had zero intersections with the ETF listing.

## 2026-07-19 AI bookkeeping product analysis

- Mainstream bookkeeping products converge on six workflows: fast manual entry, account/credit management, category and tag customization, budgets/statistics, recurring or scheduled obligations, and import/review/archiving.
- Chinese products such as 钱迹、随手记、鲨鱼记账、薄荷记账 emphasize low-friction entry, account/category templates, charts, recurring bills, and bill import. YNAB emphasizes envelope budgeting, reconciliation, and scheduled transactions. Money Manager/MoneyWiz emphasize multi-account, multi-currency, attachments, exports, and reporting.
- The most important trust rule for AI receipt recognition is review-before-posting: a receipt can contain several purchases, tax/service fees, refunds, or ambiguous dates. AI output must land in an editable draft batch with confidence and source image references; confirmation is the only operation that posts ledger transactions.
- Account types need to distinguish assets from credit accounts. Credit/e-wallet debt accounts use the credit nature; a credit-card payment is a transfer between an asset account and a credit account and must not inflate income/expense statistics.
- Transactions should store positive magnitude plus an explicit `transaction_type`; transfers carry both source and destination account IDs. This avoids negative amount ambiguity and makes category statistics deterministic.
- A default `owner_key` is needed because the current Cynexus deployment has no user/auth aggregate. Keeping it on every finance table allows a later identity integration without rewriting the ledger schema.
- Core already owns PostgreSQL schema `cynexus_appl_core`, uses BIGINT IDs, soft-delete timestamps, Spring JDBC/MyBatis infrastructure, and Snowflake ID generation. Finance persistence belongs in core infra behind domain repository ports; DTOs/controllers must remain in application.
- Existing AI execution only sends text to OpenAI-compatible chat adapters. Receipt images require an explicit multimodal AI contract and chat content parts; core should call that contract through an infra client so provider credentials and model routing remain owned by AI.

## 2026-07-19 AI bookkeeping implementation decisions

- First usable surface: Ledger dashboard, Accounts management, and AI Capture review. Budgets/recurring rules are represented as next-phase-ready fields/extension points but are not allowed to block the core ledger workflow.
- Tables: `finance_accounts`, `finance_categories`, `finance_transactions`, `finance_ai_captures`, `finance_ai_capture_images`, and `finance_ai_capture_items`. All include owner, timestamps, soft archive/delete markers where applicable, stable business status values, and JSONB raw payloads for provider/audit context.
- AI capture accepts one source image per Shortcut request, persists it in `bytea` for a self-contained local workflow, can split one receipt into multiple draft items, and keeps incomplete items in review.
- Manual and AI-confirmed writes share the same domain transaction policy and repository port. Confirming a batch is idempotent through capture-item status and transaction linkage.
- UI uses restrained workspace styling: compact summary band, explicit account/credit distinction, red/green financial semantics consistent with the existing market screens, skeletons during fetches, and a review-first upload panel rather than a decorative landing page.

## 2026-07-19 AI bookkeeping consumption-analysis adjustment

- The primary user workflow is API-driven image ingestion, not manual transaction entry.
- The Finance landing view is therefore a consumption-analysis workspace: period filter, AI image-ingestion CTA, income/expense summary, daily spending rhythm, category distribution, account balances, and recent confirmed entries.
- Manual entry remains available as backend capability for correction/import compatibility, but is no longer presented as the main overview action.
- The default capture API accepts one multipart field named `image`; it posts complete AI items immediately, maps the AI result against the current account/category candidates, and leaves only incomplete items in `review`.
- Shortcut retries are deduplicated through the optional `Idempotency-Key`; when omitted, the service derives a deterministic key from the image contents. No account, category, type, or mode parameter is required.

## 2026-07-19 Credit account and installment requirements

- The finance account model currently stores `nature` as `asset|liability` and treats the generic `credit` account type as a liability. The requested domain vocabulary is `asset|credit`, with concrete credit providers: Huabei, Baitiao, Douyin Monthly Pay, and credit card.
- Credit account balances must retain the existing sign semantics: expenses increase the credit balance owed, income and repayments reduce it; transfers between an asset account and a credit account must not be counted as consumption.
- Installments should be represented separately from the purchase expense. A credit purchase records the full expense once, while a plan owns scheduled installment principal/fee rows and each payment can link to a transfer transaction. This prevents installment repayments from inflating expense statistics.
- The implemented plan model creates 2-60 scheduled rows, splits principal and fees to cents, derives the first due date from the credit account due day when available, and records repayment as an asset-to-credit transfer.

## 2026-07-19 Finance ledger management request

- Existing Core exposes only a bounded `listTransactions` read method; the finance UI renders recent entries inside consumption analysis but has no dedicated ledger management workflow.
- Transaction persistence updates `finance_accounts.balance`; edit/delete must apply the old transaction delta before applying the new delta, including both sides of transfers.
- Credit installment plans reference the purchase transaction. Deleting or materially changing a purchase with a plan must be blocked or require explicit plan handling to avoid orphaned schedules.
- Categories are seeded and listable/creatable through Core, but the UI has no category-management surface; completeness needs both richer seed data and user-managed category CRUD.
- The implementation keeps the existing `DELETE /transactions/{id}` archive contract and adds `POST /transactions/{id}/delete` for soft deletion, so existing archive/history behavior is not changed.
- Transaction edits/deletes are blocked when the transaction is referenced by a purchase installment plan or installment repayment; the derived account balance remains SQL-calculated from active, non-deleted transactions.

## 2026-07-19 Installment deletion follow-up

- The live database has one active installment plan with three installments and one paid installment; the blanket mutation guard therefore blocks the purchase delete path the user is trying to use.
- Safe explicit deletion must cascade soft deletion to the purchase transaction, installment plan, installment rows, and linked repayment transactions; direct deletion of a repayment transaction remains blocked.

## 2026-07-19 Repayment reversal decision

- A repayment transaction is a transfer from an asset account to a credit account. Deleting it must soft-delete that transfer, reset the linked installment to `pending`, clear `paid_at` and `payment_transaction_id`, and set the plan back to `active` unless other paid rows still complete it.
- The user explicitly requested this reversal behavior; it is distinct from deleting the original installment purchase, which still cascades the entire plan.

## 2026-07-19 Finance Overview

- Finance navigation currently exposes `/finance/ledger` as the consumption-analysis landing page; there is no dedicated Overview route.
- `GET /api/core/finance/overview` already returns totals, account balances, category totals, sparse daily income/expense amounts, and 12 recent transactions. The repository aggregates daily rows only for dates containing active ledger entries.
- The daily DTO/BO currently lacks an activity count. A backend `activityCount` field is needed for a GitHub-style contribution heatmap instead of deriving counts from the bounded recent transaction list.
- The new Overview can use the existing overview endpoint with a rolling 12-month range and should keep the existing month-scoped Ledger page unchanged.
- The design direction is a restrained operational finance workspace: a compact decision summary, contribution calendar as the signature element, then trend and allocation analysis with explicit empty/loading states.

## 2026-07-19 Finance Overview panel polish

- Screenshot review shows the signal panel has excessive empty vertical space because four rows are stretched to the same height as the account list; its content lacks a primary signal/value hierarchy.
- The account panel currently renders a single flat list, making assets and credit accounts visually indistinguishable beyond icon color. Group labels and a compact balance summary will improve scanability without changing account data.

## 2026-07-19 Finance chart composition polish

- Screenshot review shows the cash-flow chart has only two active dates but still reserves a large 260px chart canvas, creating mostly empty white space.
- The expense structure panel has only two categories yet is stretched to the cash-flow panel height by the parent grid; its long bars lack rank and visual amount hierarchy.
- The better composition is a compact period summary plus a shorter data-driven chart, with the category module using ranked rows and an items-start parent grid.

## 2026-07-19 Finance expense treemap

- The POC route already uses ECharts Treemap with nodeGap, label, tooltip, and ResizeObserver; this is the established project pattern to reuse.
- Expense category data already has amount and percentage, so treemap area can use amount and tile color can use each category's share of total expense.

## 2026-07-19 Finance Overview period controls

- Finance Overview period selection previously defaulted to a rolling 365-day range and had no date-picker controls.
- The new period control keeps shortcut ranges, defaults to `month`, and switches to `custom` when either boundary is selected.
- Finance dates may occur on weekends, so the shared DatePicker now supports opt-in weekend disabling and Finance Overview explicitly enables weekend selection.

## 2026-07-19 Finance account card workspace

- The Overview account panel already receives account balances and the Core account save endpoint supports an `id` for updates, so the card workspace does not require a new backend contract.
- `Drawer` supports `right` and `bottom` directions; `useSyncExternalStore` keeps the direction synchronized with the 768px desktop breakpoint without an effect-driven state update.
- Account cards expose detail on single click and enter edit mode on double click, matching the existing Transactions interaction vocabulary.

## 2026-07-19 Finance activity pulse range rendering

- Daily Activity Pulse used a fixed 53-week grid regardless of the selected Overview range. With the current-month default this produced a large empty calendar.
- Month labels were derived from the first active cell in each week, so a month with no activity in its first week could lose its label entirely.
- The corrected renderer derives week count and month labels from the selected calendar bounds while preserving the GitHub-style week-column layout for long ranges.

## 2026-07-19 Finance activity pulse independent range

- Daily Activity Pulse is a rhythm/history signal, while cash-flow and expense structure are period analysis panels; they should not share the selected date range.
- The pulse now fetches a fixed trailing 365-day overview independently and remains stable while the user changes the statistics period or custom dates.

## 2026-07-19 Finance activity pulse full width

- The pulse calendar had a `w-fit` inner surface with a 720px minimum, so wide desktop cards showed unused space to the right.
- The inner surface now uses `w-full` plus the existing minimum width, allowing the weekly columns to expand across the available card width while still scrolling on narrow screens.

## 2026-07-19 Finance account three column grid

- Account cards were limited to two columns at every desktop width. The responsive grid now uses three columns at the large breakpoint, keeping two columns for narrower layouts.

## 2026-07-19 Finance signal expansion

- Existing overview data supports more trustworthy signals without a schema change: credit balances and limits, active daily rows across the selected range, and asset account balances.
- Added credit-limit usage, bookkeeping coverage, and largest-asset concentration; each signal has an explicit unavailable state when its source data is insufficient.

## 2026-07-20 Finance dual-platform adaptation

- Existing project pattern uses useIsMobile for Drawer direction; Finance account and transaction drawers now follow it.
- Mobile transaction tables use progressive disclosure: compact cards on mobile and dense tables on desktop.
- Finance period controls and credit fields reflow structurally instead of only shrinking widths.

## 2026-07-20 Finance activity heatmap correction

- The week-grid callback used the week variable without declaring its array index; restoring the index parameter fixes the runtime ReferenceError.

## 2026-07-19 Finance signal detail expansion

- Existing daily overview rows provide a reliable calendar-day average and daily expense peak without relying on the bounded recent transaction list.
- Asset spending buffer is an estimate of current asset balance divided by selected-period average daily expense and is explicitly labeled as an estimate; no new schema is required.

## 2026-07-20 Finance AI capture history

- `finance_ai_captures` already stores `uploaded`, `processing`, `review`, `confirmed`, `failed`, and `archived` states.
- `finance_ai_capture_images.image_data` stores the original upload, so failed captures can be retried without another upload.
- `FinanceLedgerAppService.saveCapturedTransaction` accepts an `id` through its request path and updates an existing transaction; this prevents duplicate posting when editing a confirmed capture.
- The installment mutation policy rejects direct updates to transactions referenced by installment plans. The history flow should surface that validation instead of silently creating a second transaction.
- Finance drawers use `useIsMobile` and `Drawer` with `direction={isMobile ? "bottom" : "right"}`; AI Capture should follow that convention.
- The AI Capture Drawer had a generic `max-h-[min(92vh,860px)]`; with `data-[...right]:h-full`, the max-height still won and left a blank strip below the panel. Direction-specific max-height fixes the layout.

## 2026-07-20 Shortcut media processing workflow

- Existing Core `/tasks/remote-parse` expects a final `downloadUrl`; the existing link acquisition capability is the Downloader API at `https://downloader-api.bhwa233.com/api/parse?url=...`.
- Existing data media runtime downloads the resolved URL, extracts audio with ffmpeg, transcribes asynchronously, and stores the worker task only in an in-memory store. Core therefore needs its own persistent process record for the shortcut workflow.
- Existing AI media capabilities are `text_polish`, `text_summary`, and `post_metadata`, already bound to `mp4_text_polish`, `mp4_text_summary`, and `post_metadata`; the new workflow reuses them and adds no new AI capability code.
- The new process table stores source URL, resolved download URL, worker task ID, phase/progress, transcript/polished/summary, metadata, failure, attempt, and linked `media_transcription_history` ID. A partial unique idempotency index prevents duplicate shortcut submissions.
- Core scheduler claims queued or stale-running processes with PostgreSQL `FOR UPDATE SKIP LOCKED`, then executes them on a bounded task executor. Queued work can resume after Core restart; a missing in-memory data worker task is recorded as a failure rather than silently disappearing.

## 2026-07-21 Workspace AI Chat

- The global workspace header is rendered by `src/layout/workspace-shell.tsx`; `GlobalCommandTrigger` is the existing search control and the correct adjacent entry point for chat.
- `src/components/ui/message.tsx` already provides the shadcn-style Message row/avatar/content/header/footer primitives, but no MessageScroller primitive exists.
- Existing `Sheet` provides a Radix-backed right-side portal with responsive width behavior; it is appropriate for the chat surface and avoids a custom overlay.
- The existing AI execution API is `POST /api/ai/executions` with `scenarioCode`, `userInput`, `variables`, and `preferredModelCode`. Generic execution currently assumes structured JSON, so `workspace_chat` needs a plain-text scenario branch and an explicit provider capability registration.
- Current synchronized project version is `1.1.95-SNAPSHOT`; the functional change must advance Maven and UI versions together to `1.1.96-SNAPSHOT`.
- `workspace_chat` is intentionally routed through the existing usage-binding model path. The AI Provider page must have an enabled provider binding for the send action to return a live model response; the UI surfaces the backend error when that route is unavailable.
- Full Maven package passed after all parent POM versions were synchronized to `1.1.96-SNAPSHOT`; existing duplicate Lombok and MapStruct warnings remain unrelated.
- The first release start attempt used `-Version 1.1.96-260721-RELEASE`, which made the script look for a dated Maven artifact that does not exist; rerunning with `-Build` and no explicit version correctly produced `1.1.96-260721-RELEASE` from `1.1.96-SNAPSHOT`.
- Release UI bundle verification found `Open Cynexus AI`, and `POST /api/ai/executions` with `workspace_chat` returned HTTP 200 after the release restart.
- Follow-up UX correction: the first implementation used `Sheet`, which behaved as a full-height Drawer. It was replaced with the existing Radix-backed `Popover`, anchored to the chat trigger with no overlay; served bundle verification now finds `Workspace assistant` and no `SheetContent` marker.

## 2026-07-21 Workspace AI Chat shadcn base-rhea alignment

- The official `message-scroller` registry component delegates transcript behavior to `@shadcn/react/message-scroller`; the prior local implementation only approximated auto-scroll and did not implement stable-row anchoring, prepend preservation, message jumps, visibility, or active/inactive scroll controls.
- The official wrapper's content surface has no padding; padding belongs to the consuming chat composition. The chat now keeps `p-4` at that boundary.
- The official `Bubble` registry source uses `class-variance-authority` for seven variants and exposes `BubbleGroup` and `BubbleReactions`; the local component now matches those classes without adding `@base-ui/react` because the chat only needs the presentational `div` content path.
- The official scroller class list includes `scroll-fade-b`, which was not previously defined in Cynexus global CSS. A matching scroll-timeline/fallback utility was added so the class has the intended visual behavior.

## 2026-07-21 Workspace AI Chat official demo composition correction

- The official message docs render in-progress state as `Message` containing `Marker role="status"`, not as `MessageAvatar` + `Bubble`.
- The official empty conversation is an `Empty` block centered inside the content area, with `MessageCircleDashed`, `Morning, shadcn!`, and a short prompt; the prior initial assistant message was structurally wrong.
- The official demo header is a card-header pattern with `New Chat`, `How can I help you today?`, and a reset icon action.
- The official composer is a multiline `InputGroup` with `bg-input/50`, `rounded-2xl`, a block-end addon, Plus action, and ArrowUp submit action. The prior `h-10` input, separate send button, and disclaimer did not match.

## 2026-07-21 Workspace AI Chat composer width and overflow correction

- The block-end composer initially missed the official `has-[>[data-align=block-end]]:flex-col` rule and retained `items-center`, leaving the textarea and addon in one row; the textarea consequently shrank to min-content width and rendered one character per line.
- The chat textarea now uses a fixed starting height, a maximum height, and `overflow-y-auto`; long prompts scroll inside the field instead of growing the chat popup.

## 2026-07-21 Workspace AI Chat bubble and composer correction

- User messages should use `Bubble variant="muted"`; assistant messages should use `Bubble variant="ghost"` so only the sender's gray surface is visible.
- Ordinary Message rows do not need `MessageAvatar`; omitting the slot preserves the shadcn row/content alignment without introducing an unused avatar column.
- The composer textarea must be a non-growing full-width block control inside the block-end InputGroup. `flex-none`, bounded height, and hidden scrollbar rules prevent min-content collapse and keep long input scrollable without showing a scrollbar.

## 2026-07-21 Workspace AI Chat response normalization

- `POST /api/ai/executions` returns the provider completion in `data.content`; the configured provider currently places the user-facing answer inside a JSON string field named `response`.
- Workspace chat is explicitly instructed to return plain text, so this envelope is a provider transport inconsistency rather than content that should be shown to the user.
- Normalize only in `src/services/ai/chat.ts` to avoid changing JSON contracts used by structured AI capabilities.
- JSON objects without a supported text field are treated as empty responses, preventing provider transport payloads such as `{}` from leaking into the chat transcript.

## 2026-07-22 Workspace AI Chat bottom-right sizing

- A Popover anchored to the header trigger naturally opens below the header, so the chat surface now overrides Radix positioning with a fixed viewport bottom-right position.
- The width and height use viewport-relative maximums so the larger desktop surface remains usable on mobile screens.

## 2026-07-22 Workspace AI Chat positioning enforcement

- Radix Popover writes runtime `top`, `left`, and `transform` coordinates; utility classes alone were not sufficiently reliable for this forced viewport placement.
- The chat surface now has a dedicated important CSS rule for `position: fixed`, bottom/right insets, and `transform: none`, with a narrow-screen override.

## 2026-07-22 Workspace AI Chat anchored bottom-right Popover

- Directly overriding Radix Popover coordinates was not reliable in the running browser.
- A fixed `PopoverAnchor` is the correct Radix positioning model for this requirement: the trigger controls open state, while the content is positioned above a 1px anchor at the viewport bottom-right.

## 2026-07-22 Workspace AI Chat viewport panel

- The anchor-based Radix positioning still rendered the content under the header in the running UI.
- The final placement uses a full-viewport Popover shell and positions only the visual chat panel with `position: fixed; right: 24px; bottom: 24px`, removing all dependence on Popper's calculated coordinates.

## 2026-07-22 Workspace AI Chat flex viewport alignment

- To remove any remaining containing-block behavior from nested fixed positioning, the full-viewport shell now uses `align-items: flex-end` and `justify-content: flex-end`; the panel uses only bottom/right margins inside that shell.

## 2026-07-22 Workspace AI Chat direct viewport portal

- Radix Popover content was still able to place the visual panel according to the header trigger in the running UI.
- The visual panel is now rendered with `createPortal(..., document.body)` and its own fixed right/bottom coordinates; Radix remains responsible only for trigger open state.

## 2026-07-22 Autonomous AI Agent discovery

- Existing dependencies already include Spring AI 2.0.0 and LangChain4j 1.12.1, so the first Agent release should extend the current runtime instead of adding another orchestration framework.
- `AgentExecutor` is currently a placeholder: no-tool requests call the model, but any non-empty tool list returns a mock JSON response without executing a capability.
- `AiToolCatalog` only returns four hard-coded market tool codes; it does not expose schemas, permissions, freshness, source, or handlers.
- `MarketDataTools` already wraps six Market Feign operations behind `MarketDataGateway`; these are the safest first read-only Agent tool pack.
- `OpenAiCompatibleChatAdapter` parses ordinary chat/responses output but does not send `tools` or parse `tool_calls`; native provider tool calling must be added behind the infra adapter.
- `workspace_chat` currently bypasses tools by forcing an empty tool-code list, so the new Agent endpoint should be a separate scenario/runtime path and the existing chat can later delegate to it.
- Existing `cynexus_appl_ai` conventions use BIGINT IDs, JSONB payloads, soft deletion, timestamps, and schema-local tables; Agent conversations, runs, and steps fit this pattern.
- The provider capability catalog is in `cynexus_appl_core`, while runtime tool definitions belong to `cynexus-ai`; the Agent UI should show runtime tool availability and link source metadata without treating every catalog row as executable.
- Phase 99 continuation audit (2026-07-22): the live provider row `custom` is named `xtyooo`, uses `https://am.xtyooo.top`, and defaults to `gpt-5.4-mini`; the live `workspace_chat` binding points to this provider, but `autonomous_agent` has no dedicated binding yet. The runtime currently falls back to `workspace_chat`/`text_summary`, so a dedicated idempotent binding is required before claiming provider routing is complete.
- The current Agent endpoint accepts `ownerKey` from the request/query, persists only run/step rows, has no conversation list/message transcript, and executes tool calls sequentially despite the architecture document describing bounded parallel reads. These are implementation gaps, not just UI gaps.
- Existing callable service capabilities available for Agent expansion are Core finance read APIs (`/api/core/finance/overview`, `/transactions`), Knowledge Base document lookup (`/api/knowledge-base/documents/{id}`), and Market APIs backed by ClickHouse for chart/detail data. No arbitrary SQL capability should be exposed; ClickHouse access must use typed, allow-listed queries.
