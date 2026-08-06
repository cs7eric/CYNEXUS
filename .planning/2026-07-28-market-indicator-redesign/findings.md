# Findings & Decisions

## Requirements
- 以产品使用者和竞品视角重新分析 Market Indicator 的功能、逻辑、业务和展示方式。
- 重构布局与数据渲染，符合 Cynexus 的克制、密集、可扫描工作台风格。
- 对过程中需要的新数据和展示逻辑进行完整实现，不停留在静态 mock。
- 遵守 Release 栈验证和 Maven/UI 版本同步规范。

## Research Findings
- 当前页面已经接入 `fetchMarketIndicators`，数据包括 `breadth`、`snapshots`、`limitUp`、`limitDown`、`industries`、`events`。
- 当前页面顺序为：标题 -> signal filter -> 四个同质指标卡 -> 错误 -> 涨跌停 -> 行业 -> 明细+事件；用户需要在第一屏看到的“当前市场处于什么状态”不够突出。
- 当前四个指标只展示数量，没有用比例、强弱差、涨跌停封板质量、数据新鲜度形成判断语句。
- 当前全市场表格把 200 个信号结果同时渲染为唯一主列表，缺少“异动类型/强度/排序/范围”的快速切换。
- 当前筛选使用自定义按钮组，和项目的 Tabs/Select 组件体系不完全一致；refresh 逻辑与每 60 秒自动刷新共用同一个 load，可能让手动刷新和自动刷新状态表达不够明确。
- 项目设计系统要求：WorkspaceShell、semantic tokens、A 股红涨绿跌、数据用等宽数字、loading 用 skeleton、每个数据块有 error/empty/stale 状态。
- 工作区已经存在用户未提交的 Market Indicator 后端、数据管道、迁移和前端初版改动；这些改动不属于本次重构范围，需在其上兼容实现。

## Technical Decisions
| Decision | Rationale |
|----------|-----------|
| 页面主线改为“市场结论 + 关键分布 + 机会/风险列表” | 缩短从打开页面到形成市场判断的路径 |
| 把市场广度从四个独立卡片改成带比例与净强弱的状态面板 | 用户更关心上涨/下跌结构和情绪是否扩散，而不是孤立计数 |
| 采用 tabs 视图切换全市场明细的信号集合，并在表头保留行数与捕获时间 | 控制信息密度，减少单一长表带来的认知负担 |
| 行业和涨跌停作为同一“结构观察”区域 | 让板块集中度与极端价格行为在一屏形成对照 |
| 事件流保留右侧窄栏、移动端下移 | 事件是确认和回溯信息，优先级低于实时行情明细 |

## Issues Encountered
| Issue | Resolution |
|-------|------------|
| FastCtx 工具在当前工具清单中不可用 | 按项目规则使用 PowerShell `rg`/`Get-Content` 作为降级方案 |
| 项目没有可用的浏览器 MCP 工具 | 后续使用仓库已有构建/Release 入口验证；若无法截图则记录为验证限制 |

## Resources
- `cynexus-ui/src/views/market/market-indicator/index.tsx`
- `cynexus-ui/src/services/market/indicator.ts`
- `cynexus-ui/src/views/market/market-indicator/components/`
- `PRODUCT.md` / `DESIGN.md` 输出来自 Impeccable context
- `AGENTS.md`

## Visual/Browser Findings
- 没有可用的浏览器自动化/截图工具；通过 Release UI 路由与真实 API 请求完成运行验证。
- Release 页面路由返回 `200`，Market Indicator API 返回 `200`；2026-07-28 数据为 5518 只股票，API 数据块均有返回。
