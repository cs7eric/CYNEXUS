# Findings

- 当前 active 损坏 part 确认覆盖 2014、2015、2019、2025，均为 `trade_time.bin` 的 `UNKNOWN_CODEC`。
- 这些分区当前只有 `source='parquet_1m'`、`period=1` 数据，可从 `F:\data\stock_1min` 重建。
- `ch_dwd_kline_1m` 的 `trade_time` 位于排序键 `(market, symbol, period, trade_time, source)`，不能单独替换物理列文件。
- ClickHouse 表使用 `ReplacingMergeTree`，可通过同结构 staging 表导入后执行 `REPLACE PARTITION ... FROM staging`。
- 当前 Data Executor 容器没有挂载 `F:\data\stock_1min`，需要增加只读挂载或使用等价容器路径。

