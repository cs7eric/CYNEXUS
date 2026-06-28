# sources/tdx

TongDaXin source adapter.

Current code:

- `local_minute.py`: reads local TDX minute files and normalizes them for the 1m ClickHouse writer.
- `local_day.py` and `parser.py`: read local `.day` files.
- `hsjday.py`: downloads/verifies/extracts TDX daily history zips.
- `adjusted.py`: optional `eltdx` adjusted daily K-line client helpers.
- `market_overview.py`: optional `eltdx` A-share amount/breadth live capture.

Source modules fetch and parse only; persistence and job/checkpoint state belong in pipelines/runtime.
