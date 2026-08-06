# Task Plan: Market Indicator Redesign

## Goal
重构 Market Indicator 为面向盘中研判的可扫描工作台，改善信息层级、数据表达、筛选交互、状态覆盖和响应式体验，并完成构建与 Release 验证。

## Current Phase
Phase 5

## Phases

### Phase 1: Requirements & Discovery
- [x] 读取项目规范、产品与设计系统
- [x] 审查现有 Market Indicator 页面、服务与后端数据契约
- [x] 记录用户工作流、数据缺口和重构方向
- **Status:** complete

### Phase 2: Planning & Structure
- [x] 确定页面信息架构与数据表达方式
- [x] 确定复用组件与局部组件边界
- [x] 记录设计决策
- **Status:** complete

### Phase 3: Implementation
- [x] 重排页面主结构和头部状态区
- [x] 改造市场状态、涨跌停、行业、事件和明细渲染
- [x] 补齐筛选、排序、刷新、loading/error/empty 与响应式状态
- **Status:** complete

### Phase 4: Testing & Verification
- [x] UI TypeScript/build 校验
- [x] Maven compile 校验
- [x] Release 镜像栈重建和页面验证
- [x] 检查版本同步、git status 与临时文件
- **Status:** complete

### Phase 5: Delivery
- [x] 汇总变更与验证结果
- [x] 交付文件路径和剩余风险
- **Status:** complete

## Decisions Made
| Decision | Rationale |
|----------|-----------|
| 采用“市场状态 -> 异动板块/涨跌停 -> 个股监控 -> 事件”顺序 | 符合盘中用户先判断环境、再缩小范围、最后确认标的的决策路径 |
| 复用当前中性工作台设计系统与 A 股红涨绿跌语义 | 保持与项目其他市场页面一致，避免新页面视觉孤岛 |
| 前端只做展示层排序/筛选，不改变后端指标计算 | 当前数据契约已提供快照、广度、行业和事件，避免把业务规则移入 UI |

## Errors Encountered
| Error | Resolution |
|-------|------------|
| Impeccable skill 的 context 脚本路径最初使用了错误位置 | 改用 `C:/Users/Cs7er/.codex/skills/impeccable/scripts/context.mjs` |
| planning 初始化脚本在仓库根生成了被忽略的旧版计划文件 | 已使用本目录维护正式计划；收尾时清理根目录生成物 |
