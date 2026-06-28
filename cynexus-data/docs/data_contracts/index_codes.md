# Index Codes Data Contract

`domain/market/index_codes.py` contains the scoped TDX industry/concept index catalog migrated from legacy F10 helpers.

The catalog is pure in-memory data:

- Industry index codes: `sh8803XX`.
- Concept index codes: `sh8804XX`.
- Optional extra items can be passed to lookup helpers by callers.

No file-system override loading is performed in domain code. If runtime overrides are needed later, a pipeline or service layer should read them and pass `extra_items`.

Main helpers:

- `normalize_index_code`
- `infer_index_kind`
- `get_industry_codes`
- `get_concept_codes`
- `all_index_codes`
- `lookup_index_name`

## TDX 56 Industry Catalog

The legacy 56-industry TDX catalog is also exposed as pure data:

- `get_tdx_industry_56`
- `get_tdx_industry_56_pairs`
- `get_tdx_industry_name`
- `tdx_industry_seed_for`

Seed stocks are used by legacy fund-flow probes as representative constituents. The migrated catalog keeps these seeds in domain data but does not perform provider calls.
