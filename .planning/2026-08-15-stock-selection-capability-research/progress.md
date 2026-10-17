# Progress

## 2026-08-16 Phase 24 started

- 用户确认实施自然语言驱动选股。已验证根因：当前 AI 意图只参与候选解释，未进入候选 hard filter；现有快照没有缩量、爆量和大阳线所需的历史形态字段。
- 本阶段目标：交付“持续缩量后 3 倍爆量大阳线”的受控垂直切片，并设计为可扩展的 AI intent plan 合同，而不是把自然语言交给模型自由筛选。

## 2026-08-15

- 用户要求开始落地企业级选股成果；计划已从单纯调研/MVP 扩展为策略、信号、回测、健康度、观察池、AI 与 Agent/Release 的连续交付，并以运行快照和证据链作为共同合同。当前处于第 13 阶段：核查可复用模型、数据覆盖和落地边界，再开始第 14 阶段的领域/API 合同。
- 已完成第 13 阶段的第一轮符号核查：确认策略回测/健康度的若干可复用纯计算和市场事件数据，同时确认现有自选股、交易导入候选与单股前端回测不满足企业级策略运行/观察池/回测的持久化与历史冻结要求。CodeGraph CLI 查询命令已更正为 `query`。

- 已识别根目录规划文件属于既有任务，本次使用独立 `.planning` 会话目录。
- 已确认 CodeGraph CLI 和 FastCtx CLI 可用；当前开始读取项目指导和仓库结构。
- 已读取根目录、市场、数据和 UI 指导文件；确认本轮不能启动本地后端，且现有工作区包含大量既有未提交变更。
- 已完成第一轮 CodeGraph 查询和市场 API/数据任务盘点，发现已有大量市场研究数据与 Agent 只读能力，但暂未发现专门的全市场条件选股合同。
- 进一步确认仓库已有“为选股准备的落库基础”：`a_stock_financial_snapshot` 与 `a_stock_daily_screening_fact` 的迁移和 Python 财务快照归一化逻辑已存在，但尚没有对应 Java 查询模型、筛选接口或 UI。
- 确认 `market_indicator_candidate_pool` 是盘中指标采集的有限候选池，不是用户定义条件的通用选股池；当前默认通过热度、轮换和覆盖策略控制约 800 个候选，UI 只提供异动/涨跌停/快速变化视图。
- 确认现有正式产品形态是市场驾驶舱、行业/板块研究、单股深度分析、自选股和原始数据集浏览；没有独立“条件构建器 + 全市场结果表 + 保存筛选器”的产品链路。
- 确认可直接支撑 MVP 的字段主要来自股票基础、行业/概念/风格、日线/市值/流动性、资金流、市场指标事件和财务快照；但字段的全市场覆盖、刷新时间和来源口径需要在新接口返回中显式标注。
- 收到 UI 偏离既定方案的反馈，已确认问题在信息架构而非仅视觉边框：当前路线页增加了无用的空白启动层，且选股 Dialog 未呈现“市场语境 → 条件构建 → 结果研读 → 单股研究”的连续工作流。开始按该工作流重构。
- 基于竞品结构与数据覆盖，完成多维日频 MVP：新增上市板、振幅、5/10/30/60/252 日收益和主力净流入（当日/5 日）条件及排序；同步扩展 API、Feign 合同和受边界限制的 `market.stock_screener` Agent tool。
- 完成 Dialog panel 工作区重构，路由进入即打开；结果表含新增维度、数据来源/快照/警告和单股 Sheet，再显式进入 Application Analysis。
- 已通过 domain focused test、Market compile、AI Agent focused test、frontend ESLint/build 与版本同步检查。
- 已使用 `start-release.ps1 -Build -Version 1.1.400-260815-RELEASE -Services market,ai,ui` 重建并重启 Release 服务；网关 API smoke 成功返回 ChiNext 多维筛选结果（64 条匹配，首行含 30 日收益、当日/5 日主力净流入），UI `5173/cynexus-screener` 返回 200。
- 已核对 Agent capability 治理表：`market.stock_screener` 的 1.1.0 合同为 `AVAILABLE`、`is_enabled=true`、`HANDLER`；旧 1.0.0 合同已按版本淘汰为 `STALE`。
- 已补齐第二轮覆盖：行业筛选改为 90 个真实 THS 行业成分关联，结果与单股详情展示 THS 行业；新增全市场换手率、量比及部分覆盖的 PE 筛选、排序和表格字段。概念/风格关联虽存在结构表，但当前“概念”数据只有 8 支股票，不能误作为全市场条件。
- 已完成第二轮 Market domain/compile、AI Agent focused test，以及 UI ESLint/build；待 Release 部署并验证真实行业筛选与新版 Agent 合同。
- 已部署 Release `1.1.400-260815-RELEASE`（market、ai、ui）。网关验证返回 90 个 THS 行业选项（2026-08-15）；以“半导体”行业、换手率≥1%、量比≥1、换手率排序实测返回 62 支，结果含 THS 行业、换手率、量比和 PE。`market.stock_screener` Agent 1.2.0 为 `AVAILABLE`，1.0.0 / 1.1.0 已为 `STALE`。
- 收到分层分类需求：已验证 THS→TDX 映射覆盖 90 个 THS 叶子/54 个 TDX 行业大类，且现有 TDX 概念、风格关系实际覆盖分别为 5,393 / 5,279 只股票；开始接入大类、概念与风格选择。
- 已实现：新增 TDX 行业大类、THS 行业叶子、TDX 概念与 TDX 风格四组分类选项；行业大类通过日频 THS 成分与映射表生成，概念/风格由 `sec_sector` / `sec_stock_sector` 提供。四类条件均在领域层与其它条件做交集筛选，结果详情返回分类成员。
- 验证：Market domain focused test、AI Agent focused test、UI ESLint/build 均通过。首次 domain test 运行因旧测试字节码仍引用旧 record 构造器失败，更新测试构造器后通过。
- 已部署 Release `1.1.400-260815-RELEASE`（market、ai、ui），并实测 `/api/market/screener/classifications` 返回 52 个映射后的 TDX 行业大类、90 个 THS 叶子、269 个 TDX 概念和 146 个 TDX 风格。以半导体（880491）+ 芯片概念（2:31）+ MSCI中盘风格（4:2715）交集实测返回 8 支，返回行含三类分类成员。Agent `market.stock_screener` 1.3.0 为 `AVAILABLE`，旧版本均已 `STALE`。
- 已纠正 Screener 的结果点击复盘入口：不再使用 `StockDetailDialog`，而是按原 Trade Review 的全屏 `TradeReviewPanel` 打开，包含分时/分钟回放、回放游标、行业与大盘联动、逐笔、两融、财务、资金流和市场状态 tabs；复盘交易日取本次筛选的日频快照。该重型 panel 仅在点击股票后动态加载。`pnpm exec eslint src/views/cynexus-screener/index.tsx src/views/market/trade-review/index.tsx`、`pnpm build` 以及 `http://localhost:5174/cynexus-screener` HTTP 200 均通过。本次为 UI-only，未重建 Release UI。
- 条件构建器已将 Universe、Price & volatility、Momentum、Liquidity, size & capital 与 Valuation coverage 全部改为可折叠区块；每项默认展开，点击标题即可收起或再次展开，箭头同步反映状态。`pnpm exec eslint src/views/cynexus-screener/index.tsx`、`pnpm build` 及 `http://localhost:5174/cynexus-screener` HTTP 200 均通过。
- 已修复调整 Screener 中央分隔条时 panel 意外消失：根因是外层 Dialog 未像 Trade Review 复盘 panel 一样拦截拖拽过程引起的 Radix 外部 pointer/focus/interact 事件，导致 Dialog 改为关闭状态。现已拦截三类外部交互；关闭仅保留右上角关闭按钮与 Escape 行为。`pnpm exec eslint src/views/cynexus-screener/index.tsx` 与 `pnpm build` 均通过。
- 已将 Criteria builder 顶部扩展为 `Indicators`、`Strategies`、`AI screening` 三个可切换标签：现有多维日频条件与运行逻辑全部保留在 Indicators；Strategies 与 AI screening 以明确的非执行状态预留各自的产品契约，避免将尚未注册的策略或模型能力伪装为可用。AI tab 的提示明确其上线前需要能力、Prompt、Provider 路由、输出合同和审计一起完备。`pnpm exec eslint src/views/cynexus-screener/index.tsx`、`pnpm build` 与 HMR `5174/cynexus-screener` HTTP 200 均通过。
- 已为三个模式标签补充语义图标：指标使用图表、策略使用工作流、AI 选股使用机器人图标；`pnpm exec eslint src/views/cynexus-screener/index.tsx` 与 `pnpm build` 均通过。
- 恢复本轮企业化实施上下文：已读取策略计划、DDD/部署/Agent 工具指导，并执行 CodeGraph 状态与 `StockScreenerQueryDomainService` 影响检查。索引有待同步变更；下一步先同步后完成第 13 阶段的模型、数据库和调用链核查，再冻结第 14 阶段合同。
- 已完成 CodeGraph 同步并核验共享调用边界：Screener repository 只由领域服务和其 focused test 使用，适合被策略运行领域服务安全复用；`MarketResearchFeignClient` 影响范围较大，因此不在策略 MVP 早期无必要扩展。
- 已完成对旧指标关注清单、Screener BO/API 和自选清单持久化样式的结构搜索；确认策略/观察池必须新建独立、类型化的领域合同和持久化链路，不能复制旧 Map/JSON 关注清单实现。
- 已读取现有 Screener 的 BO、领域服务、应用映射、Repository 实现及筛选预留事实表 DDL。已确认首批策略可以冻结现有日频筛选输入/实际快照/排序候选与 source，而不依赖尚未成熟的财务覆盖；继续核查持久化、用户身份与 migration 编排后即可完成第 13 阶段。
- 已确认具体落地约束：策略 MVP 采用受控 `SCREENER_PRESET` 而非自由 JSON 条件语言；数据写入遵循 Market 的 MyBatis-Plus/Snowflake 基础设施模式。正在补齐 owner 身份和 Release migration 编排核查。
- 已完成 owner/migration 核查：现有 Market 列表不具备显式 tenant/owner 隔离，不能复用；新策略和观察池表需显式写入可信身份范围，迁移必须同时登记 Release 白名单。接下来完成基础审计列/身份适配的最后核验，并冻结 Phase 14 合同。
- 第 13 阶段已完成并通过设计验收：策略 MVP 采用三个受控日频策略，策略运行冻结可解释证据；观察池为独立人工决策聚合；财务/盘中/回测/AI 延后。已发现签名的网关注入身份机制，Phase 14 正在核查其服务端解析接口后开始落库。
- 已完成身份机制核查：当前通用基础 PO/审计不能保障用户隔离，Portfolio 的硬编码身份不可复用。Phase 14 将采用 Market Application 的签名身份解析器 + domain `StrategyScope`，并在新表显式保存 tenant/owner。
- 已确认 Phase 14 实现模板：在 Market 内部独立实现签名身份解析，策略领域采用 port/adapter/configuration 装配。正在读取 Portfolio import 的具体持久化 adapter，随后开始写入类型化策略合同、迁移与第一批固定策略 seed。
- 已完成 Phase 14 的实现设计：复用 Portfolio import 的 owner-scoped repository/并发语义，策略定义与版本只读，运行事实不可变，观察池独立维护状态与 review 历史。下一步开始实现迁移、domain port/BO、infra adapter、Application API 及前端策略 Tab 的真实列表入口。
- 已读取 Portfolio import 的 port、adapter、mapper 与 XML，实现所需的 owner scoped SQL、时间审计和乐观更新方式已明确；开始 Phase 14 代码落地。
- 已落地 Phase 14 的首个垂直切片：新增策略全量生命周期表 DDL（含运行、候选、证据、观察池、review）并登记 Release market migration；新增受治理策略目录的 DDD 读取 API 和三个策略 seed。下一步运行影响分析与 Market compile，修复结果后再接入真实 Strategies Tab。
- 已新增策略目录领域 focused test，覆盖 active catalog 映射和空 repository 的安全语义；下一步执行该测试与 Market compile。
- 首次 focused test 未能启动：直接模块构建因本地 Maven 仓库不存在 `cynexus-market-common:1.1.400-SNAPSHOT`。按三次错误协议改为 Maven reactor `-am`，避免重复同一失败命令。
- 第二次 focused test 未启动：PowerShell/Maven 将未加引号的 Surefire property 解析成 lifecycle phase。将使用带引号的标准 `-Dtest` reactor 方式继续，未重复失败的命令。
- 第三次发现 reactor 上游模块没有目标测试时 Surefire 会失败；改为将 `failIfNoSpecifiedTests` 作为完整引号参数传入。该参数形式与首次失败的未加引号调用不同。
- 验证结果：策略目录 domain focused test 通过（2 tests）；Market API/domain/application-controller 编译通过。随后启动 Market infra reactor compile，输出在编译阶段被会话截断，下一步先确认最终 exit 状态再继续 UI 接入。
- 已确认 Market infra reactor compile 最终通过。已进入前端实现阶段，读取了 Cynexus frontend/React 性能工作流和 UI 设计规范；将把 Strategies Tab 的静态提示替换为真实策略目录、明确的数据状态与下一步动作。
- 已完成前端结构影响检查：页面和既有 Screener service 是唯一直接受影响方。将按需加载策略目录，保留现有指标模式的加载路径和 Trade Review 动态加载边界。
- 已替换 Strategies Tab 的静态占位：新增策略目录 service、按需查询、loading/error/retry/empty 状态和策略版本卡片；“Preview rules” 会先重置后原子加载受治理条件至 Indicators，避免旧条件与策略定义混用。下一步执行 UI lint/build，修复 TypeScript 问题后开始 Phase 15 的策略运行持久化。
- UI lint 首次发现策略目录在 effect 内触发同步 state 更新；已按 React 规范改为在 Tabs 切换事件中按需发起请求，使 `StrategyCatalogPanel` 仅负责渲染。正在重新执行 lint/build。
- 二次 UI build 发现 JSON 参数转换的动态字段赋值不满足 TypeScript 的联合字段类型；已用局部安全映射消除该实现层类型问题，正在重新运行 lint/build。
- Phase 14 已完成：策略/版本、运行快照、候选证据、观察池/review schema 已可随 Release migration 进入数据库；策略目录 API 与 UI 已落地。策略目录 lint 已通过，Vite build 已完成 TypeScript 与 module transform，最终 bundle summary 仍需在下一轮验证中确认。已进入 Phase 15，开始把预览升级为有身份范围和可回放证据的策略运行。
- 已开始 Phase 15 后端落地：新增策略运行 BO/port、日频版本参数执行/评分/证据 policy、Snowflake ID adapter、MyBatis run/candidate/evidence 持久化、签名身份解析和受限 POST 运行 API。下一步编译并增加 domain focused tests，再把策略 Tab 的预览动作接到真实运行结果。
- Phase 15 domain focused tests 已通过（3 tests），Market infra compile 已通过。application-controller compile 发现该模块没有 common-security 依赖，已添加项目同版本依赖；将重新编译后继续 UI 运行入口。
- 已修正 Market application controller 的 common-security 依赖并重新编译通过。版本同步检查确认 Maven/UI 均为 `1.1.400-SNAPSHOT`。当前工作区仍有大量用户/并行任务改动；本轮只新增/更新 Stock Selection、Screener UI、market migration 与规划文件，未清理其他文件。
- 下一步：将 Strategies Tab 的操作升级为调用真实运行 API，并将候选评分/证据显示在结果工作区；然后完成 migration/Release 部署和网关 smoke。Phase 15 仍进行中，尚未部署 Release。
- 已开始将 Strategies Tab 从“Preview rules”升级为真实策略运行：service 已定义运行 API 和冻结候选/evidence DTO；工作区将把运行结果与实时 Indicators 结果明确分开，避免把可审计 run 误显示为可重算的普通筛选列表。候选行支持进入 Trade Review，证据在右侧 Sheet 展示。
- Release 已完成 market/ui 构建、迁移和重建；目录 API 与 UI 路由均返回成功。运行 smoke 发现可信身份链路的部署缺口：Gateway 已有签名密钥，但 Market 容器未注入同一 `CYNEXUS_TRUSTED_IDENTITY_SECRET`，导致运行 API 拒绝请求。已在 Release compose 补齐变量，正在重新部署 Market 后复测。
- Phase 15 已完成并部署：Strategies Tab 可真实运行受治理策略，结果区显示 run/version/snapshot/coverage/截断提示、候选 rank/score/reason，字段级 evidence 用 Sheet 展示，候选可直接进入 Trade Review。UI lint 和生产 build 通过；Release `market,ui` 已重建。网关 smoke 成功：`TREND_MOMENTUM` 命中 206 支、冻结 100 个候选与 400 条 evidence；数据库已验证运行、候选和 evidence 数量。修复并部署了 Market 缺少可信身份密钥注入的 Release compose 缺口。进入 Phase 16：观察池工作流。
## 2026-08-15 Phase 16 continued

