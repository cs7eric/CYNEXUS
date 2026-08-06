# 期指结构 AI 分析

目标：沿用项目既有 AI 能力链路，为 IF、IH、IM、IC 提供可解释的多空持仓结构分析，并在期货持仓页面展示。

## 阶段

- [completed] 1. 研究已有 AI capability、provider、usage binding、prompt 与前端调用案例
- [completed] 2. 设计并实现期货专用 domain/API/infra 链路与 prompt seed
- [completed] 3. 接入 IF/IH/IM/IC 页面交互与展示
- [completed] 4. 同步版本、编译测试、Release 部署验证

## 约束

- 只对 IF、IH、IM、IC 开放分析入口。
- 不新增第二套 prompt 存储；使用 `ai_prompt` 与 `ai_usage_bindings`。
- 遵守 application/domain/infra 分层，分析编排与业务含义不能堆在 Controller 或 AppService。

## 错误记录

| 错误 | 尝试 | 处理 |
|---|---:|---|
