# THS Industry Constituents Pipeline

`sync_ths_industry_constituents` migrates the legacy q.10jqka industry constituent producer.

Layering:

- Source: fetch q.10jqka pages with `hexin-v`, parse GBK HTML tables, and dedupe by source stock code.
- Domain: normalize six-digit stock codes and convert quote fields into a stable snapshot contract.
- Storage: replace requested `industry_code` snapshots in PostgreSQL.
- Job: Java can trigger `sync_ths_industry_constituents` through the shared Trigger API or CLI module.

Inputs:

- Live: `industry_codes=881103,881101`.
- History: `history_path` pointing to a legacy `881103.json` file or a directory of per-industry JSON files.

Out of scope:

- Legacy Flask drawer APIs.
- Old JSON runtime cache and heatmap rendering helpers.
- UI joins against old local files.
