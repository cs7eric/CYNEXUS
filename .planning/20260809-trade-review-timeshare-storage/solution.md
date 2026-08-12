# Trade Review Timeshare Persistence and Cache Design

## 1. Outcome

Build one deterministic post-close data path for the trade-review workbench:

```text
daily selection inputs
  -> deduplicated archive plan
  -> raw stock/ETF timeshare fetch
  -> minute-slot normalization
  -> local 1m amount enrichment
  -> ClickHouse durable write
  -> Redis binary publication
  -> existing stock-chart API
  -> main replay chart and per-stock sparklines
```

Stock/ETF raw timeshare remains the authoritative displayed price series. Index and industry context uses the existing 1m K fact because the product permits an intraday K chart and the TDX historical-timeshare protocol does not return industry series.

## 2. Data ownership

| Data | Durable source | Redis | Consumer |
| --- | --- | --- | --- |
| Stock/ETF raw timeshare price and volume | `ch_ods_stock_timeshare_1m` | Yes, 7 calendar days | Main chart, small chart, replay |
| Stock/ETF minute amount and VWAP | Minute-aligned enrichment from `ch_dwd_kline_1m` | Included in normalized payload | Main chart and hover |
| Index/TDX/THS industry context | Existing `ch_dwd_kline_1m` | Yes, normalized context payload | Right-side context chart |
| Limit status | `market_limit_daily_stocks` | No point-level duplication | Selection and card ordering |
| THS membership/rank input | `ths_industry_constituent_snapshot` | No point-level duplication | Selection reasons |
| Close price/change percentage | `a_stock_daily_screening_fact` | Header metadata only | Sorting and labels |
| Portfolio trade symbols | `portfolio_trade` distinct symbol/date only | No trade detail | Mandatory selection |
| Archive lineage and health | New PostgreSQL batch/target/reason tables | Optional short status cache | Data-health page and repair |

## 3. Selection policy

For one exact trade date, build target reasons in this order:

1. All A-share/ETF symbols that have a portfolio trade on that date.
2. Symbols manually pinned by a review observation or explicit on-demand load.
3. All `limit_up` and `limit_down` symbols from the final close pool.
4. For every successful THS industry snapshot on the same date:
   - remove every limit-up/limit-down symbol;
   - exclude rows with null `change_pct`;
   - take top 15 descending by `change_pct`;
   - take bottom 10 ascending by `change_pct`, excluding rows already selected in the top side.
5. Add configured market indices and every active industry quote target used by Industry Application.

Deduplicate fetch targets by:

```text
(trade_date, target_type, market, canonical_symbol)
```

Keep every reason separately. A stock belonging to five industries is fetched once but may have five or more reason rows.

Limit membership is explicit, not inferred from a hard-coded 10% threshold, so 5%, 10%, 20%, and 30% boards remain correct.

No future snapshots are allowed. If a same-day THS batch is unavailable, retain the limit/trade/manual/context pools and mark industry selection degraded; do not silently use a future or previous-day ranking as if it were current.

## 4. PostgreSQL archive manifest

### `timeshare_archive_batch`

```text
id
trade_date
status                 PLANNED/RUNNING/PARTIAL/SUCCESS/FAILED
selection_version
config_snapshot        jsonb
planned_target_count
ready_target_count
partial_target_count
failed_target_count
cache_published_count
started_at
completed_at
error_summary          jsonb
created_at
updated_at
```

Unique active batch per `(trade_date, selection_version)`.

### `timeshare_archive_target`

```text
id
batch_id
trade_date
target_type            STOCK/ETF/INDEX/TDX_INDUSTRY/THS_INDUSTRY
storage_kind           RAW_TIMESHARE/KLINE_CONTEXT
market
symbol
canonical_symbol
name
priority
status                 PLANNED/FETCHING/READY/PARTIAL/FAILED
point_count
expected_point_count
completeness_pct
prev_close
close_price
change_pct
volume_unit_multiplier
price_source
amount_source
avg_price_source
cache_status
attempt_count
last_error_code
last_error_message
fetched_at
updated_at
```

Unique key:

```text
(batch_id, target_type, market, canonical_symbol)
```

### `timeshare_archive_reason`

```text
id
target_id
reason_type
industry_code          nullable
industry_name          nullable
rank_direction         GAINER/DECLINER nullable
rank_position          nullable
change_pct             nullable
source_record_date
created_at
```

Reason types:

