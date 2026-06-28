# domain/market

Market-domain calculations.

Use this folder for market sentiment, limit emotion, sector breadth, fund flow, margin, and market-wide signals.

- `market_technical.py`: pure market-overview technical helpers for window metrics, regime buckets, support/resistance levels, style rotation, industry strength, breadth sentiment, and similar-scenario backtests. It does not read databases, files, env, or network.
- `market_heatmap.py`: pure industry heatmap payload normalization from persisted quote rows.
- `sector_quote.py`: pure TDX sector quote detail payload wrapping and section-count metadata.
- `index_returns.py`: pure broad-index window return calculations and default index target definitions.
- `index_codes.py`: pure TDX industry/concept index code catalog and lookup helpers.
- `msi_factors.py`: pure MSI factor formulas plus limit-emotion stock snapshot extraction; it does not write storage.