- 规范观察池半成品：新增 API DTO 与 AppService，controller 不再直接调用 domain service，并补充复盘命令端点。
- 完成 tenant/owner 绑定的候选校验、活动项幂等、生命周期决策映射、乐观锁和领域单测；前端接入策略候选加入观察池、观察池列表和人工复盘。
- 已通过：`StockSelectionWatchlistDomainServiceTest`（4 tests）、market infra/controller reactor compile、Screener service/page ESLint、UI production build；Maven/UI 版本均为 `1.1.400-SNAPSHOT`。
- Release 已执行：`deploy/release/start-release.ps1 -Build -Version 1.1.400-260815-RELEASE -Services market,ui`。market 与 ui 容器已重建；`GET /api/market/screener/watchlist` 返回空列表 200；用 `candidateId=0` 的无副作用 POST 返回受控领域校验 `candidateId must be positive`。
- Phase 16 complete. 下一阶段为机会信号：日频先行、盘中增量预留，补充触发时间、证据、去重和生命周期。

## 2026-08-15 Phase 17 started

- 已确认市场指标事件事实表具备分钟级唯一去重、来源和触发证据；Screener 开始接入 Signals 面板，复用既有只读市场指标合同。
- Signals 面板已完成：展示事件类型、触发时间、信号值、价格、涨跌幅、来源、涨停状态及数据日期；单击进入 Trade Review。ESLint 与 UI production build 通过。
- 运行时验收暂未完成：两个既有 market indicator gateway read 端点在 20 秒均超时，market 容器在运行且 Nacos 健康注册；下一步须定位 PostgreSQL/Redis 的事件查询延迟后再发布和验收，Phase 17 保持进行中。
- 已定位并上线 `idx_market_indicator_snapshots_capture_valid` 在线 partial index；Release market migration 已将索引建成。网关复测命令仍在等待响应，需继续完成时延验收。

