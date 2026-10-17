# Limit Board Quality And Trend

## Goal

Make limit-board session data trustworthy under delayed captures and expose a stock-level sealed-amount trend from persisted samples.

## Phases

- [completed] Diagnose capture cadence, gap handling, and persisted trend payloads.
- [completed] Fix aggregation/collection semantics and add focused tests.
- [completed] Add the trend query/display to the limit-board monitor.
- [completed] Verify the new session job registration/runtime and publish the data/market changes. Live discovery remains pending the next trading session; the UI artifact builds successfully and Release UI deployment remains deferred.

## Constraints

- Preserve unrelated user changes.
- Do not start deprecated Dev or local source-tree runtimes.
- Keep the synchronized `1.1.398-SNAPSHOT` version contract.

## Verification Notes

- Limit-board domain and pipeline/storage tests: 13 passed.
- Limit-board page/service ESLint: passed; UI TypeScript/build: passed.
- Core compile and Core/Market Release rebuild: passed; Core and Market are running on `1.1.398-260813-RELEASE`.
- Data executor rebuilt and healthy.
- Existing historical rows are marked `historical_partial` and use conservative `valid_sample_count * sample_interval_seconds` duration estimates.
- Historical rows now display `已确认封板时长`; gaps are explicitly excluded from the confirmed duration.
- The Release UI image rebuild could not pull `nginx:1.27-alpine` from Docker Hub; the current Release UI container remains on the prior image. The built UI artifacts include the trend chart and are ready for the next UI image rebuild when registry access is available.
- Full-market discovery now owns Redis notifications for the five-second sampler. The priority monitor is explicitly excluded so its partial universe cannot introduce false tracking membership.
- Trend samples are stored compactly as two PostgreSQL arrays: `smallint[]` relative-second offsets plus `bigint[]` sealed amounts in integer yuan, anchored by one `timestamptz`; at most 120 points are retained and the arrays are constrained to equal length.
- Market API mapping returns the arrays as JSON arrays, and the UI prefers this compact format while retaining the legacy JSONB point-array fallback for historical rows.
- PostgreSQL migration, data executor rebuild, and Market Release rebuild completed. The session trigger dry-run returned success with a 3-second cadence configuration.

## Errors Encountered

- The first repository Python test command lacked `PYTHONPATH`; rerun with `cynexus-data/src` succeeded previously.
