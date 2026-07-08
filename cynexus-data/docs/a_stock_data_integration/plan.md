# A Stock Data Integration Plan

## Goal

Integrate the missing `a-stock-data.md` public data-source capabilities into `cynexus-data` while preserving the existing project layering:

`provider source -> parser/adapter/domain normalization -> optional pipeline/storage -> internal service route`

The first pass focused on source/query integration and live reachability validation. The 2026-07-07 pass adds explicit PostgreSQL/ClickHouse contracts and a reusable persistence job for the datasets that do not already have a matching project table.

## Scope

- EastMoney: rate-limited HTTP helper, reports, datacenter datasets, concept blocks, fund-flow minute/daily, news, global news, hot rank/concepts, limit pools, industry rank, stock info enrichment.
- THS extras: EPS forecast, hot reason, northbound flow, limit-up reason/pool, hot list.
- CNInfo: announcements and IRM investor Q&A.
- Sina: financial statements and ETF option quote/greeks.
- Baidu: K-line with MA fields.
- iwencai: report semantic search/query with explicit API key handling.
- TDX/mootdx: quote/order-book/transaction helper wrappers where local optional dependency supports them.

## Non-Goals

- No Java/UI route migration unless explicitly required by an existing page.
- No hidden scraping credentials or cookies.
- No forced full-market high-frequency crawl. High-frequency sources must run against an explicit small stock/option pool.

## Phases

1. Source foundation: add provider-scoped modules and reusable EastMoney throttled client.
2. Internal query surface: add thin FastAPI routes under `/internal/a-stock/*`.
3. Tests: add mock/unit coverage for parsers, URL/parameter shaping, and failure normalization.
4. Live smoke: run public endpoint probes with tiny payloads and record pass/fail.
5. Documentation: update process and result matrix with implemented files, test commands, failures, and next persistence steps.
6. Persistence: add PG/CH DDL, storage repositories, normalization, and `sync_a_stock_dataset`.
7. Knowledge base: publish the detailed table map and full capability map into `kb_document`.

## Quality Rules

- Source modules do not import storage/runtime/job state.
- Routes do not contain provider-specific parsing beyond request parameter mapping.
- EastMoney endpoints go through the shared throttled session helper.
- Optional providers (`mootdx`, `pandas`, API-keyed iwencai) fail explicitly with actionable messages.
- Every live smoke failure is recorded; failed connectivity does not get disguised as implemented success.
