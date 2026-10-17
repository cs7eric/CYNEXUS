# Findings

- The old duration fix added every real timestamp delta, including large capture gaps. That can imply continuous sealing during unobserved periods.
- Current persisted data has large gaps: examples have 70 samples over roughly 110 minutes, `gap_count` around 1249, and `coverage_ratio` about 4.86%.
- Capture runs can take about 9-10 seconds when they perform a full 5514-code discovery, while the nominal scheduler cadence is 5 seconds. This creates skipped/delayed observations.
- `trend_features.sealed_amount_points` already persists relative-time sealed amount samples for each summary. It can support a stock trend chart without reintroducing raw Redis access.
- The current UI only fetches session summaries and does not expose a selected-stock trend view.
- New summaries accumulate only positive gaps up to `max(15, sample_interval_seconds * 3)`; larger gaps are quality gaps and are excluded from continuous sealed duration.
- The old standalone capture schedule is disabled in favor of a five-minute asynchronous scheduler session that performs sequential five-second capture cycles under one lease.
- The full-market indicator refresh is already scheduled every 30 seconds and completes in about 25 seconds for the 5,514-stock universe. It is the suitable authoritative discovery source; the limit-board sampler must not perform a separate full-market scan.
- The handoff uses the existing session Redis namespace. Discovery publishes codes plus up/down direction; the sampler remains idle until this shared tracking set is populated.
- Historical summaries are explicitly `historical_partial`; their duration is a conservative sample-count estimate, while their persisted trend points remain usable for visual inspection with a warning.
- A compact trend representation is preferable to JSONB point objects for a 3-second sampler: one origin timestamp plus parallel `smallint[]` offsets and `bigint[]` integer-yuan amounts removes repeated JSON keys and preserves enough resolution for the chart.
- The live schema has `trend_series_origin_at`, `trend_time_offsets`, and `trend_sealed_amounts`; a check constraint limits the series to 120 aligned points.
