# Findings

## Current Task

- 用户反馈当前页面数据重复、缺少行业关联、无法点击行业查看成分股，并要求从使用者角度继续优化。
- PRODUCT.md 定义目标用户为盘中/盘后 A 股研究、交易和复盘用户；页面应优先扫描、对比、快速切换标的。
- DESIGN.md 的视觉方向是“安静的研究桌”：中性表面、稳定密度、语义色、A 股红涨绿跌、明确快照时间。

## Initial Hypothesis

- “都是 9.42” 可能来自当前采集快照中的 `lastPrice` 或 `changePct` 字段异常/映射，必须直接检查 API 返回及数据库分布，不能只调整 UI。
- 关联程度需要建立在现有 `sec_stock_sector`、`sec_sector`、行业快照和股票快照之上；输出应是可解释的联动线索，不直接宣称因果。

## Data Findings

- 当前 API 快照字段是变化的：前 12 行价格从 `11.14` 到 `1.50` 不重复；数据库最新有效快照 5509 条，`last_price=9.42` 只有 3 条，`change_pct=9.42` 为 0 条。此前“都是 9.42”更像是页面缺少筛选/下钻和上下文造成的感知问题，需要用多字段表格与选中详情解决。
- PostgreSQL 当前有约 5515 只股票关联到 `category_raw=0` 的行业、2713 个行业节点；现有 API 只返回最多 100 个行业，并且没有行业成分股接口。
- `sec_stock_sector` 有 `relation_level`、`reason`、`source` 字段，可以作为关联权重的基础；`category_raw=0` 可作为行业主关系，其他类别可作为关联主题/概念。
- 当前 Market Indicator DTO 只有 overview/snapshot/industry/event，没有关系或成分股结构，需扩展 API 与前端 service。