```text
PORTFOLIO_TRADE
REVIEW_OBSERVATION
MANUAL_LOAD
LIMIT_UP
LIMIT_DOWN
THS_TOP_GAINER
THS_TOP_DECLINER
SYSTEM_INDEX
TDX_INDUSTRY
THS_INDUSTRY
```

These tables are data lineage/repair state, not scheduler execution state.

## 5. ClickHouse stock/ETF timeshare fact

Reuse `ch_ods_stock_timeshare_1m` after checking it is still empty. If it contains data by implementation time, create a v2 table and cut over instead of dropping populated columns.

Recommended compact schema:

```text
trade_date             Date
target_type            LowCardinality(String)
market                 LowCardinality(String)
symbol                 String
tdx_code               String
slot                    UInt16
trade_time              DateTime('Asia/Shanghai')
price                   Decimal64(4)
avg_price               Nullable(Decimal64(4))
volume_lots             UInt64
turnover                Nullable(Decimal64(2))
price_source            LowCardinality(String)
amount_source           LowCardinality(String)
source_version          LowCardinality(String)
fetched_at              DateTime64(3, 'Asia/Shanghai')
ingested_at             DateTime64(3, 'Asia/Shanghai')
row_hash                materialized UInt64
```

Engine:

```text
ReplacingMergeTree(ingested_at)
PARTITION BY toYYYYMM(trade_date)
ORDER BY (trade_date, target_type, market, symbol, slot, price_source)
```

Do not persist:

```text
raw_payload
time_label
code duplicated from symbol
turnover_rate
volume_ratio
change_pct per point
cum_volume
cum_turnover
```

Time label, cumulative values, and change percentage are derived during read/decode.

## 6. Canonical minute grid and enrichment

Canonical stock/ETF slots use minute-start labels:

```text
slot 0..119   = 09:30..11:29
slot 120..239 = 13:00..14:59
```

Trade and indicator timestamps use floor-to-minute. The chart domain remains fixed at `09:30–15:00`, so replay never changes X-axis scale as points appear.

Local TDX 1m K uses minute-end labels. Align as:

```text
09:31 K bar -> 09:30 timeshare slot
...
15:00 K bar -> 14:59 timeshare slot
```

The provider timeshare volume is currently lots while local 1m K is shares. Detect the multiplier from candidates `{1, 10, 100, 1000}` by minimizing the daily volume reconciliation error; normally it is 100. Persist the selected multiplier in the archive target.

Enrichment rules:

1. Price always comes from raw historical timeshare.
2. Volume displayed in the chart comes from raw historical timeshare.
3. Turnover comes from the minute-aligned 1m K `amount` only when slot alignment and volume reconciliation pass.
4. Average price preference:
   - valid provider `avg_price`;
   - otherwise cumulative aligned `amount / shares`;
   - otherwise null, with an explicit degraded status.
5. Never approximate turnover using `last_price * volume`.

Lineage examples:

```text
price_source = easy_tdx.history_timeshare
amount_source = tdx_local.1m
avg_price_source = tdx_local.1m.cumulative_vwap
```

## 7. Quality policy

`READY` requires:

- exact requested trade date;
- 240 unique canonical slots;
- strictly increasing slots;
- no lunch-break points;
- price > 0 and volume >= 0;
- last raw-timeshare price reconciles with daily close within the configured tolerance;
- no future-dated source or metadata.

`PARTIAL` examples:

- 1–10 missing slots;
- raw price/volume valid but amount/VWAP enrichment unavailable;
- volume reconciliation exceeds tolerance;
- close mismatch exceeds tolerance;
- cache publication failed after durable persistence.

`FAILED` means no usable raw stock/ETF series. Overall trade-review availability remains:

```text
READY       raw timeshare and 1m K both available
DEGRADED    exactly one is available or enrichment is partial
UNAVAILABLE raw timeshare and 1m K both missing
```

## 8. Data Executor implementation

Add a dedicated job and pipeline:

```text
job key: sync_trade_review_timeshare
scheduler code: trade_review_timeshare_close
pipeline: kline.sync_trade_review_timeshare
```

Do not use the current `sync_tdx_intraday_detail` default-all-security path unchanged. Reuse its source and sink components after correcting provider provenance.

Pipeline stages:

