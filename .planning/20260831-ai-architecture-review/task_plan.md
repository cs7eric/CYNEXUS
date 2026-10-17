# AI Architecture Review

## Goal
审查现有 AI 实现，评估与 Cynexus + Spring AI + LangChain4j + AgentScope 分层方案的差距，输出可执行重构计划。

## Phases
- [x] 盘点现有模块、调用链和配置模型
- [x] 对照目标框架职责识别边界问题
- [x] 输出分阶段迁移方案、风险和验证策略
- [x] 将架构基线写入 `docs/architecture/ai-refactor-architecture.md`
- [ ] Phase 1：统一所有能力的 Prompt/Context/Structured Output 契约（MP4 已接入，其他能力迁移中）
- [ ] Phase 2：完成 Spring AI 全部文本/视觉能力、观测和回归验证（基础执行层已接入）
- [ ] Phase 3：迁移稳定能力到真正的 capability-specific LangChain4j AI Services（基础工厂和执行桥已接入）
- [ ] Phase 4：接入 Spring AI RAG 与向量检索到真实知识库生命周期（基础适配器已接入）
- [ ] Phase 5：以独立编排器方式接入 AgentScope
- [ ] Phase 6：删除重复实现并完成 Release 验证
