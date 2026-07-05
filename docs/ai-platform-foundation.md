# Cynexus AI 基础能力接入方案

## 背景

本次接入参考 `E:/project-repo/jc-ai` 的组织方式，但不照搬它的示例项目结构。Cynexus 已经有 `cynexus-ai` 业务域、`ai_prompt` 管理表、provider/model/binding 基础链路，所以 AI 基础能力必须落在现有分层内，避免把 demo 代码、第三方框架对象和业务 API 混在一起。

本次只完成依赖与配置底座，不新增真实业务接口，不替换现有 `mock` adapter，不强制启用任何外部模型。

## 分层落点

| 层 | 模块 | 职责 |
| --- | --- | --- |
| API | `cynexus-ai-api` | 只放对外 DTO / request，不直接依赖 Spring AI、LangChain4j 或 provider SDK |
| Common | `cynexus-ai-common` | AI 域内常量、轻量工具，不承载模型调用实现 |
| Domain | `cynexus-ai-domain` | provider/model/prompt/tool/routing 的领域对象和领域服务，不引用第三方模型客户端 |
| Infra | `cynexus-ai-infra` | Spring AI、DashScope、DeepSeek、LangChain4j、PGVector、文档解析、对象存储、重试/熔断等适配实现 |
| Application | `cynexus-ai-application-controller` | 编排服务和 Web API，SSE/流式接口在这一层暴露 |
| Starter | `cynexus-ai-starter` | 运行时装配、环境变量配置、Nacos 配置入口 |

## 已接入依赖

### Spring AI 基础

落点：`cynexus-ai-infra`

- `spring-ai-starter-model-openai`
- `spring-ai-starter-model-deepseek`
- `spring-ai-rag`
- `spring-ai-starter-vector-store-pgvector`
- `spring-ai-starter-vector-store-redis`
- `spring-ai-pdf-document-reader`
- `spring-ai-tika-document-reader`
- `spring-ai-markdown-document-reader`

覆盖能力：

- `ChatClient`
- Prompt 模板
- Memory / Advisor 基础
- SSE / 流式输出所需模型能力
- Function Calling / Tool Calling 基础
- 结构化输出前置依赖
- RAG、文档读取、PGVector / Redis Vector Store

说明：provider starter 目前以 `optional=true` 放在 infra，避免只加依赖就触发自动配置并要求本地必须有 API key。后续真实 adapter 开发时，再按 provider 选择性放开到运行时。

### Spring AI Alibaba / DashScope

落点：`cynexus-ai-infra`

- `spring-ai-alibaba-starter-dashscope`
- `dashscope-sdk-java`

覆盖能力：

- 通义千问 / DashScope 对话
- DashScope Embedding
- 图像生成
- 语音 ASR / TTS

说明：DashScope 相关 starter 同样暂时 optional，真实启用前需要先完成 provider adapter 与 `ai_provider` / `ai_model_config` 的映射。

### LangChain4j 基础

落点：`cynexus-ai-infra`

- `langchain4j`
- `langchain4j-open-ai`

覆盖能力：

- AI Service
- Tool Calling
- Memory
- 动态 Agent
- 租户 Prompt
- 结构化输出

说明：这里没有照搬 `jc-ai` 的独立 `tenant_prompt` 表。Cynexus 统一用 `cynexus_appl_ai.ai_prompt` 管理 prompt，租户/场景/版本能力后续通过 `extra`、`tags`、`scenario_code` 或扩展字段承载。

### Prompt 工程与评估

落点：`cynexus-ai-infra`

- `jsonschema-generator`
- `jsonschema-module-jackson`
- `spring-retry`
- `spring-aspects`
- `resilience4j-spring-boot3`
- `httpclient5`

覆盖能力：

- Prompt 版本管理和回滚的服务实现基础
- A/B 测试与离线评估的执行基础
- 安全边界、输入清洗、输出校验
- 结构化输出 JSON Schema
- 模型调用重试、超时、熔断

### 企业知识库 RAG

落点：`cynexus-ai-infra`

- `minio`
- `pdfbox`
- `poi-ooxml`
- `flexmark-all`
- `jtokkit`
- Spring AI 文档读取与 vector store 依赖

覆盖能力：

- 文件存储
- PDF / Word / Markdown / TXT 文档解析
- 文档分块
- Token 预算控制
- Embedding
- PGVector 召回
- 问答、评估、反馈的数据链路准备

## 运行配置

运行配置只放环境变量名，不在配置文件中写明文 key：

```yaml
cynexus:
  ai:
    platform:
      enabled: false
      default-provider: mock
      providers:
        openai:
          api-key-env: CYNEXUS_AI_OPENAI_API_KEY
        deepseek:
          api-key-env: CYNEXUS_AI_DEEPSEEK_API_KEY
        dashscope:
          api-key-env: CYNEXUS_AI_DASHSCOPE_API_KEY
```

后续 provider adapter 读取顺序应为：

1. `ai_provider` / `ai_model_config` 中的 provider/model 能力配置。
2. `SecretResolver` 从环境变量或密钥系统解析 API key。
3. `ModelRoutingDomainService` 根据 scenario/provider/model 选择运行时。
4. Infra adapter 组装 Spring AI 或 LangChain4j 客户端。

## 后续落地顺序

1. Provider adapter：补 `OpenAiCompatibleAdapter`、`DeepSeekAdapter`、`DashScopeAdapter`，替换目前只有 `mock` 的运行态。
2. Prompt runtime：把 `ai_prompt` 的 `system_prompt/user_prompt_template/output_schema/variables` 接入执行链路。
3. SSE：在 application-controller 增加流式执行 API，返回 `Flux` 或 MVC SSE。
4. Tool registry：把现有 `AiToolRegistry` 拓展为 Spring AI ToolCallback / LangChain4j Tool 双适配。
5. RAG schema：在 `cynexus_appl_ai` 下新增知识库、文档、chunk、会话、反馈、评估表。
6. RAG runtime：实现上传、解析、分块、向量化、召回、rerank、回答、引用来源。
7. PromptOps：在现有 prompt CRUD 基础上补版本发布、回滚、A/B 测试、评估报告。
8. 多 Agent：在 application 层定义业务 agent 编排，infra 层分别适配 Spring AI / LangChain4j / AgentScope。

## 约束

- 不在 `lib/api.ts` 或前端杂项文件里堆 AI API，前端继续按业务域放到 `src/services/ai`。
- 不新增独立 prompt 表来分裂现有 `ai_prompt`，除非未来出现强业务理由。
- 不在配置文件提交真实 API key。
- 不让第三方框架类型穿透到 domain/api 层。
- Provider starter 默认不强制运行，避免本地没有 key 时破坏现有服务启动。
