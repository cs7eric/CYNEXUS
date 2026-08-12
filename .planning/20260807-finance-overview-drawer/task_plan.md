# Finance overview drawer restructure

## Goal

把 Finance 的账户与记账分类从独立页面迁移到 Overview 的导航按钮 Drawer：账户用可展开 Collapsible 展示详情、信用还款和分期；设置 Drawer 承载记账分类等配置；统一导航按钮样式，并移除独立账户入口。

## Phases

- [completed] Phase 1: 检查当前 Finance 路由、导航、账户/分类组件和服务边界
- [completed] Phase 2: 实现 Overview 的账户 Drawer、Collapsible 账户详情与信用操作
- [completed] Phase 3: 实现 Settings Drawer 并迁移记账分类管理
- [completed] Phase 4: 清理独立 Accounts 页面入口与旧布局，统一导航按钮样式
- [completed] Phase 5: 运行 UI 检查、部署 Release UI 并验证页面
- [completed] Phase 6: 拆分 Finance 顶部导航与工具操作，放宽抽屉内容间距
- [completed] Phase 7: 回滚错误的全局布局调整，仅优化账户弹出层

## Decisions

- 保留现有后端 API，不新增 Java 后端改动。
- Overview 导航中的“账户”和“设置”使用 Drawer；账户 Drawer 内使用 Collapsible，而不是继续叠加独立 Card。
- 账户 Drawer 中的信用还款和分期能力复用现有服务与组件。
- 账户 Drawer 的“还款”和“分期计划”使用独立 Dialog，账户自身只负责折叠详情，避免 Drawer 内继续堆叠大块内容。

## Errors Encountered

| Error | Attempt | Resolution |
|---|---:|---|
