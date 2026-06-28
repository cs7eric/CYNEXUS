# pipelines/market

Market-level pipelines produce lightweight cross-index or whole-market serving results.

`build_index_returns.py` reads broad-index K-line facts from ClickHouse and writes compact window-return rows to PostgreSQL for Java/UI reads.

`sync_tdx_industry_quotes.py` fetches TDX 56 industry index quote snapshots through eltdx and writes compact PostgreSQL serving rows.

`build_market_heatmap.py` turns persisted industry quote rows into a compact JSONB heatmap snapshot for Java/UI reads.

`sync_tdx_sector_quote_details.py` captures eltdx `f10.theme_market` detail sections for TDX industry seeds and persists JSONB serving rows.
