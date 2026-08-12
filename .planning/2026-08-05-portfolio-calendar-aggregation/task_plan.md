# Personal Portfolio 周/月/年收益聚合修复

## 目标

修复 Personal Portfolio 日历图下周、月、年收益没有按对应周期聚合的问题，并完成前端验证。

## 阶段

- [completed] 1. 定位日历图、数据接口与周/月/年计算链路
- [completed] 2. 修复聚合逻辑并补充回归测试
- [completed] 3. 运行 UI 检查、版本与 Release 影响验证
- [completed] 4. 修复日收益视图中间持仓收益数据为空
- [completed] 5. 将周/月/年视图严格改为视图范围内交易日收益聚合
- [completed] 6. 将收益组成改为视图收益，并修复历史总资产快照
- [completed] 7. 重新定义热力图为视图区间内按持仓聚合的收益贡献
- [completed] 8. 修复首次导入基线日与 2026-08-03 日收益计算
- [completed] 9. 补充持仓贡献、基线日和跨日现金流回归测试
- [completed] 10. 构建、部署 market/ui 并核对 Release 实际数据
- [completed] 11. 修复日视图热力图误用区间累计贡献
- [completed] 12. 支持点击日历日期切换右侧单日数据
- [completed] 13. 让手动总资产基线按后续每日收益动态变化

## 错误记录

| 错误 | 尝试 | 处理 |
| --- | --- | --- |
| 回归测试首次 NPE | 新增 overview 测试后运行 Maven | 补齐测试 fixture 的 `applyRealizedPnlBaselines` 与 `valuationStatus` mock |
| UI 全量 build 被既有社媒页面 TypeScript 错误阻断 | `pnpm.cmd run build` | 已确认错误集中在既有 `social-monitoring/index.tsx`，本次未修改；Personal Portfolio 定向 ESLint 无 error |
| Release 多服务参数写法错误 | 外层 PowerShell 调用 `-Services market,ui` / 重复 `-Services` | 脚本内直接使用数组语法 `@('market','ui')` |
