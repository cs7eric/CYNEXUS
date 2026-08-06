# pipelines/market

Market-level pipelines produce lightweight cross-index or whole-market serving results.

`build_index_returns.py` reads broad-index K-line facts from ClickHouse and writes compact window-return rows to PostgreSQL for Java/UI reads.

`sync_tdx_industry_quotes.py` fetches TDX 56 industry index quote snapshots through eltdx and writes compact PostgreSQL serving rows.

`build_market_heatmap.py` turns persisted industry quote rows into a compact JSONB heatmap snapshot for Java/UI reads.

`sync_tdx_sector_quote_details.py` captures eltdx `f10.theme_market` detail sections for TDX industry seeds and persists JSONB serving rows.

`market_indicator_refresh.py` captures the active stock universe through `eltdx.quotes`,

The candidate universe is first restricted to stocks present in
`market_limit_daily_stocks` with a `limit_up` or `limit_down` record in the
previous 365 calendar days. The full deduplicated universe is refreshed on every
run; `limit_codes` is retained only as an explicit test cap. It stores one
append-only PostgreSQL snapshot per symbol and emits transition events for rapid
moves and limit-board state changes. Stock industry metadata comes from the
latest THS90 constituent snapshot and the existing THS90-to-TDX56 mapping, not
from concept/sector labels. Raw unbounded tick history is intentionally not
persisted in PostgreSQL.
