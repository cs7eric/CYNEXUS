# Progress

## 2026-08-05

- 已读取 `cynexus-frontend` 与 `planning-with-files` 指南。
- 已读取 `cynexus-ui` 的区域规范、设计规范与实现指南。
- 已确认工作区存在大量用户既有修改，后续只修改本任务相关文件。
- 已完成 CodeGraph 同步，开始定位 Personal Portfolio 日历收益聚合链路。
- 已确认问题根因：交易时段内 AppService 将当前单日收益覆盖到周/月/年聚合点。
- 已修复为仅在日粒度覆盖实时单日收益，周/月/年保留领域服务已计算的聚合值。
- 已新增回归测试，验证周聚合点保留 100 的周期收益，同时 `todayPnl` 独立为 5。
- 验证通过：PortfolioOverviewAppServiceTest 4/4、PortfolioPerformanceDomainServiceTest 17/17、UI build、Personal Portfolio ESLint（0 errors，1 个既有 warning）。
- 已执行 `start-release.ps1 -Services market -Build -Version 1.1.398-260805-RELEASE`，market 容器重建并启动；健康检查 200，周/月/年接口均返回 200。
- 用户反馈日收益视图中央热力图为空；已通过 Release API 确认根因是持仓 `todayPnl` 全部为 null。
- 已修复收盘后 overview 丢弃行情参数的问题，改为继续使用日线、实时和分钟行情计算各持仓 `todayPnl`。
- 回归测试通过：PortfolioOverviewAppServiceTest 5/5，新增收盘后持仓收益行情传递测试。
- Release API 验证：overview 返回 5 个持仓，`nullTodayPnl=0`；market health 200。
- Personal Portfolio 定向 ESLint 0 errors、1 个既有 warning；UI 全量 build 仍被既有 `social-monitoring/index.tsx` TypeScript 错误阻断。

## 2026-08-06

- 复核 Release API，确认周/月/年聚合目前只剩 2026-08-03 后的点；根因是 AppService 用 realized P&L 的 `latestResetAt` 裁剪了性能视图范围。
- 下一步移除该裁剪，保留基线作为领域收益校准参数，让月/年视图按请求范围内的交易日点聚合。
- 已移除 `latestResetAt` 对性能视图 `from` 的裁剪；新增领域测试验证月聚合只求请求范围内日点之和，应用测试验证复盘基线日期不再截断全年视图。
- focused Maven tests：`PortfolioPerformanceDomainServiceTest` 18/18、`PortfolioOverviewAppServiceTest` 8/8；Personal Portfolio 定向 ESLint 通过。
- 已部署后端 Release `1.1.398-260806-RELEASE` 的 market 服务；health HTTP 200。
- Release API 核对：当前月周视图日点合计与周聚合均为 `196.6`；全年月视图为 `164.5 + 196.6 = 361.1`；五年年视图的 2026 年聚合为 `361.1`。

## 2026-08-06 视图收益与总资产

- 新增 `PortfolioPerformanceBreakdown`，热力图/列表均读取当前视图的 `periodPnl`，不再读取当前持仓浮动盈亏；日收益中间区域也统一使用视图内日收益。
- 历史快照改为按日期反推现金流，保证 `totalAssets` = 当日收盘持仓市值 + 当日现金余额，并随每日 P&L 正确变化。
- UI 定向 ESLint 0 errors、TypeScript `tsc -b` 通过；领域测试 19/19 通过。
- Release 外层 PowerShell 多服务参数传递失败，已改为在当前 PowerShell 直接使用 `@('market','ui')` 数组语法。

## 2026-08-06 最终验证

- `PortfolioPerformanceDomainServiceTest` 20/20；`PortfolioOverviewAppServiceTest` 8/8。
- Personal Portfolio 定向 ESLint、`tsc -b` 和 Vite build 均通过。
- 已使用 `start-release.ps1 -Services @('market','ui') -Build -Version 1.1.398-260806-RELEASE` 重建并重启 market/ui。
- Release health 为 `UP`，UI HTTP 200；当前 2026-08-01 至 2026-08-06 的日/周收益点均按视图内点求和，日点总资产随收益变化。

## 2026-08-06 口径返修

- 已根据用户纠正重新确认产品口径：视图收益由持仓/证券贡献组成，不由日期块组成。
- 已定位 7/30 和 8/3 回归到 `dailyPoints` 使用资产差额；下一步恢复逐证券收益计算，并让总资产跟随该收益序列。
- 已扩展领域结果与 performance API，准备返回视图区间内逐证券累计收益贡献。
- 已将领域日收益改回逐证券前收/成交/收盘计算，并改为从当前总资产向前倒推每日资产路径；首次导入基线规则重新生效。
- 已将热力图改为读取 performance API 的逐证券 contributions，方块恢复为证券代码/名称/视图区间收益贡献。
- 已新增基线日不计历史浮亏、逐证券贡献求和和总资产随收益路径变化的领域回归测试，并同步应用层 todayPnlBySymbol 调用测试。
- focused Maven tests 通过：`PortfolioPerformanceDomainServiceTest` 22/22、`PortfolioOverviewAppServiceTest` 8/8。
- Personal Portfolio 定向 ESLint、`tsc -b` 与 Vite build 均通过。
- 已使用 Release `1.1.398-260806-RELEASE` 重建并重启 market/ui；market health 为 `UP`，UI HTTP 200。
- Release 实际数据确认：7/30 为 `0`，8/3 为 `12.1`；日/周/月/年逐证券贡献之和均等于视图收益，日资产相邻差额均等于对应日收益。