## 2026-08-15 Phase 17 complete

- 清理本任务产生且精确识别的长查询/建索引会话，重建有效的快照 partial index；共享 PostgreSQL、Redis、ClickHouse、Nacos 与 Data Executor 保持运行。
- 修复 MarketIndicator CTE：最新 capture 改为可利用倒序索引的标量查询，并在 5 个快照读取位置加入交易日 join。`EXPLAIN ANALYZE` 为 12.23 ms。
- 执行 Release：`deploy/release/start-release.ps1 -Build -Version 1.1.400-260815-RELEASE -Services market,ui`，再执行 Market 修复的同版本重建。Market Maven reactor package 和 UI production build 均通过。
- 运行时验收通过：`GET /api/market/indicator/latest?status=all&limit=10` 200（1534 ms），`GET /api/market/indicator/radar/events?kind=all&limit=5` 200（1310 ms），`/cynexus-screener` 200。Phase 17 complete；下一阶段为无未来函数的策略回测与绩效评估。

## 2026-08-15 Phase 18 started

- 已恢复计划并读取 DDD、Release 与 Agent 工具规范；将先从现有策略运行、日线数据和已有纯计算能力建立可复现回测合同，再补齐受限读取入口与界面。
- 已完成历史回放调用链核查：策略可按历史日期重放，但现有持久化与日线读取均不足以承载批量、无未来函数的绩效回测；下一步核查 ClickHouse 批量日线与实际覆盖后冻结数据/持有规则。
- 已确认 ClickHouse client 直接提供带起止日期的单标的原始日线读取；首次覆盖查询使用了错误表名，已改按生产 client 的 `daily_raw` schema 继续，不重复失败的查询。
- 已完成回测领域与前端垂直切片：历史交易日内逐日重放受治理策略，固定信号后下一交易日收盘入场、持有 N 个交易日收盘退出、双边成本、`SH:000300` 基准、缺失价格跳过原因和 owner/tenant 隔离均由领域/持久化合同保证；结果独立存入 backtest run/trade 表，不污染策略运行事实。
- 已增加 `POST /api/market/screener/strategies/{strategyCode}/backtests` 和 `GET /api/market/screener/backtests/{backtestId}`；Strategies Tab 提供参数化 Backtest 对话框、绩效摘要、告警和逐笔结果。focused domain tests、Market compile、Screener scope lint、UI production build 与版本同步均通过；全量 UI lint 仍因既有 109 个其它页面 React lint 错误失败，不归因于本切片。
- Agent 暂缓：现有 Market Feign 读取不传播可信 owner/tenant scope，不能安全读取私有回测结果；回测本身是显式用户写操作，故不将其注册为 Autonomous Agent tool。该前置合同已记录至 Phase 21。
- Release smoke 暴露同步回测会因长窗口触发网关超时，且回测配置 JSON 需要明确的 Java Time 序列化；已将回测改为 tenant/owner-scoped 的持久化队列。POST 立即返回 `QUEUED`，单消费者 dispatcher 将任务原子认领为 `RUNNING` 后执行，UI 每 2 秒轮询同一结果；完成或安全失败都会写回同一回测 ID。
- 修复回测读取 SQL 的多表 `status` 列歧义，并新增 `QUEUED`/`RUNNING` 数据库状态约束迁移。Release 已重新构建并部署 market（UI 在前一次部署已构建）。网关验收：POST 在 1 秒内返回 `QUEUED`，GET 随后返回 `RUNNING`，22 秒后同一 ID 成功返回 `SUCCEEDED`、1 条交易、9.7919% 净收益、0.1615% CSI 300 基准收益和 9.6304% 超额收益；`/cynexus-screener` 返回 200。
- Phase 18 complete。已通过 domain focused tests（4）、Market infra/controller compile、Screener scoped ESLint、UI production build、版本同步和 Release `market,ui` 重建。下一阶段应补齐策略有效性监控与当日逻辑健康度。