1. Resolve the exact trade date and prerequisites.
2. Build and persist the archive batch/targets/reasons.
3. Query already-ready targets for resume support.
4. Fetch stock/ETF raw timeshare with a bounded persistent-client worker pool.
5. Read 1m amount rows from ClickHouse in batches for all fetched symbols.
6. Normalize slots, reconcile volume units, enrich turnover/VWAP, and validate.
7. Replace the exact symbol/date fact rows in ClickHouse.
8. Commit target status.
9. Encode and publish Redis only after the ClickHouse write succeeds.
10. Finalize batch health and bounded error summary.

Recommended initial runtime parameters:

```text
workers = 8
timeout_seconds = 15
retries = 2
retry_backoff_seconds = [1, 3]
clickhouse_batch_rows = 50000
resume = true
cache_ttl_seconds = 604800
top_gainers = 15
top_decliners = 10
```

Each worker should reuse one TDX connection and reconnect/rotate host only after failure. Opening a new client and selecting a best host for every symbol is too slow for roughly 2,000 targets.

The same job supports repair parameters:

```text
trade_date
symbols             optional bounded list
statuses            FAILED/PARTIAL
rebuild_plan        false by default
rebuild_cache       optional
dry_run
```

## 9. Redis binary cache

### Common Java primitive

Add a sibling `RedisBinaryCacheOperations` in `cynexus-common-cache`; do not change existing string-operation behavior. Provide bounded:

```text
get(key) -> byte[]
set(key, byte[], ttl)
multiGet(keys) -> list<byte[]>
delete(key)
exists(key)
```

Use a String key serializer and raw byte-array value serializer.

### Python producer

Add an explicit binary Redis client method/option to the existing Redis hook with `decode_responses=False`.

### Key

```text
cynexus:market:timeshare:v1:{tradeDate}:{targetType}:{market}:{canonicalSymbol}
```

One key per series is acceptable: weekly key overhead is small, per-card retrieval is direct, and failures do not invalidate a whole trading day.

### Value

Use columnar MessagePack followed by Zstd level 3:

```text
[
  schemaVersion,
  flags,
  pointCount,
  priceScale,
  prevCloseScaled,
  volumeUnitMultiplier,
  presenceBitmap,
  priceDeltaArray,
  avgPriceDeltaArrayOrNull,
  volumeArray,
  turnoverArrayOrNull,
  sourceCodes
]
```

Do not include symbol/date/type/name inside the payload because they are already in the key or instrument metadata.

Redis is never authoritative. Read order:

```text
Redis binary
  -> on miss/corruption/error: ClickHouse durable read
  -> optionally republish asynchronously
```

Readers do not trigger the external provider and do not delete malformed immutable bundles in the request path.

Estimated seven-calendar-day footprint for the current selection size:

```text
target: 80–200 MB
reserved feature budget: 512 MB
```

Do not change the global Redis eviction policy solely for this feature. If total instance headroom is insufficient, use a separate Redis instance; a separate logical DB does not isolate memory.

## 10. Market service/API

Keep the existing endpoint:

```text
GET /api/market/stock-chart/intraday
```

Add `strict_date=true`; trade review always sends it. In strict mode, an absent requested date must not silently return a previous trade date.

Extend response metadata:

```text
timeshare_status
timeshare_point_count
timeshare_expected_point_count
timeshare_data_source
timeshare_serving_source     REDIS/CLICKHOUSE
average_price_available
turnover_available
data_warnings
```

Implementation boundary:

- `StockChartQueryRepositoryImpl.listTimeshare` calls a market `infra/cache` adapter first, then `ClickHouseStockChartClient`.
- Replace `list-timeshare.sql`, which currently reconstructs from `ch_dwd_kline_1m`, with a read from the persisted stock/ETF timeshare fact.
- Index/industry calls continue to use `listMinuteBars` and the 1m context path.
- Correct the general `source` field so it no longer always reports `clickhouse.ch_dwd_kline_1m` for stock timeshare.

Add operational endpoints under trade review:

```text
GET  /api/market/trade-review/timeshare/health?trade_date=...
POST /api/market/trade-review/timeshare/reload
```

Health returns batch counts, prerequisite status, coverage, cache publication, and bounded failed targets. Reload only accepts an exact date and optional bounded symbol list and triggers the allow-listed scheduler/Data Executor job.

Raw minute-series retrieval is deliberately excluded from autonomous Agent tools because it is large and UI-oriented. A future Agent feature should expose a bounded intraday summary, not raw points.

## 11. Frontend changes

