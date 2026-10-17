# 企业级选股与跟踪闭环

## 目标

在已有多维指标筛选基础上，构建可审计、可回放、可跟踪的企业级选股闭环：市场状态与行业机会 → 策略/指标候选 → 机会信号与证据 → 历史回测与当日健康度 → 观察池与复盘决策；AI 仅在完整能力合同、Prompt、Provider 路由和审计齐备后启用。

## 阶段

- [completed] 1. 读取项目指导并盘点仓库结构
- [completed] 2. 盘点市场数据、查询、指标和数据执行能力
- [completed] 3. 盘点前端页面、路由和当前可呈现形态
- [completed] 4. 盘点接口、运行环境和可复用链路
- [completed] 5. 输出现有能力地图、缺口与后续建议
- [completed] 6. 以真实覆盖的数据扩展并重构 Cynexus Screener：分类、价格/波动、多周期动量、流动性/规模和主力资金流；保留单股研究上下文，并明确财务维度尚不可用
- [completed] 7. 补齐全市场行业关联与高覆盖日频因子：以 THS 行业成分表提供行业选择、行业归属、换手率、量比和 PE 覆盖语义；再评估概念/风格关联的可用性
- [completed] 8. 建立分层板块筛选：以 THS→TDX 映射提供行业大类，并接入 TDX 概念、风格成分的选择和交集筛选
- [completed] 9. 结果行接入 Trade Review 单股复盘 panel，替换 Application Analysis 跳转
- [completed] 10. 为选股条件构建器的全部分类区块提供折叠交互
- [completed] 11. 修复调整选股工作区分栏时 Dialog 被外部交互关闭的问题
- [completed] 12. 将条件构建器扩展为指标、策略与 AI 选股模式标签
- [completed] 13. 明确企业级闭环合同、数据边界、阶段验收与当前能力缺口
- [completed] 14. 建立策略定义、版本、运行快照、候选证据与观察池的领域/API/持久化合同
- [completed] 15. 实现策略选股 MVP：可版本化策略、可复现运行、候选评分/证据与 Screener 策略 Tab
- [completed] 16. 实现观察池工作流：加入/移除、关注原因、失效条件、跟踪状态与 Trade Review 联动
- [completed] 17. 实现机会信号：日频先行、盘中增量预留，提供触发时间、证据、去重和生命周期
- [completed] 18. 实现无未来函数的策略回测与绩效评估：交易日对齐、持有规则、成本、基准、分市场阶段指标（已完成异步队列、Release smoke 与真实网关验收）
- [completed] 19. 实现市场状态与策略健康度：策略运行快照驱动的覆盖/新鲜度/信号密度检查，以及 Valid/Weakening/Invalid/Not assessed 判定、建议动作和 UI 可见性
- [completed] 20. 扩展策略回测为可审计的单标的模式：历史日期按同一受治理策略条件精确校验标的，保留无未来函数、成本、基准与 owner/tenant 隔离
- [completed] 21. 补强策略健康度：近期命中、后续表现与退化监控
- [completed] 22. 实现 AI 选股辅助：完整 capability/prompt/provider/binding/结构化输出/审计与 UI，禁止无合同空接入；已完成真实 Release 调用、可信候选一致性与执行审计验收
- [completed] 23. 完成 Agent 只读查询、数据质量/回填、端到端验证、Release 部署与产品/运行文档
- [completed] 24. 将 AI Screener 升级为“自然语言意图 → 可编辑计划 → 历史形态硬筛 → 受控研究解释”，先交付缩量后爆量大阳线这一垂直切片
- [completed] 25. 将选股升级为可迭代闭环：AI 意图规划输出可执行指标与不确定范围、人工编辑后可保存/更新为策略、全市场与目标回测、多个观察名单及持仓后收益观测
- [completed] 26. 补强观察名单收益观测：从列表摘要扩展为单标的、入选后相对基准的有界历史曲线与可审阅证据

### Phase 25 delivery ledger

- [completed] AI intent planning: managed capability, bounded executable criteria, editable assumptions and visible route metadata.
- [completed] Target-list backtest: up to 50 normalized market/code symbols, deduplicated and evaluated exactly on every historical signal date; list and legacy single-symbol modes are mutually exclusive and persisted in the immutable config JSON.
- [completed] User strategy create/update: owner-scoped definition, immutable versions, change summary and optimistic concurrency.
- [completed] Named watchlists and return observation: parent lists, candidate entry snapshot, benchmark excess return and review lifecycle.

### Phase 25 next slice

- [completed] Named watchlists and return observation: replace the implicit single list with owner-scoped named lists, freeze the candidate entry context, and add a bounded performance read with benchmark excess return.

### Phase 25 current slice

- [completed] User strategy create/update: catalog is owner-scoped; user changes append immutable versions under an optimistic definition lock; the explicit user write remains outside the autonomous Agent tool registry because no preview/confirmation/audit Agent lifecycle is introduced in this slice.

### Phase 26 delivery ledger

- [completed] Watchlist performance history: owner-scoped, 30–252 point daily series from the frozen entry date; stock return, SH:000300 benchmark return and excess return remain inspectable with source and availability warnings.
- [completed] Watchlist UI: each item provides an Observe dialog with entry context, latest comparative return and a bounded historical chart without replacing the Trade Review workflow.

## 错误记录

