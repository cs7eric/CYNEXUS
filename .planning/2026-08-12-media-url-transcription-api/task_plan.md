# URL 转写 API 计划

## 目标

提供一个只传原始 URL 的 API 调用入口，自动执行 Downloader 解析、媒体转写、AI 润色、摘要和历史入库，行为与 URL 自动化处理流程一致。

## 阶段

- [completed] 1. 确认现有 URL 自动化链路和媒体地址选择规则
- [completed] 2. 新增 URL-only API 并统一 Downloader 地址选择
- [completed] 3. 补充测试、文档和版本/工作区检查
- [completed] 4. 将 URL API 改为立即入队响应，并新增 Settings/API Workflows 执行记录页

## 错误记录

| 错误 | 尝试 | 处理 |
| --- | --- | --- |
