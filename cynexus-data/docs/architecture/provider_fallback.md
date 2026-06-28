# Provider Fallback

`sources/provider_fallback.py` replaces the legacy `market_data_provider.py` singleton with a source-layer fallback facade.

Rules:

- Source fallback only fetches and normalizes provider payloads.
- It does not write ClickHouse/PostgreSQL, read job state, or manage checkpoints.
- Pipelines decide whether to persist rows and how to report job metadata.
- Provider order comes from `config/sources.yaml` unless the job request passes `providers`.

Current kline support:

- Daily network fallback: Tencent, EastMoney.
- Minute network fallback: Sina, EastMoney, Tencent where provider adapters support the requested period.
- TDX local remains pipeline-owned because it reads local filesystem files and has different batching semantics.
- Market symbol helpers live in `core/market_symbol.py` so domain conversion and source provider calls do not depend on each other.

Every fallback result includes `source`, `stale`, `dataQuality`, and `attempts` so Java/API callers can inspect which provider actually produced data.
