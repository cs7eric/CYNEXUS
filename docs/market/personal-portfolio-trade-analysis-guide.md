# 个人交易记录分析 Guide

本文规定如何读取 Cynexus 个人投资账本，并对最近交易进行可复核的分析。分析只使用已确认且有效的交易，不把 AI 候选记录当作交易事实，也不提供自动交易建议。

## 1. 数据来源

默认从 Release Gateway 读取 Portfolio API：

```text
GET http://localhost:18000/api/market/portfolio/trades
GET http://localhost:18000/api/market/portfolio/overview
GET http://localhost:18000/api/market/portfolio/performance?range=...
```

如果 Gateway 地址变化，以当前 Release 配置或 UI 的 `VITE_CYNEXUS_API_BASE` 为准。不要直接读取数据库表代替 API，因为 API 负责 owner/tenant 隔离、状态过滤和 DTO 映射。

PowerShell 读取示例：

```powershell
$base = "http://localhost:18000/api/market/portfolio"
$trades = ((Invoke-WebRequest -UseBasicParsing "$base/trades").Content | ConvertFrom-Json).data
$overview = ((Invoke-WebRequest -UseBasicParsing "$base/overview").Content | ConvertFrom-Json).data
```

读取失败时，记录 HTTP 状态、响应错误和时间，不要用缓存数据假装最新结果。

## 2. 交易记录字段

每条交易至少检查以下字段：

| 字段 | 含义 | 校验要求 |
| --- | --- | --- |
| `id` | 交易稳定 ID | 同一记录分析时保持不变 |
| `symbol` | 证券代码 | 代码与证券名称必须匹配 |
| `securityName` | 证券名称 | 仅作展示，不能替代代码 |
| `instrumentType` | `STOCK` 或 `ETF` | 分析时分组保留 |
| `side` | `BUY` 或 `SELL` | 只能取这两个值 |
| `tradedAt` | 成交时间 | 按时间、再按 ID 排序 |
| `quantity` | 成交数量 | 必须大于 0 |
| `price` | 成交价格 | 必须大于 0 |
| `totalFee` | 总费用 | 费用不能重复扣除 |
| `status` | 交易状态 | 只把 `ACTIVE` 纳入账本计算 |

## 3. 标准分析流程

### 3.1 读取并清洗

1. 读取 `/trades` 和 `/overview`。
2. 检查响应 `code` 是否为 `0`，`data` 是否存在。
3. 统计总记录数，并单独统计 `ACTIVE`、`VOIDED` 等状态。
4. 排除非 `ACTIVE` 记录，但在报告中保留状态异常提示。
5. 检查空代码、非法方向、负数数量/价格、重复 ID 和不合理时间。

示例：

```powershell
$active = @($trades | Where-Object status -eq "ACTIVE")
$voided = @($trades | Where-Object status -eq "VOIDED")
"total=$($trades.Count) active=$($active.Count) voided=$($voided.Count)"
```

### 3.2 提取最近交易

默认取最近 10 笔；需要行为分析时取最近 30 天或最近 20 笔，并明确口径：

```powershell
$recent = $active |
  Sort-Object @{Expression={ [datetime]$_.tradedAt }; Descending=$true}, @{Expression={ [long]$_.id }; Descending=$true} |
  Select-Object -First 10
```

输出必须包含时间、方向、代码、名称、数量、价格、费用和状态，避免只展示盈亏结论而没有事实依据。

### 3.3 按证券汇总

对每个 `symbol` 分组，计算：

- 买入数量、卖出数量、净持仓数量。
- 买入金额：`quantity x price + totalFee`。
- 卖出净收入：`quantity x price - totalFee`。
- 交易次数、最近交易时间、买卖方向比例。
- API 返回的已实现盈亏、未实现盈亏和当前市值。

注意：买入总额减卖出净收入不是已实现盈亏，因为它还包含当前未平仓交易的成本。已实现盈亏应使用系统账本的移动加权平均成本结果。

### 3.4 检查持仓和风险集中度

从 `/overview.positions` 读取当前持仓，检查：

- `quantity > 0` 的标的及其 `marketValue`。
- 单标的市值占持仓市值比例。
- 未实现盈亏和收益率。
- `latestPrice` 是否为空、行情日期是否新鲜。

集中度公式：

```text
单标的占比 = 单标的 marketValue / 所有正持仓 marketValue
```

不要把现金误算成持仓集中度；现金应单独报告。

### 3.5 识别交易行为问题

按时间序列逐标的查看交易，重点识别：

- 亏损后继续补仓，且没有相应的风险上限。
- 短时间内反复买卖，价差不足以覆盖费用和滑点。
- 下跌卖出后快速追回，形成追涨杀跌。
- 盈利交易过早卖出，而亏损交易长期持有。
- 同一标的价格出现不合理断层，可能是代码、复权口径或 OCR 错误。

这些是执行行为信号，不等同于对未来价格的预测。

## 4. 数据质量检查

报告结论前必须检查：

1. `overview.accountUpdatedAt` 是否晚于最近交易时间。
2. 有持仓的标的是否有 `latestPrice`。
3. `valuationStatus` 是否为可用状态；`stale` 或 `unavailable` 必须明确标记。
4. 买卖数量是否满足卖出不超过历史持仓。
5. 证券名称是否在同一代码下发生异常变化。
6. 是否存在 `VOIDED`、重复交易或时间默认为当天零点的记录。
7. 价格断层是否可能来自 ETF 拆分、复权或单位错误。

只要账户更新时间滞后、行情缺失或价格口径未确认，就不能把总盈亏描述成精确的实时收益率。

## 5. 报告输出模板

按以下顺序输出：

1. **数据范围**：读取时间、最近交易时间、有效/作废记录数。
2. **账户摘要**：现金、成本、市值、已实现盈亏、未实现盈亏、估值状态。
3. **最近交易表**：最近 10 笔事实记录。
4. **主要盈亏来源**：按标的列出已实现和未实现盈亏。
5. **行为问题**：只引用可追溯的交易序列和数字。
6. **数据异常**：单独列出，不与投资判断混在一起。
7. **改进建议**：围绕仓位上限、止损规则、交易频率和记录核对，不给出买卖指令。

## 6. 口径限制

只有成交记录时，不计算完整账户累计收益率、XIRR 或 TWR。因为缺少可靠的初始资金、入金、出金、分红和现金余额。可以报告的比例包括：

- 有明确成本分母的单个持仓未实现收益率。
- 已完成交易周期的收益率。
- 单标的市值占当前持仓市值比例。

所有金额和比例都应注明估值日期、数据状态和是否包含费用。
