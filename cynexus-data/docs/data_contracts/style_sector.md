# Style Sector Snapshot Contract

`build_style_sector` produces lightweight style-sector change snapshots for Java/UI reads.

## Inputs

- `sec_sector` / `sec_stock_sector` category `4` rows from `sync_stock_sectors`.
- Tencent qt realtime stock snapshots for constituent change percentages.

## Output Table

`cynexus_data.market_style_sector_snapshot`

- `trade_date`: snapshot date.
- `name`: style sector name, such as `昨日涨停`.
- `rank_by_change`: rank after sorting by `change_pct` descending.
- `change_pct`: equal-weight average constituent change percentage.
- `valid_size`: constituents with usable quote data.
- `sample_size`: total constituents in the style sector.
- `payload`: full compact snapshot row, including stock codes.

Raw realtime quote rows are not stored here; this is a compact serving snapshot.
