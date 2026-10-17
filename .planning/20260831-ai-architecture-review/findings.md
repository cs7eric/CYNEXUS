# Findings

- 当前核心调用链为 Domain Port -> 自研 AiCompletionGateway -> 自研 Adapter -> Java HTTP Client。
- `cynexus-ai-infra/pom.xml` 已声明 Spring AI、LangChain4j 和 DashScope 依赖，但生产 Java 源码未使用对应 ChatClient、AI Services 或 AgentScope API。
- AI 配置以 `ai_providers`、`ai_prompt`、`ai_usage_bindings`、`ai_execution_log` 为中心，Binding 驱动模型和 Provider fallback。
- Agent 的 Run/Step/Tool/确认/审计/配额为自研实现；不应让多个 Agent 框架同时持有同一状态机。
- MP4 `media_transcription_enrich` 已是一次结构化调用，但旧的 polish/summary/metadata/ask 能力仍保留作为兼容入口。
- 目标分层与迁移阶段已固化到 `docs/architecture/ai-refactor-architecture.md`，后续实现必须通过其中的 Anti-Drift Checklist。