## 2026-08-16 Phase 19 started

- 已完成 CodeGraph 状态核验（索引 up to date）并检查策略目录、运行、回测和 Screener 前端的调用边界；健康度不修改 immutable strategy run/backtest facts，而是复用受治理策略的同一日频 Screener evaluation 计算派生评估。
- 已新增策略健康度领域合同和规则：`VALID` / `WEAKENING` / `INVALID`，检查 daily snapshot、universe coverage、signal density、freshness，并返回原因、建议动作、source 和字段级检查结果。
- 已新增 `GET /api/market/screener/strategies/{strategyCode}/health`，沿用可信 tenant/owner 身份解析；新增 UI Strategies Tab 的“Today logic still valid?”状态卡，展示候选数、信号密度、快照日、建议和告警。
- Agent exposure 评估结论：该评估依赖受保护策略上下文且可能触发全市场计算，当前 Autonomous Agent Feign 不传播可信 owner/tenant；本阶段明确不注册 Agent tool，保留为 Phase 21 的有 scope bounded-read 合同工作。
- Release 首次 smoke 显示在健康度 GET 中即时重放全量 Screener 约需 16 秒，超过网关 8 秒超时。已改为仅从同一 owner 的最新不可变策略运行读取已审计的 universe/candidate/snapshot 数据，未运行时返回 `NOT_ASSESSED`，避免将高成本全市场重算放在交互读路径。
- Phase 19 complete。健康度 focused domain tests（2，连同关联 run/backtest tests 共 6）通过，Market infra compile、Screener scoped ESLint、UI production build、版本同步均通过。Release `1.1.400-260816-RELEASE` 已重建 market/ui；真实网关 smoke：健康度端点 401 ms 返回 `VALID`，UI `/cynexus-screener` 返回 200。

