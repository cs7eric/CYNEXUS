# Data Source Capability Matrix

This matrix tracks provider capabilities after the legacy migration. Sources fetch and normalize data only; pipelines own persistence.

| Provider | Capability | New Modules | Persistence Owner |
| --- | --- | --- | --- |
| TDX local | 1m local minute K-line, `.day` daily raw, hsjday zip sync | `sources/tdx/*`, `pipelines/kline/*` | ClickHouse writers in kline pipelines |
| eltdx | adjusted daily K-line, stock topics, market overview breadth, auction/timeshare detail, industry quotes, sector quote details, stock share profile | `sources/tdx/*`, `hooks/sources/tdx.py` | Kline, market, stock-basic, MSI pipelines |
| EastMoney | kline fallback, market breadth, stock metadata enrichment | `sources/eastmoney/*`, `hooks/sources/eastmoney.py` | Kline and market-overview/stock-basic pipelines |
| Tencent | kline fallback, qt realtime snapshots, stock fund-flow, style-sector quote input | `sources/tencent/*`, `hooks/sources/tencent.py` | Kline, fund-flow, market-overview, style-sector pipelines |
| Sina | minute K-line fallback | `sources/sina/*`, `hooks/sources/sina.py` | Kline pipelines |
| THS | industry fund-flow, industry constituents | `sources/ths/*`, `hooks/sources/ths.py` | Fund-flow and stock-basic pipelines |
| AkShare | market overview breadth/amount and fund-flow fallback | `sources/akshare/*`, `hooks/sources/akshare.py` | Market-overview pipelines |

## Boundaries

- Large OHLCV and high-frequency facts go to ClickHouse.
- PostgreSQL stores job state, checkpoints, metadata, compact factor results, and UI-serving snapshots.
- Source modules do not write databases or manage job state.
- Domain modules stay pure and do not read env, network, files, or databases.
