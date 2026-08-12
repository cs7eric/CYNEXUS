# Progress

## 2026-08-09

- 创建重建计划。
- 完成损坏分区、表结构、Parquet 源路径和 Data Executor 挂载情况检查。
- 新增 `rebuild_kline_1m_partitions` job：默认 dry-run，使用同结构 staging 表、源行数/日期/标的/source/period 校验，并以 `REPLACE PARTITION` 原子替换；`max_files` 被限制为 dry-run 参数，避免误用部分源文件覆盖完整分区。
- `read_parquet_1m_arrow` 增加 inclusive `end_date` 与多年份过滤，重建 job 按 worker 聚合多个文件后批量写入，减少 ClickHouse insert 次数。
- Data Executor 已重建并重新创建，挂载只读 `/app/parquet/stock_1min`（宿主机 `F:/data/stock_1min`），health 与 job registry 已验证。
- 2026-08-09 执行成功：从 5795 个 Parquet 文件写入 staging 后替换 2014、2015、2019、2025 四个分区；重建源/目标行数分别为 149342880、143741317、213702412、318300163。
- 替换后四个分区均通过 count、日期范围、uniqExact(symbol)、source、period 校验；已确认原 5 个损坏 part 不再 active，抽样 `002579.SZ` 的 2014/2025 分钟数据可读。
- 已执行 `SYSTEM START MERGES cynexus.ch_dwd_kline_1m` 恢复后台合并。
