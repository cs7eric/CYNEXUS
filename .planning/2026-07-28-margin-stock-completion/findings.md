# Findings

- Release API 当前个股两融总量为 4090 条：SSE 1993，SZSE 2097，行数本身完整。
- 2026-07-27 的 SSE 官方明细 1993 条全部缺 `margin_balance` 和 `securities_lending_balance_amount`；SZSE 官方明细 2097 条全部缺 `financing_repay_amount`。
- EastMoney `RPTA_WEB_RZRQ_GGMX` 返回完整字段：`RZYE` 融资余额、`RZMRE` 融资买入、`RZCHE` 融资偿还、`RQYL` 融券余量、`RQYE` 融券余额、`RQMCL` 融券卖出量、`RQCHL` 融券偿还量、`RZRQYE` 两融余额。
- 当前 `_eastmoney_stock_margin_row` 把 `RQYE` 错映射到 `securities_lending_balance`，且没有写入 `securities_lending_balance_amount`，需要修正。
- `margin_stock_history_backfill` 当前每天只处理 1 只自选股，所以大多数个股 Drawer 只有当日官方数据；需要扩大待回填代码选择范围，同时保持限流可控。
