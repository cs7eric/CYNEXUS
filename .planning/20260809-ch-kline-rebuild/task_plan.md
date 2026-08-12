# ch_dwd_kline_1m 坏分区安全重建

## Goal

为 `ch_dwd_kline_1m` 提供可复用、可 dry-run、可校验的 Parquet 年份分区重建流程，并在确认后重建当前确认损坏的 2014、2015、2019、2025 分区。

## Phases

- [completed] 1. 设计并实现可复用的分区重建 job（staging + 校验 + REPLACE PARTITION）
- [completed] 2. 构建/更新 Data Executor，完成受限 dry-run 和运行时挂载验证
- [completed] 3. 执行四个年份分区重建并逐分区验证
- [completed] 4. 做 active part/查询回归检查，记录恢复结果和后续防护建议

## Safety decisions

- 不直接修改或覆盖 `trade_time.bin`。
- 不先 `DROP PARTITION`；先写入独立 staging 表并验证。
- 只有全部源数据校验通过后，才使用 `REPLACE PARTITION ... FROM staging` 替换目标分区。
- 默认 dry-run；执行动作必须显式 `execute=true`。
- staging 表按 run 复用，失败时保留以便诊断，不自动清理。

## Errors Encountered

| Error | Attempt | Resolution |
|---|---|---|
