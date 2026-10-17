# Progress

- 2026-08-31：完成 AI 模块、Provider 路由、Prompt、MP4 和 Agent 入口盘点。
- 2026-08-31：完成 Spring AI、LangChain4j、AgentScope 与现有实现的职责对照。
- 2026-08-31：形成分层迁移建议，当前未修改业务代码。
- 2026-08-31：将两份架构方案合并为 `docs/architecture/ai-refactor-architecture.md`，并扩展后续实施阶段。
- 2026-08-31：补充现状代码映射、能力契约、MP4 时序、职责矩阵、迁移交付物、配置/安全/观测/验收规则和 Anti-drift checklist。
- 2026-08-31：新增 `AiPromptRuntimeResolver` 领域契约及基础设施实现，MP4 AI Domain Service 改为通过 Resolver 获取 Prompt 并渲染变量；API 和数据库契约未改变。
- 2026-08-31：`mvn -pl cynexus-ai/cynexus-ai-infra -am -DskipTests compile` 通过。
- 2026-08-31：新增 `AiModelExecutor` 领域契约及兼容实现，MP4 Domain Service 改为依赖统一模型执行边界；旧 Completion Gateway 保留为 delegate。
- 2026-08-31：`mvn -pl cynexus-ai/cynexus-ai-infra,cynexus-ai/cynexus-ai-application/cynexus-ai-application-controller -am -DskipTests compile` 通过。
- 2026-09-03：完成 Phase 3 类型化 LangChain4j AI Service 工厂及 Spring AI ChatModel 桥接，新增媒体、知识库、市场事件、因子研究和期货分析服务契约。
- 2026-09-03：完成 Phase 4 文档切分、Embedding Port、权限感知 VectorStore 检索和索引编排服务；新增切分与租户/所有者元数据测试。
- 2026-09-03：`mvn -pl cynexus-ai/cynexus-ai-infra -am -DskipTests compile` 与 Phase 3/4 聚焦测试通过。
- 2026-09-03：复核发现 Phase 1-4 仍有未完成项；补充 LangChain4j 按 capability 选择 Typed Service、RAG 缺失身份拒绝、文档重建前删除旧向量，并将计划状态修正为进行中。
- 2026-09-03：新增 `/api/ai/knowledge-base/search` 权限感知语义检索 API；补充 MP4 bounded repair、结构化输出清洗、LangChain4j 回退日志、模型密钥轮换缓存失效和相关测试。
- 2026-09-03：新增可选 `cynexus.ai.rag.enabled` 配置，提供 Spring AI OpenAI Embedding + PgVector 自动装配（默认关闭），并抽取 OpenAI client 构建支持；AI infra 编译通过。

- 2026-09-04: Added DB-managed knowledge_base_rag_answer prompt/binding migration, UI capability metadata, RAG answer contract tests, context-aware Langfuse reporting for Spring AI, and PGVector extension/vector_store migration.

- 2026-09-04: Added VisionOutputDomainPolicy for receipt, trade screenshot, review and WeChat route semantic validation; Scenario vision finalization now enforces capability-specific rules.

- 2026-09-04: Release migration initially exposed missing pgvector type in local image; migration now degrades explicitly to compatibility JSON table when extension binary is unavailable. Fixed RAG optional Bean wiring so disabled RAG does not prevent AI startup. Release AI image 1.1.400-260904-RELEASE rebuilt and health is UP.

- 2026-09-04: Migrated MarketIndicatorReplayAiDomainService production wiring to AiPromptRuntimeResolver with backward-compatible constructor fallback.

- 2026-09-04: FuturesPosition and MarketEventFactorAnalysis production wiring now accepts AiPromptRuntimeResolver; legacy constructors remain for compatibility.
- 2026-09-04: Stock Selection and Knowledge Base document generation production wiring now uses AiPromptRuntimeResolver; legacy constructors remain for compatibility. Prompt rendering rejects unresolved placeholders.
- 2026-09-04: Added unified vision execution-context overloads and propagated generated execution context through AgentExecutor/LangChain4j vision paths.
- 2026-09-04: Connected Knowledge Base document lifecycle to asynchronous AI index events (upsert/delete), added tenant/owner document metadata migration, published-only vector indexing, and published/status retrieval filtering.
- 2026-09-04: RAG answer prompt now resolves through DB Prompt Runtime Resolver; release Rabbit topology and migration registration updated.
- 2026-09-05: Added framework-neutral BasicAiStructuredOutputParser and migrated remaining Stock Selection, Market Information, Market Indicator Replay, Futures Position, Factor Study and Market Event JSON extraction points to the shared parser boundary.
- 2026-09-05: Replaced remaining LangChain4j Map-based capability outputs with capability-specific transport records/JsonNode fields, added parser contract tests, and rebuilt AI Release 1.1.400-260905-RELEASE with readiness UP.
