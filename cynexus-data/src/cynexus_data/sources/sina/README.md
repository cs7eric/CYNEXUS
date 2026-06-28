# sources/sina

Sina source adapter.

Implemented surface:

- `CN_MarketData.getKLineData` minute K-line rows for provider fallback.

The parser is tolerant of malformed rows, supports second/minute timestamps, and emits `volume_ratio` parity metadata. Source modules fetch and parse only; persistence belongs in K-line pipelines.

Use this folder for Sina client calls, parsers, schemas, and adapters. Do not write storage records directly from source clients.
