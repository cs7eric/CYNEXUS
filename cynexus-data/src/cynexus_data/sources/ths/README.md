# ths

TongHuaShun source clients and parsers. Keep anti-bot/header concerns in clients and raw HTML parsing in parser modules.

Implemented surfaces:

- `data.10jqka.com.cn` industry fund-flow pages.
- `q.10jqka.com.cn` industry constituent pages.

Live requests require `hexin-v`; optional `akshare` and `py-mini-racer` provide the token generator. Dry-runs and parser tests do not import either optional dependency.