## 2026-08-16 Phase 20 started

- 已将单标的回测冻结为“仍按受治理策略规则的历史快照命中”而非脱离规则的买入持有：回测请求/配置新增可选 `stockCode` 与 `stockMarket`，两者必须同时提供且受 A 股市场/六码校验。
- 为避免只扫描策略排序前 100 名造成假阴性，Screener 领域查询新增内部精确标的条件；单标的回测在每个历史信号日用同一策略条件直接评估该市场/代码，候选数限制不参与该模式。配置会随原有 JSONB 审计记录持久化，无需迁移。
- 策略回测对话框已新增可选标的代码与市场选择，结果明确显示 target 和 single-symbol 语义。已补充领域测试覆盖精确目标和不完整目标拒绝；下一步执行 focused tests、编译、UI lint/build、版本同步和 Release 验收。

## 2026-08-16 Phase 20 complete

- 验证通过：domain focused tests 共 11 个（Screener 3、策略运行 1、回测 5、健康度 2）零失败；Market application-controller reactor compile、Screener service/page ESLint、UI production build 和版本同步均通过。
- 已部署 Release `1.1.400-260816-RELEASE`（market、ui）。真实网关 smoke 使用 `TREND_MOMENTUM`、`SZ:000001` 创建异步回测，持久化配置返回正确 target；任务安全完成为 `NO_DATA`，原因是该标的在所选历史窗口没有策略命中，不是运行错误。响应明确携带单标的审计告警；Release UI `/cynexus-screener` 为 200，market/ui 均运行。

## 2026-08-16 Phase 21 started

- 已开始补充健康度的近期命中与表现监控：健康度读取同一 owner/tenant 的最近成功全市场回测，限定最近 90 日信号，计算成交数、胜率和平均超额收益；单标的回测不会被当作策略总体健康度证据。
- 已加入领域契约、owner-scoped 最新回测查询和 health check 的监控阈值；健康度领域 focused test 通过。尚未完成 infra/application/UI 编译、Release 部署和真实网关验收，Phase 21 保持进行中。

## 2026-08-16 Phase 21 complete

- 已完成 infra/application/UI 接入并重新发布 Market/UI Release。健康度端点真实返回 `VALID`，并含 `snapshot`、`coverage`、`signal_density`、`freshness`、`recent_performance` 五项检查；最近回测监控返回 1 笔交易、9.6304% 平均超额收益。策略卡片展示近期胜率、近期超额和回测截至日期。
- 修复了验收中发现的回测表无 `deleted_at` 字段问题；最新回测查询现在仅检查策略定义/版本的软删除状态。

## 2026-08-16 Phase 22 complete

- 已交付受治理 AI 选股：稳定 capability `stock_selection_ai_screening` 与同名 Prompt、Usage binding、AI Provider UI 元数据、结构化输出校验、可信 Screener 日线候选快照、Provider 回退和 `ai_execution_log` 审计保持一致。AI 只能解释候选快照，候选代码必须属于该快照，且不输出交易决定。
- Release 初次验收暴露默认 `custom` provider 无法调用（`Failed to call AI provider: custom`）。已将幂等 Prompt/binding migration 的基线路由改为已注册、可用的 `xhhai` OpenAI-compatible provider，并以 `-Services ai` 重建 Release；没有修改或暴露任何 Provider 密钥。
- 真实网关调用 `POST /api/ai/stock-selection/screening` 成功（`code=0`）：2026-08-14 快照、3 个候选、`openai_compatible / gpt-5.4-mini / v1`。返回代码 `603986,600487,300308` 与同条件可信 Screener 返回完全一致；最新执行日志为 `SUCCEEDED`（6489 input / 3214 output tokens，46608 ms）。
- Release 验收：AI、Market、UI 容器均处于 Up；`/cynexus-screener` HTTP 200；版本同步检查通过。下一阶段开始处理 Agent 可安全暴露的只读查询、数据质量与回填/监控、端到端验收和产品运行文档。