| 错误 | 尝试 | 处理 |
| --- | --- | --- |
| 根目录已有其他任务的 task_plan.md、findings.md、progress.md | planning-with-files 初始化 | 不覆盖根目录文件，改用本目录记录 |
| Impeccable 上下文脚本首次使用了错误的安装路径 | 1 | 改用 `C:\\Users\\Cs7er\\.codex\\skills\\impeccable\\scripts\\context.mjs`，已成功读取产品设计上下文 |
| 规划文件补丁引用了错误的 findings 标题 | 1 | 读取实际文件结构后按现有 Findings 段落追加 |
| 首次 CodeGraph 查询使用了不存在的 `search` 子命令 | 1 | 已确认 CLI 使用 `query`；后续使用 `query/callers/impact` |
| 第一次读取 Screener policy 时误写为 `domain/screener/StockScreenerQueryPolicy.java` | 1 | 实际策略类名为 `StockScreenerDomainPolicy`；后续按 CodeGraph 精确路径读取 |
| 直接执行 Market domain focused test 时本地 Maven 仓库缺少同版本 `cynexus-market-common` | 1 | 改为 reactor 方式 `-pl cynexus-market/cynexus-market-domain -am` 执行，先构建所需内部依赖 |
| Reactor test 通过 PowerShell 传入 Surefire property 时被 Maven 解析为非法 lifecycle phase | 2 | 不再传该 property；使用带引号的 `-Dtest=...` 执行 reactor test |
| Reactor 仅指定目标测试时，上游模块因无同名测试被 Surefire 终止 | 3 | 将 `-Dsurefire.failIfNoSpecifiedTests=false` 作为完整引号参数传入，不再重试此前参数形式 |
| 策略目录按 mode 的 effect 触发读取，被 React lint 判为 effect 内同步 setState | 1 | 将读取移至 Tabs value-change 事件；组件只渲染由父层维护的请求状态 |
| 策略参数 JSON 到 `Partial<Criteria>` 的动态键写入未能保持 TypeScript 判别类型 | 1 | 使用局部 `Record<string, unknown>` 承接已验证键值，再返回受限 `Partial<Criteria>` |
| Market application controller 未声明 `cynexus-common-security`，无法编译可信身份签名验证器 | 1 | 添加同版本 common-security 依赖后重新执行 controller reactor compile |
| 首次 Release 策略运行 smoke 返回 “A trusted stock selection identity is required” | 1 | `market` 容器未注入与 Gateway 一致的 `CYNEXUS_TRUSTED_IDENTITY_SECRET`；在 Release compose 中补齐该环境变量并重新部署 market |
| 首次运行持久化 SQL 将策略代码误当作 run 表直接列 | 1 | run 只关联 `strategy_version_id`；改为按版本 ID 查询，确认 1 次 run、100 candidates、400 evidence |
| 发布等待脚本使用了 PowerShell 内建只读变量 `$PID` 作为 foreach 变量 | 1 | 改用 `$processId`；不影响 Release 构建或服务重建 |
| Phase 17 对 `/api/market/indicator/latest?status=all&limit=10` 的 gateway smoke 在 20 秒内超时 | 1 | 不重复该全量端点；先完成前端静态校验，再以更窄的 `radar/events` 或直接查看 market 容器日志定位数据查询延迟 |
| Phase 17 首轮索引后端点仍在 20 秒超时 | 2 | `latest_candidate_capture` 通过 CTE join 仍扫描约 91.8 万候选；改为标量子查询以使用倒序索引，并让快照查询显式约束交易日 |
| 首次回测数据覆盖检查假设了错误的 ClickHouse 表名 | 1 | 生产客户端实际读取 `daily_raw` / `daily_qfq`；后续按客户端的实际 schema 检查 |
| 单标的回测补丁首次未匹配 Screener 测试辅助构造器的实际换行 | 1 | 读取精确行段后用局部补丁更新构造器，不重复原补丁 |
| 第一次写入 Phase 23 质量记录时假设 findings 已有 Agent 标题 | 1 | 用精确 heading 搜索确认 findings 尚无该段，改为在文件末尾追加独立质量事实 |
| Data Executor 首次物化使用 `lagInFrame` 的 Float64 与 NULL 默认值类型不兼容 | 1 | 将窗口输入显式转为 `Nullable(Float64)`，以 NULL 保留首个交易日没有前收的语义 |
| 首次物化将所有六位 SH/SZ/BJ 行视作 A 股，PostgreSQL 表的 `(source, code, trade_date)` 未存 market，产生跨市场代码碰撞 | 1 | 对齐 A 股板块代码范围，并以一次事务替换已验证的日期范围，清理本次失败写入的非 A 股行 |
| 首次通过终端调用真实 AI 筛选时，客户端 30 秒采集窗口没有接收到响应 | 1 | 不重复提交；从执行审计确认同一请求在 16.9 秒后成功完成，并读取审计快照验证硬筛候选与解释结果一致 |
| Phase 25 首次 Release UI 构建失败 | 1 | `notification` API 需要 `{ title }` 对象而非字符串；已修正保存策略对话框的成功/失败提示，并重新执行 UI 构建与 Release。 |
| Phase 25 resumed snapshot-field inspection used a malformed broad `rg` expression | 1 | No source changed; use fixed-string or individual-term searches for the remaining impact inspection. |
| Phase 25 watchlist UI combined patch did not match the current callback context | 1 | No source changed; split the UI work into service, dialog, sheet, and parent-state patches using the exact local sections. |
| Phase 25 version synchronization search used a malformed alternation expression | 1 | Runtime inspection still confirmed the standard `1.1.400-260816-RELEASE` target; use literal per-file checks after deployment. |
| Phase 26 performance dialog lint rejected synchronous loading-state reset in an effect | 1 | Derive loading from the initially keyed dialog state and only update state from the asynchronous request callbacks. |
