# 进度记录

## 2026-08-30

- 已读取 planning-with-files 规范。
- 已盘点个人持仓相关领域模型、应用服务、DTO、持久化交易字段和 React 页面。
- 已确认当前任务先输出详细 plan，不执行产品代码改动。
- 已新增 `PortfolioTradeRoundBO`、`PortfolioTradeAnalyticsBO` 和纯 FIFO `PortfolioTradeAnalyticsDomainService`。
- 已新增 `/api/market/portfolio/analytics`，支持日期、标的、股票/ETF、WIN/LOSS/BREAKEVEN 筛选，并返回轮次和数据质量警告。
- 已在个人持仓页增加“交易分析”入口和总览/轮次表，包含加载、错误、空数据和数据质量状态。
- 验证通过：后端 controller 模块 `mvn -DskipTests compile`；领域测试 2 个通过；UI `pnpm build` 通过。
- 未执行 Release 部署：当前变更尚未完成全量第一阶段筛选、组合风险和回撤计算，待确认后继续扩展。
- 已补齐交易级累计收益曲线和最大回撤，并增加 API/UI 的标的、股票/ETF、WIN/LOSS/BREAKEVEN 筛选。
- 已增加 short/medium/long 持仓周期筛选、累计收益/回撤条形可视化和最大回撤摘要。
- 已补充当前持仓最大权重与组合集中度（HHI）摘要，并联动 overview 数据。
- 已修复分析筛选 effect 的 lint 问题；定向 eslint 通过。
- 已为成交记录增加 strategy/tags 元数据透传和迁移；新增账户现金流/账户快照/行业归属表结构迁移，为真实收益和行业集中度提供持久化基础。
- 后端 controller 编译通过，UI build 通过；最大亏损无亏损样本时返回 0。
- 已新增现金流、账户快照领域对象、仓储适配器及 `/cash-flows`、`/account-snapshots` API；分析响应增加时间加权收益、资金加权收益和账户级回撤字段。
- 已新增 `/industry-concentration` API，按当前持仓行业归集并提供 UNKNOWN 桶；交易 DTO/PO/查询映射已透传行业字段。
- 验证通过：`mvn -pl cynexus-market/cynexus-market-application/cynexus-market-application-controller -am -DskipTests compile`、`pnpm build`。