## 2026-08-16 Phase 23 Agent read slice

- 已完成 Agent 暴露边界审计：既有 `market.stock_screener` 已是全市场、参数有界的公开日频只读工具；策略运行、回测和观察池属于 owner/tenant-scoped 状态，Agent 不可发起任何写操作，也不能借无签名 Feign 读取这些数据。
- 新增 `market.stock_selection_strategy_health` 只读工具：仅允许三个已发布策略代码与一个可选交易日，返回 Valid/Weakening/Invalid/Not assessed、覆盖率、数据新鲜度、信号密度、近期表现、checks 和 warnings。该工具从 `AgentExecutionContext` 取得 owner/tenant，以 Release 环境的 trusted identity secret 重新签名后调用已存在的 Market health API；缺少 context 或 secret 时安全失败，调用参数不能覆盖 scope。
- 已添加严格输入/输出 schema、模型使用说明、handler contract 覆盖和缺失 scope 测试。Agent 运行时已在 Release 重新构建；治理表确认该 capability 为 `AVAILABLE`、`TENANT`、`is_read_only=true`、`requires_confirmation=false`，且版本为 1.0.0。

## 2026-08-16 Phase 23 数据质量 Release 验收

- 完成 Screener 数据质量报告：domain policy 计算 `READY / PARTIAL / STALE`，API/Agent schema/UI 同步返回快照日期、日线覆盖、字段覆盖与 warning。
- 已通过：Market domain focused tests 9 项、Market controller reactor compile、AI infra compile、Screener scoped ESLint、UI production build、版本同步检查。
- 已执行 Release：`deploy/release/start-release.ps1 -Build -Version 1.1.400-260816-RELEASE -Services ai,market,ui`；AI、Market、UI 均已重建，Gateway health 为 `UP`，Screener 页面 HTTP 200。
- 真实网关验收：`GET /api/market/screener?...pageSize=1` 返回 `READY`，快照 `2026-08-14`，最新交易日滞后 1 天，daily snapshot `5514 / 5530`（99.71%）；PE `4079 / 5530`（73.76%）仅作为部分覆盖 warning 返回。首次服务重建后在启动期返回 503，服务注册完成后请求成功；无功能错误。
- 下一步：在 Data Executor 落地日频 Screener snapshot 覆盖/回填与监控，闭合当前“实时拼接读取”到“可持续数据质量”的运营缺口。

## 2026-08-16 Phase 23 日频事实回填与监控

- 已实现并注册 `materialize_stock_screening_facts`：ClickHouse `daily_raw` → PostgreSQL `a_stock_daily_screening_fact`，支持指定交易日/区间、原子替换、checkpoint watermark、批量写入和覆盖失败语义；`quality_check` 增加 `screening_fact_coverage`。
- 新增市场 migration 并登记 Release 清单：交易日 17:55 调度 `stock_screening_fact_materialization`，运行参数固定为 `daily_raw`/2000 行批次，覆盖不一致会写失败状态。
- 验证通过：相关 pytest 20 项、Python compileall、job registry/jobs.yaml 加载、dry-run。首次真实调用发现并修复 ClickHouse `lagInFrame` Nullable 类型问题及六位非 A 股代码与无 market 自然键的碰撞问题。
- 已重建 Data Executor，并以内部 trigger 对 2026-08-14 执行真实物化：5,541 读 / 5,541 写，PostgreSQL 覆盖 100%。随后真实 `quality_check` 返回 5,541 / 5,541、100%。Data Executor healthy；market migration 已 Release 应用，调度记录已落库。
- 尚待：完成产品/运行文档和全链路最终验收；当前 Screener 线上读取仍是实时多源拼接，尚未切换为该事实表，避免未经等价验证的读路径替换。

## 2026-08-16 Phase 23 complete

- 已新增运行文档 `docs/architecture/stock-selection-operations.md`，明确服务路径、事实投影、历史回填、质量口径、告警语义和故障处置；未将日频事实投影伪称为已切换的服务读路径。
- 最终运行验收：Screener `READY`、日线 `5514 / 5530`、PE `73.76%`；Release UI `/cynexus-screener` HTTP 200；调度任务已启用且绑定 `materialize_stock_screening_facts` / 17:55；PostgreSQL 事实表为 5,541 行；Data Executor healthy；Maven/UI 版本同步通过。
- Phase 23 complete。企业级 MVP 已具备受治理指标/策略/AI 选股、观察池、回测和健康度、受限 Agent 读、运行质量报告、可持续日频事实回填与运行文档。后续若要把事实表改为 Screener 的 serving source，必须先做逐字段等价/历史回放验证、补齐行业/资金/财务多源 as-of 对齐，再独立立项切换。
# 2026-08-16 Phase 24 resumed

