# 修复 AI 交易记录手续费

## Goal

修复交易截图识别把成交额误当手续费的问题，并修正已落账的同类错误数据。

## Phases

- [x] 定位 AI 提取、候选标准化、确认落账和展示链路
- [x] 在领域手续费策略中识别明显错误的 AI 手续费，并补充测试
- [x] 更新 DB prompt 与 Java fallback，加入幂等数据修复迁移
- [x] 运行聚焦测试、编译、版本检查和 Release 市场服务部署验证

## Errors Encountered

| Error | Attempt | Resolution |
|---|---:|---|
| FastCtx/CodeGraph MCP tools unavailable | 1 | Used local `codegraph` CLI; repository search fallback for exact source text |
| `Get-Content cynexus-market/*/pom.xml` wildcard failed on PowerShell | 1 | Used direct known paths and `rg` |
| Release script rejected `-Version 1.1.398-SNAPSHOT` | 1 | Migrations completed before version validation; retry with `1.1.398-260807-RELEASE` |
| Release package test compilation failed in existing `RedisAgentRateLimitConcurrencyTest` | 1 | Build Release JARs with `-Dmaven.test.skip=true`; focused and full market domain tests already passed |
| PowerShell parsed `-Dmaven.test.skip=true` as a Maven lifecycle phase | 1 | Quote the property argument |