1. Remove direct `fetchEasyTdxTimeshare` fallback from the main replay load.
2. Remove direct Easy-TDX calls from every industry constituent sparkline.
3. Keep each visible card's own `fetchStockIntraday` request so pagination and card skeletons remain independent; those requests now hit Redis/ClickHouse instead of TDX.
4. Pass `strict_date=true` for every trade-review call.
5. Preserve the fixed full-session X-axis before playback starts.
6. Use the stored `avg_price` line inside the stock chart; if unavailable, show a small `均价不可用` degraded label rather than inventing a line.
7. Use close-based `change_pct` from daily screening for cards and ordering, never Market Indicator events.
8. Show data date, data source, readiness, and point count in the panel status area.
9. Add `加载分时/重试` only for missing or failed symbols; do not auto-call the provider on every render.
10. Keep current Dog Loader target-switch behavior and per-card Skeleton geometry.

## 12. Scheduler order

Recommended post-close readiness sequence:

```text
trade calendar/date close
  -> limit pool and daily screening complete
  -> THS industry constituent snapshot complete
  -> local TDX 1m close load attempted
  -> trade_review_timeshare_close
  -> repair PARTIAL targets after a later local 1m load if needed
```

The timeshare collector may fetch raw price/volume even if local 1m enrichment is not ready. It records `PARTIAL`, publishes the usable raw series, and a later repair fills amount/VWAP.

The existing page's local 1m load button remains responsible for `kline_1m_tdx_close`. The new timeshare health block gets its own scoped reload action and must not relabel the 1m job as a timeshare job.

## 13. Verification

### Python

- Selection top/bottom, limit exclusion, ties, fewer-than-25 industries, and cross-industry deduplication.
- Portfolio/manual reason inclusion without persisting user trade details.
- Canonical 240-slot mapping and lunch exclusion.
- 09:30 timeshare to 09:31 K alignment.
- Volume multiplier detection and rejection.
- Exact VWAP/turnover enrichment.
- Provider provenance and absence of raw/ratio fields.
- Resume, retry, partial results, cache-after-durable-ack, and Redis failure tolerance.
- No-future/as-of tests.

### Java

- Binary codec cross-language fixture tests.
- Cache hit, miss, timeout, malformed payload, and ClickHouse fallback.
- Strict-date no-fallback behavior.
- Stored timeshare SQL no longer reads `ch_dwd_kline_1m`.
- Index/industry context still reads 1m K.
- Status/source/warning DTO mapping.
- Reload input bounds and allow-listed job trigger.

### UI

- Main and card paths never call `/internal/a-stock/tdx/timeshare`.
- Independent per-card skeleton/loading/empty states.
- Fixed X-axis throughout playback.
- Price, average line, and volume remain slot-aligned.
- Missing average line and partial data are explicit.
- Target switching and panel resizing do not leave stale loaders.

### Runtime

- One sampled stock/ETF has 240 raw points and matching date.
- `600519` raw volume reconciles with 1m volume after multiplier normalization.
- Shanghai Composite context is available.
- An industry context uses 1m K and does not attempt raw timeshare.
- Redis compressed payload size and decode latency are measured.
- Health counts match ClickHouse facts and manifest targets.

## 14. Rollout order

1. Schema and codec fixture.
2. Selection manifest and dry-run target counts.
3. Durable stock/ETF collector without Redis.
4. Amount/VWAP enrichment and quality states.
5. Binary Redis producer/consumer and fallback.
6. Market strict-date/status API and stored-timeshare SQL cutover.
7. Trade-review UI removal of direct TDX calls.
8. One-day backfill and reconciliation.
9. Seven-day backfill for cache/retention validation.
10. Enable the close scheduler and repair workflow.

Because this changes Data Executor, ClickHouse/PostgreSQL schema, shared cache primitives, market service, scheduler registration, and UI behavior, implementation requires synchronized Maven/UI versions and a full Release application deployment and verification.

## 15. Capacity

At the measured scale of roughly 2,316 daily unique stock/context series and 555,840 points:

```text
ClickHouse new raw stock/ETF fact: approximately 20–45 MB per trade day
250 trade days: approximately 5–11 GB per year
PostgreSQL manifest/reasons: tens of MB per year
Redis seven-calendar-day compressed working set: approximately 80–200 MB
```

The main operational risks are source throughput, exact date semantics, minute-label alignment, enrichment reconciliation, and partial-state repair—not storage capacity.