- Reconfirmed the AI input defect before implementation: `researchIntent` is only contextual information for the model after the trusted Screener candidate snapshot has already been created. It cannot constrain the candidate set, so the reported "持续缩量然后三倍爆量大阳线" mismatch is expected.
- The implementation target remains the approved vertical slice: a bounded, testable historical-pattern rule in the Market Screener first; then AI intent parsing into an editable plan that applies only supported rule fields.
- Inspected the Screener domain, application, API, front-end, Agent tool, and daily-fact projection boundaries. The historical fact table has the required OHLCV/previous-close fields but needs a bounded historical materialization before a current-day pattern match can be served reliably.
- Inspection typo: one `rg` command used an unbalanced regular expression while locating the Feign declaration. No files were changed; subsequent inspection will use simple literal searches.
- First Market patch did not apply because the repository lookup is chained directly in `StockScreenerQueryDomainServiceImpl`, rather than assigned to the inspected local variable. No source file was modified by the rejected patch; the next patch uses the exact call site.
- Implemented the first deterministic vertical slice across the bounded Screener contract: typed contraction / breakout-volume / bullish-body criteria, domain validation and hard matching, recent daily-fact metrics, API and Feign propagation, AI candidate forwarding, Agent schema/version update, and an editable Screener filter group.
- `mvn -pl cynexus-market/cynexus-market-domain -am -DskipTests compile` completed successfully. The controller and AI-infra reactor compiles reached their final module compilation without a reported error; their concise final status will be confirmed with focused tests before proceeding.
- Confirmed the immediate production-data constraint: `a_stock_daily_screening_fact` currently contains only the 2026-08-14 snapshot (5,541 rows), so an enabled multi-session pattern would correctly reject candidates until the durable projection is backfilled. A direct ClickHouse coverage probe first used the wrong default database (`daily_raw` was not found); inspect the configured database before executing the bounded backfill.
- Verified ClickHouse uses database `cynexus` and has 4,037 daily sessions (2010-01-04 through 2026-08-14), so a bounded historical projection backfill is available. A later version-inspection `rg` pattern was malformed; no source changed and the version will be checked through a literal/structured command before Release.
- Release script first reached its idempotent migration stage but the non-interactive command was cut before image builds. A hidden background-process fallback was rejected by the terminal policy before it started; retry the same approved Release command through a PTY/session so it can be monitored rather than killed by the short pipe timeout.

## 2026-08-16 Phase 24 complete

- Rebuilt and recreated Release `market`, `ai`, and `ui` with `1.1.400-260816-RELEASE`; all three containers are running. The AI and Market actuator health endpoints report `UP`, and `/cynexus-screener` returns HTTP 200.
- Ran the Data Executor's controlled 30-session historical materialization through its internal trigger API for `2026-07-06` through `2026-08-14`. Durable run `job_run_3c0dccb572c9443aae647ac7b87ec76b` succeeded with `165854` rows read/written and no warnings or errors; the fact table now has 30 trading days of coverage.
- Runtime Screener acceptance passed: `GET /api/market/screener?volumeContractionDays=3&minBreakoutVolumeMultiple=3&minBullishBodyPct=5&pageSize=10` returned one real match, `SZ:301365 矩阵股份`, including derived `priceVolumePattern` evidence of `3 / 3.64879641 / 12.77980200`.
- Runtime intent-plan acceptance passed: the phrase `持续缩量然后三倍爆量大阳线` resolves to `volumeContractionDays=3`, `minBreakoutVolumeMultiple=3`, and `minBullishBodyPct=5`; no unsupported phrase remains.
- Runtime AI acceptance passed and was audited: the trusted snapshot was hard-filtered to the same single stock before the model call, and the latest `stock_selection_ai_screening` execution completed successfully using `openai_compatible / gpt-5.4-mini / v1` (16,956 ms). Its structured result names only `301365 矩阵股份` and reports the matching volume/body evidence.
- Verification passed: Screener scope ESLint, `pnpm exec tsc -b --pretty false`, production Vite build, the focused Market/AI Maven tests and reactor compiles from the implementation phase, release health checks, version sync (`1.1.400-SNAPSHOT`), and live gateway smoke tests. Vite retains pre-existing font-resolution and large-chunk warnings only.

## 2026-08-16 Phase 25 started

- Captured the expanded enterprise workflow requirement. The next implementation will replace fixed phrase defaults with an AI-generated, bounded executable indicator plan plus explicit assumptions, then extend the existing strategy and watchlist contracts without introducing a second ungoverned workflow.

## 2026-08-16 Phase 25 AI intent-planning slice

- Replaced the fixed phrase parser with a separate managed AI capability: `stock_selection_ai_intent_planning`. The planner receives only the typed Screener allow-list and the user's intent, produces a strict `screening_plan`, and the domain policy rejects unsupported criteria before anything reaches the Screener.
- A plan now carries editable criteria, applied rules, explicit operational assumptions, unsupported phrases, warnings, and the actual provider/model/prompt-version route. The existing AI review runs the planner before it creates the trusted candidate snapshot, so direct execution cannot silently reuse the old fixed 3-day/3x defaults.
- Added the idempotent DB-managed prompt and usage binding migration, Release migration registration, AI Provider capability metadata, shared route selector alignment, and plan-panel assumption/route display. Applying remains an explicit user action; the model cannot issue raw SQL or execute the screen itself.
- Verification passed: `StockSelectionAiDomainServiceTest` (3 tests), AI application-controller compilation (compiled `StockSelectionAiAppService.class` after the change), Screener scoped ESLint, TypeScript build, and Vite production build. The Vite font-resolution and chunk-size warnings are pre-existing/non-blocking.

## 2026-08-16 Phase 25 target-list backtest slice

- Extended the governed backtest request/config/API/UI with an optional target list. The dialog accepts `SZ:000001`, `SH:600519` style whitespace/comma-separated symbols and preserves the existing full-market and single-symbol flows.
- Domain tests now cover exact multi-target replay and duplicate collapse. The focused `StockSelectionStrategyBacktestDomainServiceTest` passed 6 tests; Market application-controller reactor compilation passed.

## 2026-08-16 Phase 25 user-strategy slice started

