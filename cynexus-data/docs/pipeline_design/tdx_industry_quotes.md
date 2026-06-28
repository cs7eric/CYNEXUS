# TDX Industry Quotes Pipeline

## Job

`sync_tdx_industry_quotes`

## Purpose

Migrate the scoped data-production part of legacy `tdx_industry_service.py`: resolve TDX 56 industry index full codes through eltdx, fetch quote snapshots, and persist lightweight serving rows for Java/UI.

## Parameters

- `snapshot_date` / `trade_date`: snapshot date, default today.
- `create_table`: apply PostgreSQL DDL.
- `rows`: JSON array for repair/testing/import without live eltdx access.
- `mapping`: optional `{code6: name}` override, default TDX 56 catalog.
- `batch_size`: quote fetch batch size, default 20.
- `pool_size`, `timeout` / `timeout_seconds`: optional eltdx client settings.
- `source`: storage source tag, default `eltdx`.

## Boundary

- Domain owns static TDX 56 industry code and seed catalogs.
- Source owns eltdx code resolution and quote normalization.
- Pipeline owns orchestration and dry-run behavior.
- PostgreSQL stores only compact quote snapshots.
- Old heatmap Flask response assembly remains outside `cynexus-data`.
