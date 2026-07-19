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

- Mainstream bookkeeping products converge on six workflows: fast manual entry, account/liability management, category and tag customization, budgets/statistics, recurring or scheduled obligations, and import/review/archiving.
- Chinese products such as 钱迹、随手记、鲨鱼记账、薄荷记账 emphasize low-friction entry, account/category templates, charts, recurring bills, and bill import. YNAB emphasizes envelope budgeting, reconciliation, and scheduled transactions. Money Manager/MoneyWiz emphasize multi-account, multi-currency, attachments, exports, and reporting.
- The most important trust rule for AI receipt recognition is review-before-posting: a receipt can contain several purchases, tax/service fees, refunds, or ambiguous dates. AI output must land in an editable draft batch with confidence and source image references; confirmation is the only operation that posts ledger transactions.
- Account types need to distinguish assets from liabilities. Credit/e-wallet debt accounts are liabilities; a credit-card payment is a transfer between an asset account and a liability account and must not inflate income/expense statistics.
- Transactions should store positive magnitude plus an explicit `transaction_type`; transfers carry both source and destination account IDs. This avoids negative amount ambiguity and makes category statistics deterministic.
- A default `owner_key` is needed because the current Cynexus deployment has no user/auth aggregate. Keeping it on every finance table allows a later identity integration without rewriting the ledger schema.
- Core already owns PostgreSQL schema `cynexus_appl_core`, uses BIGINT IDs, soft-delete timestamps, Spring JDBC/MyBatis infrastructure, and Snowflake ID generation. Finance persistence belongs in core infra behind domain repository ports; DTOs/controllers must remain in application.
- Existing AI execution only sends text to OpenAI-compatible chat adapters. Receipt images require an explicit multimodal AI contract and chat content parts; core should call that contract through an infra client so provider credentials and model routing remain owned by AI.

## 2026-07-19 AI bookkeeping implementation decisions

- First usable surface: Ledger dashboard, Accounts management, and AI Capture review. Budgets/recurring rules are represented as next-phase-ready fields/extension points but are not allowed to block the core ledger workflow.
- Tables: `finance_accounts`, `finance_categories`, `finance_transactions`, `finance_ai_captures`, `finance_ai_capture_images`, and `finance_ai_capture_items`. All include owner, timestamps, soft archive/delete markers where applicable, stable business status values, and JSONB raw payloads for provider/audit context.
- AI capture accepts one source image per Shortcut request, persists it in `bytea` for a self-contained local workflow, can split one receipt into multiple draft items, and keeps incomplete items in review.
- Manual and AI-confirmed writes share the same domain transaction policy and repository port. Confirming a batch is idempotent through capture-item status and transaction linkage.
- UI uses restrained workspace styling: compact summary band, explicit account/liability distinction, red/green financial semantics consistent with the existing market screens, skeletons during fetches, and a review-first upload panel rather than a decorative landing page.

## 2026-07-19 AI bookkeeping consumption-analysis adjustment

- The primary user workflow is API-driven image ingestion, not manual transaction entry.
- The Finance landing view is therefore a consumption-analysis workspace: period filter, AI image-ingestion CTA, income/expense summary, daily spending rhythm, category distribution, account balances, and recent confirmed entries.
- Manual entry remains available as backend capability for correction/import compatibility, but is no longer presented as the main overview action.
- The default capture API accepts one multipart field named `image`; it posts complete AI items immediately, maps the AI result against the current account/category candidates, and leaves only incomplete items in `review`.
- Shortcut retries are deduplicated through the optional `Idempotency-Key`; when omitted, the service derives a deterministic key from the image contents. No account, category, type, or mode parameter is required.