- Resumed the active plan and began the owner-scoped, immutable user-strategy implementation. The deliberate Agent exposure decision is recorded: this is user-initiated mutation only, not an autonomous Agent tool.
- Implemented owner-scoped user strategies: a server-side allow-list validates the exact Screener criterion vocabulary; a definition keeps an optimistic version and every save appends an immutable active strategy version. Runs, backtests, and health assessments now resolve the catalog through the caller scope so private strategies neither leak nor fail historical replay.
- Added the Indicator save/update dialog and Strategy-tab edit path. The first Release build caught a notification type mismatch; the UI notification calls now use the shared object contract and the release is being retried.
- Verification complete: domain focused tests passed (13); Market controller and infra reactors compiled; scoped UI lint, TypeScript build, and Vite production build passed. Release `market,ui` rebuilt successfully, the screener page returned 200, the scoped strategy catalog returned its preset entries with `definitionVersion`, and PostgreSQL confirmed the owner/version migration columns. User strategy persistence is deliberately not populated with synthetic workspace data during smoke verification.

## 2026-08-16 Phase 25 named-watchlist slice started

- With versioned user strategies complete, the next dependent aggregate is named watchlists and post-entry performance observation. The implementation will preserve old flat-list data through a default parent list migration rather than discarding existing user watch records.
- Added the schema foundation only: owner-scoped named parent lists, legacy `My watchlist` backfill, per-list active uniqueness, and reserved trusted entry-context fields. It is intentionally not Release-deployed yet because the domain/API/UI write path has not been completed; partial schema deployment would not make the feature usable.

## 2026-08-16 Phase 25 named-watchlist slice resumed

- Restored the active plan and verified the repository is intentionally dirty with unrelated concurrent work; changes remain scoped to the existing stock-selection files. CodeGraph/FastCtx are unavailable in this environment, so the resumed impact inspection uses targeted `rg` searches.
- The incomplete slice currently has the parent-list domain/repository contract, trusted candidate entry projection, and migration in place. Next is to complete the focused domain test compatibility, then map the named-list fields in the UI before implementing bounded return observation.
- Updated the watchlist domain test double for the parent-list contract and trusted candidate entry context. `StockSelectionWatchlistDomainServiceTest` now passes all 4 tests. The Maven process exceeded the first terminal collection window but its Surefire report confirms success.
- Confirmed the migration scopes active uniqueness to `(watchlist_id, stock_code)` and backfills the legacy flat list. Candidate entry context is projected from a strategy-candidate/run snapshot; performance must use a separate bounded daily-bar read, rather than trusting a client-submitted or current entry price.
- Inspection note: a broad `rg` expression for snapshot fields was malformed. No files changed by that search; subsequent searches will use fixed-string/individual terms.
- Implemented the bounded watchlist observation contract: frozen entry price/date/market, batch latest stock close, `SH:000300` aligned benchmark return, per-item availability warnings, source metadata, and a 100-item cap. The first focused test build exposed that the existing backtest test fake must implement the newly extended market-data port; it has been updated before rerunning verification.
- Verification so far: 12 focused Market domain tests passed (watchlist lifecycle, watchlist observation, and backtest regression); Market application-controller reactor compilation passed. Existing unrelated Maven model/MapStruct warnings remain, with no new compilation error.
- UI now accepts/creates named lists, groups existing records by list in the Watchlist panel, surfaces the frozen entry date/price, and independently renders return, benchmark excess, latest close, data source, and availability warnings. Remaining checks are infra compilation, TypeScript/Vite build, Release migration/runtime smoke, and a final Agent-exposure exclusion note for the owner-scoped read.
- Infra reactor compilation passed. Frontend checks passed: scoped ESLint, TypeScript project build, and Vite production build. Vite reports only the established unresolved local Montserrat assets and large-chunk advisory warnings.
- The Release deployment target remains the standard `develop` UI workflow paired with the shared Release backend; because this slice changes Market API/schema, it requires the versioned Release `market,ui` rebuild rather than a Vite-only handoff. The owner-scoped observation Agent-exposure exclusion is documented in findings.

## 2026-08-16 Phase 25 complete

- Released `market,ui` as `1.1.400-260816-RELEASE`. The named-watchlist migration applied, including legacy backfill and active per-list uniqueness; both containers recreated successfully.
- Runtime smoke passed: `/cynexus-screener` HTTP 200, Market actuator health `UP`, and `GET /api/market/screener/watchlist/performance` returned the bounded owner-scoped contract (`items`, `dataSource=clickhouse.daily_raw`, `warnings`) successfully. The current smoke identity has no watchlist items, so it correctly returns an empty list rather than fabricated observations.
- Final verification: focused domain tests (12) passed; Market application and infra reactor compiles passed; scoped ESLint, TypeScript build, and Vite build passed; full Release build/recreate passed; Maven/UI versions remain `1.1.400-SNAPSHOT`; scoped `git diff --check` reported no whitespace error. Vite retains only established font-resolution and chunk-size warnings.

## 2026-08-16 Phase 26 complete

- Added the owner-scoped watchlist performance-history API and domain calculation. It uses the persisted candidate entry context as the return baseline, aligns `SH:000300` over the available date range, bounds the response to 30–252 daily points, and preserves source/warning semantics.
- Added the Watchlist `Observe` dialog with loading, error, empty and success states, plus a comparative return chart and entry evidence. The existing Trade Review action remains the stock-review entry point.
- Verification passed: focused Market domain tests (13), Market application/infra reactor compiles, scoped ESLint, TypeScript build and Vite production build. Release `market,ui` was rebuilt as `1.1.400-260816-RELEASE`; `/cynexus-screener` returned HTTP 200, Market health was `UP`, and the overview endpoint returned the bounded owner-scoped contract. The smoke identity currently has no watchlist items, so no synthetic item was created solely to exercise a non-empty history path.
- Maven/UI versions remain synchronized at `1.1.400-SNAPSHOT`; `git diff --check` found no whitespace error. The established Vite local-font and chunk-size warnings remain non-blocking.
