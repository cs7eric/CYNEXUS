# TDX Sector Quote Detail Pipeline

`sync_tdx_sector_quote_details`

Migrates the scoped data-production part of legacy `sector_quote_service.py`: capture TDX theme-market detail sections for industry seeds and persist a compact serving snapshot.

## Flow

1. Resolve TDX 56 industry codes and representative seed stocks from pure domain catalogs.
2. Fetch requested `theme_market` sections through optional `eltdx`.
3. Normalize sections into a pure domain payload.
4. Replace the requested industry's snapshot rows in PostgreSQL.

## Boundary

- No old Flask API or UI response wiring.
- No stock-universe JSON lookup.
- No Tencent constituent fallback in this job; Tencent realtime quote paths remain in the Tencent source/fund-flow migrations.
- Dry-run accepts explicit `rows` or generated samples and does not touch eltdx/PostgreSQL.
