# Market Overview 看板

## Goal

重构 `/market/overview` 为 Market 研究总览入口，聚合 Market 下 Pulse、Sentiment、Margin、ETF、Futures、Indicator 六个模块的关键快照，帮助用户先判断环境再进入细分分析。

## Phases

- [complete] Phase 1: 盘点现有路由、服务与设计约束
- [complete] Phase 2: 重构总览信息架构与聚合数据加载
- [complete] Phase 3: 运行 lint/build 并完成 Release UI 重建

## Decisions

- 使用现有 `WorkspaceShell` 与语义 token。
- Overview 是导航层，不复制子页面详细表格；首屏呈现市场环境判断、六模块状态和跨模块变化。
- 失败时每个模块独立降级，并保留进入对应深度页面的链接。
- 新路由命名为 `/market/overview`，侧边栏标签为 `market overview`。

## Errors Encountered

| Error | Attempt | Resolution |
|---|---:|---|
