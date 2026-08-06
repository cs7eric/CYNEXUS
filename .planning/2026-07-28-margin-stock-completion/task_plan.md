# 个股两融数据补全

## Goal

补全个股两融数据的关键字段和历史来源，避免页面出现大面积空值，并让补数任务可持续执行。

## Phases

- [completed] 确认缺失字段与数据源覆盖
- [completed] 修复 EastMoney 字段映射和日更补全
- [completed] 修复历史回填配置并补现有数据
- [completed] 测试、版本同步和 Release 验证

## Errors Encountered

| Error | Attempt | Resolution |
|---|---:|---|
| SSE/SZSE 官方明细接口字段不对称 | 1 | 使用 EastMoney 两融明细作为字段补全来源，官方交易所数据继续作为主记录来源 |
