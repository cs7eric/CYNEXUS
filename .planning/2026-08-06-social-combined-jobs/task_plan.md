# 社媒帖子+评论 Job 全链路

## 目标

为小红书和抖音新增独立 Job 分类与任务配置：一次任务完成帖子发现和评论采集，保留每次运行的帖子/评论数量与耗时，接通 Python Executor、Java Job 管理/API、前端任务配置和 Release 验证。

## 阶段

- [completed] 1. 梳理现有 Job 分类、调度配置、社媒采集链路和运行记录
- [in_progress] 2. 实现帖子+评论合并 Job、耗时指标与测试
- [pending] 2. 实现帖子+评论合并 Job、耗时指标与测试
- [pending] 3. 注册小红书/抖音 Job 分类及 Java 调度/配置链路
- [pending] 4. 接通前端 Job 配置与运行结果展示
- [pending] 5. 构建、部署 Release、联调和回归验证

## 错误记录

| 错误 | 尝试 | 处理 |
| --- | --- | --- |
| planning-with-files 初始路径不存在 | 1 | 使用可用的 `C:\\Users\\Cs7er\\.agents\\skills\\planning-with-files` 说明继续 |
| pytest 无法导入 `cynexus_data` | 1 | 在 `cynexus-data` 工作目录未设置 `PYTHONPATH=src`；下一次带环境变量运行 |
