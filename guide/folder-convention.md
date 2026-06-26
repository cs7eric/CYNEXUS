可以。你这个项目的代码 folder 规范我建议直接定成 **Maven 多模块 + DDD 分层 + 微服务 starter 独立启动**。

核心原则：

```text
api         对外契约
application 用例编排 / 入口层
domain      领域模型 / 业务规则
infra       技术实现 / DB / Feign / Spring AI / Redis / Nacos
starter     独立 Spring Boot 启动模块
common      当前服务内部通用
```

---

# 1. 根目录规范

```text
cynexus
├── pom.xml
├── README.md
├── PROJECT_BRIEF.md
├── CODEX_TASK.md
│
├── docs
│   ├── architecture
│   ├── database
│   ├── deployment
│   └── ai
│
├── deploy
│   ├── docker
│   │   ├── docker-compose.core.yml
│   │   └── docker-compose.full.yml
│   ├── nacos
│   ├── prometheus
│   ├── grafana
│   └── jenkins
│
├── sql
│   ├── cynexus_ai
│   ├── cynexus_market
│   └── common
│
├── cynexus-common
├── cynexus-ai
└── cynexus-market
```

说明：

```text
docs        项目文档
deploy      Docker / Nacos / Jenkins / Prometheus / Grafana 配置
sql         初始化 SQL / migration SQL
common      通用基础模块
ai          AI 微服务
market      行情微服务
```

---

# 2. common 模块规范

```text
cynexus-common
├── pom.xml
│
├── cynexus-common-core
├── cynexus-common-web
├── cynexus-common-security
├── cynexus-common-db
├── cynexus-common-cloud
├── cynexus-common-redis
└── cynexus-common-observability
```

第一阶段可以先建：

```text
cynexus-common-core
cynexus-common-web
cynexus-common-security
cynexus-common-db
cynexus-common-cloud
```

---

## 2.1 `cynexus-common-core`

放最基础的、无 Spring 强依赖的东西。

```text
cynexus-common-core
└── src/main/java/com/cynexus/common/core
    ├── result
    │   ├── Result.java
    │   ├── PageResult.java
    │   └── ResultCode.java
    │
    ├── exception
    │   ├── BizException.java
    │   ├── SystemException.java
    │   └── ErrorCode.java
    │
    ├── id
    │   ├── IdGenerator.java
    │   ├── SnowflakeIdGenerator.java
    │   └── IdConstants.java
    │
    ├── enums
    │   ├── BaseEnum.java
    │   └── EnabledStatus.java
    │
    ├── util
    │   ├── JsonUtils.java
    │   ├── DateTimeUtils.java
    │   └── StringUtils.java
    │
    └── constant
        ├── CommonConstants.java
        └── OperatorConstants.java
```

适合放：

```text
Result
BizException
ErrorCode
Snowflake ID
时间工具
JSON 工具
基础枚举
```

不应该放：

```text
Controller
Service
Mapper
Entity
业务领域对象
```

---

## 2.2 `cynexus-common-web`

放 Web 层公共能力。

```text
cynexus-common-web
└── src/main/java/com/cynexus/common/web
    ├── config
    │   └── WebMvcConfig.java
    │
    ├── advice
    │   ├── GlobalExceptionHandler.java
    │   └── ResponseBodyAdvice.java
    │
    ├── filter
    │   └── TraceIdFilter.java
    │
    ├── validation
    │   └── ValidationErrorHandler.java
    │
    └── request
        ├── RequestContext.java
        └── RequestContextHolder.java
```

适合放：

```text
全局异常处理
统一返回包装
TraceId
参数校验错误处理
WebMvc 配置
```

---

## 2.3 `cynexus-common-security`

你现在是单用户模式，所以这里做轻量安全即可。

```text
cynexus-common-security
└── src/main/java/com/cynexus/common/security
    ├── config
    │   └── CynexusSecurityProperties.java
    │
    ├── filter
    │   └── AdminTokenAuthFilter.java
    │
    ├── context
    │   ├── CurrentOperator.java
    │   └── SecurityContextHolder.java
    │
    └── mode
        └── SecurityMode.java
```

适合放：

```text
single-user 模式
admin token
CurrentOperator
created_by / updated_by 当前操作人
```

第一版不要放：

```text
用户注册
登录
RBAC
OAuth2
权限菜单
多租户
```

---

## 2.4 `cynexus-common-db`

放数据库公共配置。

```text
cynexus-common-db
└── src/main/java/com/cynexus/common/db
    ├── config
    │   ├── MybatisPlusConfig.java
    │   └── DataSourceConfig.java
    │
    ├── handler
    │   └── MetaObjectFillHandler.java
    │
    ├── entity
    │   └── BasePO.java
    │
    ├── mapper
    │   └── BaseMapperX.java
    │
    └── typehandler
        └── JsonbTypeHandler.java
```

适合放：

```text
MyBatis-Plus 配置
分页插件
自动填充 created_at / updated_at / created_by / updated_by
BasePO
JSONB TypeHandler
```

---

## 2.5 `cynexus-common-cloud`

放微服务基础能力。

```text
cynexus-common-cloud
└── src/main/java/com/cynexus/common/cloud
    ├── config
    │   ├── FeignConfig.java
    │   └── NacosConfig.java
    │
    ├── feign
    │   ├── FeignErrorDecoder.java
    │   └── FeignRequestInterceptor.java
    │
    └── loadbalancer
        └── LoadBalancerConfig.java
```

适合放：

```text
Nacos Discovery
Nacos Config
OpenFeign
LoadBalancer
Feign 拦截器
Feign 错误处理
```

---

# 3. 单个微服务标准结构

每个业务服务都按这个结构：

```text
cynexus-{service}
├── pom.xml
│
├── cynexus-{service}-api
├── cynexus-{service}-common
├── cynexus-{service}-domain
├── cynexus-{service}-infra
├── cynexus-{service}-application
│   ├── cynexus-{service}-application-controller
│   ├── cynexus-{service}-application-mq
│   └── cynexus-{service}-application-job
└── cynexus-{service}-starter
```

第一阶段 `mq` 和 `job` 可以不建，或者先空着。

---

# 4. `api` 模块放什么

`api` 是 **跨服务契约模块**，给其他服务依赖。

以 `cynexus-market-api` 为例：

```text
cynexus-market-api
└── src/main/java/com/cynexus/market/api
    ├── dto
    │   ├── MarketOverviewDTO.java
    │   ├── SectorRankDTO.java
    │   ├── SectorSummaryDTO.java
    │   ├── StockDailyFeatureDTO.java
    │   └── StockIntradayFeatureDTO.java
    │
    ├── request
    │   ├── MarketOverviewQuery.java
    │   ├── SectorRankQuery.java
    │   └── StockFeatureQuery.java
    │
    ├── response
    │   └── MarketApiResponse.java
    │
    ├── enums
    │   ├── MarketCode.java
    │   └── TradeMarket.java
    │
    └── feign
        └── MarketFeignClient.java
```

`api` 里可以放：

```text
DTO
Request
Response
FeignClient
跨服务枚举
接口常量
```

`api` 里不要放：

```text
Controller
ServiceImpl
Mapper
PO
DO
Domain Entity
复杂业务逻辑
```

---

# 5. `application` 模块放什么

`application` 是 **用例编排层**。
里面可以拆 controller / mq / job。

以 `cynexus-ai-application` 为例：

```text
cynexus-ai-application
├── pom.xml
│
├── cynexus-ai-application-controller
│   └── src/main/java/com/cynexus/ai/application/controller
│       ├── AiExecutionController.java
│       ├── AiScenarioController.java
│       ├── AiModelController.java
│       ├── AiPromptController.java
│       └── AiToolController.java
│
├── cynexus-ai-application-mq
│   └── src/main/java/com/cynexus/ai/application/mq
│       ├── AiExecutionConsumer.java
│       └── AiTraceConsumer.java
│
└── cynexus-ai-application-job
    └── src/main/java/com/cynexus/ai/application/job
        ├── AiModelHealthCheckJob.java
        ├── AiExecutionCleanJob.java
        └── AiCostStatisticsJob.java
```

Controller 只做：

```text
接收入参
参数校验
调用 application service
返回 response
```

Controller 不做：

```text
模型选择
工具选择
Prompt 拼接
JSON 校验
业务规则判断
DB 直接操作
```

---

# 6. application service 放哪里？

我建议放在 `application-controller` 或单独建 `application-service`。
为了第一版简单，可以先放在：

```text
cynexus-ai-application-controller
└── src/main/java/com/cynexus/ai/application
    ├── controller
    ├── service
    │   ├── ScenarioExecutionAppService.java
    │   ├── ModelConfigAppService.java
    │   ├── PromptTemplateAppService.java
    │   └── ToolConfigAppService.java
    │
    ├── command
    │   ├── ExecuteScenarioCommand.java
    │   └── CreateScenarioCommand.java
    │
    └── assembler
        ├── AiExecutionAssembler.java
        └── AiScenarioAssembler.java
```

如果后面 application 变复杂，再独立拆：

```text
cynexus-ai-application-service
```

---

# 7. `domain` 模块放什么

`domain` 是 **核心业务规则层**，不能依赖 infra。

以 `cynexus-ai-domain` 为例：

```text
cynexus-ai-domain
└── src/main/java/com/cynexus/ai/domain
    ├── model
    │   ├── AiModel.java
    │   ├── AiProvider.java
    │   ├── ModelCapability.java
    │   ├── ModelRuntimeConfig.java
    │   └── ModelRoutePolicy.java
    │
    ├── scenario
    │   ├── AiScenario.java
    │   ├── ScenarioCode.java
    │   ├── ScenarioConfig.java
    │   ├── ScenarioToolBinding.java
    │   └── ScenarioOutputSchema.java
    │
    ├── prompt
    │   ├── PromptTemplate.java
    │   ├── PromptVersion.java
    │   └── PromptVariable.java
    │
    ├── tool
    │   ├── AiTool.java
    │   ├── ToolCode.java
    │   ├── ToolPermission.java
    │   ├── ToolCallPolicy.java
    │   └── ToolCallRecord.java
    │
    ├── execution
    │   ├── AiExecution.java
    │   ├── AiExecutionId.java
    │   ├── AiExecutionStatus.java
    │   ├── AiExecutionStep.java
    │   └── AiOutputValidationResult.java
    │
    ├── service
    │   ├── ModelRoutingDomainService.java
    │   ├── ToolPermissionDomainService.java
    │   ├── OutputValidationDomainService.java
    │   └── FallbackPolicyDomainService.java
    │
    ├── repository
    │   ├── AiModelRepository.java
    │   ├── AiScenarioRepository.java
    │   ├── PromptTemplateRepository.java
    │   ├── AiToolRepository.java
    │   └── AiExecutionRepository.java
    │
    └── event
        ├── AiExecutionCreatedEvent.java
        ├── AiExecutionSucceededEvent.java
        └── AiExecutionFailedEvent.java
```

domain 里放：

```text
Entity
ValueObject
Domain Service
Repository 接口
Domain Event
领域规则
```

domain 里不要放：

```text
MyBatis Mapper
PO
FeignClient
Spring AI ChatClient
RedisTemplate
Controller
```

---

# 8. `infra` 模块放什么

`infra` 是 **技术实现层**。

以 `cynexus-ai-infra` 为例：

```text
cynexus-ai-infra
└── src/main/java/com/cynexus/ai/infra
    ├── persistence
    │   ├── entity
    │   │   ├── AiModelConfigPO.java
    │   │   ├── AiProviderConfigPO.java
    │   │   ├── AiScenarioConfigPO.java
    │   │   ├── AiPromptTemplatePO.java
    │   │   ├── AiToolDefinitionPO.java
    │   │   ├── AiToolBindingPO.java
    │   │   ├── AiExecutionLogPO.java
    │   │   └── AiToolCallLogPO.java
    │   │
    │   ├── mapper
    │   │   ├── AiModelConfigMapper.java
    │   │   ├── AiScenarioConfigMapper.java
    │   │   ├── AiPromptTemplateMapper.java
    │   │   ├── AiToolDefinitionMapper.java
    │   │   └── AiExecutionLogMapper.java
    │   │
    │   ├── repository
    │   │   ├── AiModelRepositoryImpl.java
    │   │   ├── AiScenarioRepositoryImpl.java
    │   │   ├── PromptTemplateRepositoryImpl.java
    │   │   └── AiExecutionRepositoryImpl.java
    │   │
    │   └── converter
    │       ├── AiModelConverter.java
    │       ├── AiScenarioConverter.java
    │       └── PromptTemplateConverter.java
    │
    ├── ai
    │   ├── adapter
    │   │   ├── AiModelAdapter.java
    │   │   ├── OpenAiModelAdapter.java
    │   │   ├── DeepSeekModelAdapter.java
    │   │   ├── QwenModelAdapter.java
    │   │   └── ClaudeModelAdapter.java
    │   │
    │   ├── registry
    │   │   └── AiModelAdapterRegistry.java
    │   │
    │   ├── factory
    │   │   ├── DynamicChatClientFactory.java
    │   │   └── SpringAiChatClientFactory.java
    │   │
    │   └── output
    │       ├── StructuredOutputParser.java
    │       └── JsonSchemaValidator.java
    │
    ├── tool
    │   ├── registry
    │   │   └── AiToolRegistry.java
    │   │
    │   ├── market
    │   │   └── MarketDataTools.java
    │   │
    │   ├── document
    │   │   └── DocumentTools.java
    │   │
    │   └── strategy
    │       └── StrategyTools.java
    │
    ├── client
    │   ├── market
    │   │   └── MarketServiceClientAdapter.java
    │   │
    │   └── document
    │       └── DocumentServiceClientAdapter.java
    │
    ├── secret
    │   ├── SecretResolver.java
    │   ├── EnvSecretResolver.java
    │   └── KmsSecretResolver.java
    │
    ├── cache
    │   ├── AiConfigCache.java
    │   └── PromptTemplateCache.java
    │
    └── observability
        ├── AiTraceLogger.java
        ├── AiTokenUsageRecorder.java
        └── AiCostRecorder.java
```

infra 放：

```text
PO / Mapper
RepositoryImpl
Feign 适配
Spring AI 适配
Redis 缓存
第三方 SDK
Secret Resolver
日志记录
技术配置
```

---

# 9. `starter` 模块放什么

`starter` 是 **真正独立启动的 Spring Boot 应用**。

以 `cynexus-ai-starter` 为例：

```text
cynexus-ai-starter
└── src/main
    ├── java/com/cynexus/ai
    │   └── CynexusAiApplication.java
    │
    └── resources
        ├── application.yml
        ├── application-dev.yml
        ├── application-local.yml
        └── logback-spring.xml
```

只放：

```text
启动类
application.yml
日志配置
启动相关配置
```

不要放：

```text
业务 Controller
Domain Entity
Mapper
ServiceImpl
```

启动类：

```java
package com.cynexus.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients(basePackages = "com.cynexus")
@SpringBootApplication(scanBasePackages = "com.cynexus")
public class CynexusAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(CynexusAiApplication.class, args);
    }
}
```

---

# 10. AI 服务完整 folder 规范

```text
cynexus-ai
├── cynexus-ai-api
│   └── src/main/java/com/cynexus/ai/api
│       ├── dto
│       ├── request
│       ├── response
│       ├── enums
│       └── feign
│
├── cynexus-ai-common
│   └── src/main/java/com/cynexus/ai/common
│       ├── constant
│       ├── enums
│       └── exception
│
├── cynexus-ai-domain
│   └── src/main/java/com/cynexus/ai/domain
│       ├── model
│       ├── scenario
│       ├── prompt
│       ├── tool
│       ├── execution
│       ├── service
│       ├── repository
│       └── event
│
├── cynexus-ai-infra
│   └── src/main/java/com/cynexus/ai/infra
│       ├── persistence
│       │   ├── entity
│       │   ├── mapper
│       │   ├── repository
│       │   └── converter
│       │
│       ├── ai
│       │   ├── adapter
│       │   ├── registry
│       │   ├── factory
│       │   └── output
│       │
│       ├── tool
│       │   ├── registry
│       │   ├── market
│       │   ├── document
│       │   └── strategy
│       │
│       ├── client
│       ├── secret
│       ├── cache
│       └── observability
│
├── cynexus-ai-application
│   └── cynexus-ai-application-controller
│       └── src/main/java/com/cynexus/ai/application
│           ├── controller
│           ├── service
│           ├── command
│           ├── query
│           └── assembler
│
└── cynexus-ai-starter
    └── src/main
        ├── java/com/cynexus/ai/CynexusAiApplication.java
        └── resources/application.yml
```

---

# 11. Market 服务完整 folder 规范

```text
cynexus-market
├── cynexus-market-api
│   └── src/main/java/com/cynexus/market/api
│       ├── dto
│       │   ├── MarketOverviewDTO.java
│       │   ├── SectorRankDTO.java
│       │   ├── SectorSummaryDTO.java
│       │   ├── StockDailyFeatureDTO.java
│       │   └── StockIntradayFeatureDTO.java
│       │
│       ├── request
│       │   ├── MarketOverviewQuery.java
│       │   ├── SectorRankQuery.java
│       │   └── StockFeatureQuery.java
│       │
│       ├── response
│       ├── enums
│       └── feign
│           └── MarketFeignClient.java
│
├── cynexus-market-common
│   └── src/main/java/com/cynexus/market/common
│       ├── constant
│       ├── enums
│       └── exception
│
├── cynexus-market-domain
│   └── src/main/java/com/cynexus/market/domain
│       ├── market
│       │   ├── MarketOverview.java
│       │   ├── MarketBreadth.java
│       │   └── MarketMoneyFlow.java
│       │
│       ├── sector
│       │   ├── Sector.java
│       │   ├── SectorRank.java
│       │   └── SectorStrength.java
│       │
│       ├── stock
│       │   ├── Stock.java
│       │   ├── KLine.java
│       │   ├── DailyFeature.java
│       │   └── IntradayFeature.java
│       │
│       ├── service
│       │   ├── MarketFeatureDomainService.java
│       │   └── SectorStrengthDomainService.java
│       │
│       ├── repository
│       │   ├── MarketOverviewRepository.java
│       │   ├── SectorRepository.java
│       │   └── StockFeatureRepository.java
│       │
│       └── event
│
├── cynexus-market-infra
│   └── src/main/java/com/cynexus/market/infra
│       ├── persistence
│       │   ├── entity
│       │   ├── mapper
│       │   ├── repository
│       │   └── converter
│       │
│       ├── datafeed
│       │   ├── MarketDataFeedClient.java
│       │   └── MockMarketDataFeedClient.java
│       │
│       ├── cache
│       │   └── MarketFeatureCache.java
│       │
│       └── job
│           └── MarketFeatureCalculateJob.java
│
├── cynexus-market-application
│   └── cynexus-market-application-controller
│       └── src/main/java/com/cynexus/market/application
│           ├── controller
│           │   └── MarketController.java
│           │
│           ├── service
│           │   ├── MarketOverviewAppService.java
│           │   ├── SectorRankAppService.java
│           │   └── StockFeatureAppService.java
│           │
│           ├── query
│           └── assembler
│
└── cynexus-market-starter
    └── src/main
        ├── java/com/cynexus/market/CynexusMarketApplication.java
        └── resources/application.yml
```

---

# 12. 类放置规则

## Controller

放这里：

```text
xxx-application-controller
└── com.cynexus.xxx.application.controller
```

示例：

```text
MarketController
AiExecutionController
AiScenarioController
```

不要放在 infra，也不要放在 starter。

---

## Application Service

放这里：

```text
xxx-application-controller
└── com.cynexus.xxx.application.service
```

示例：

```text
ScenarioExecutionAppService
MarketOverviewAppService
SectorRankAppService
```

职责：

```text
用例编排
调 domain service
调 repository
调 infra adapter
事务控制
```

---

## Command / Query

放这里：

```text
xxx-application-controller
└── com.cynexus.xxx.application.command
└── com.cynexus.xxx.application.query
```

示例：

```text
ExecuteScenarioCommand
CreateAiModelCommand
MarketOverviewQuery
```

---

## DTO

跨服务 DTO 放：

```text
xxx-api
└── com.cynexus.xxx.api.dto
```

内部应用层 DTO 可以放：

```text
xxx-application-controller
└── com.cynexus.xxx.application.dto
```

建议第一版优先放 api DTO，避免重复。

---

## FeignClient

被调用服务的 Feign 契约放在被调用方 api：

```text
cynexus-market-api
└── com.cynexus.market.api.feign.MarketFeignClient
```

调用方依赖这个 api 模块。

例如：

```text
cynexus-ai-infra 依赖 cynexus-market-api
```

---

## Domain Entity

放这里：

```text
xxx-domain
└── com.cynexus.xxx.domain.{subdomain}
```

示例：

```text
com.cynexus.ai.domain.scenario.AiScenario
com.cynexus.market.domain.stock.Stock
```

---

## Value Object

可以放在对应领域目录下：

```text
com.cynexus.ai.domain.execution.AiExecutionId
com.cynexus.ai.domain.scenario.ScenarioCode
com.cynexus.market.domain.stock.StockCode
```

如果很多，也可以单独建：

```text
domain/{subdomain}/valueobject
```

---

## Domain Service

放这里：

```text
xxx-domain
└── com.cynexus.xxx.domain.service
```

示例：

```text
ModelRoutingDomainService
ToolPermissionDomainService
MarketFeatureDomainService
```

---

## Repository 接口

放 domain：

```text
xxx-domain
└── com.cynexus.xxx.domain.repository
```

示例：

```text
AiModelRepository
AiScenarioRepository
MarketOverviewRepository
```

---

## Repository 实现

放 infra：

```text
xxx-infra
└── com.cynexus.xxx.infra.persistence.repository
```

示例：

```text
AiModelRepositoryImpl
AiScenarioRepositoryImpl
MarketOverviewRepositoryImpl
```

---

## PO / Mapper

放 infra：

```text
xxx-infra
└── com.cynexus.xxx.infra.persistence.entity
└── com.cynexus.xxx.infra.persistence.mapper
```

示例：

```text
AiModelConfigPO
AiModelConfigMapper
```

不要把 PO 暴露到 domain / api。

---

## Converter / Assembler

区分一下：

```text
Assembler：application 层 DTO 转换
Converter：infra 层 PO / Domain 转换
```

放置：

```text
application/assembler
infra/persistence/converter
```

示例：

```text
AiExecutionAssembler
AiModelConverter
```

---

## 第三方 Client

放 infra：

```text
xxx-infra
└── com.cynexus.xxx.infra.client
```

示例：

```text
MarketServiceClientAdapter
ExternalMarketDataClient
OpenAiApiClient
```

---

## AI Model Adapter

只属于 `cynexus-ai-infra`：

```text
cynexus-ai-infra
└── com.cynexus.ai.infra.ai.adapter
```

示例：

```text
AiModelAdapter
OpenAiModelAdapter
DeepSeekModelAdapter
QwenModelAdapter
ClaudeModelAdapter
```

---

## Tool

只属于 `cynexus-ai-infra`：

```text
cynexus-ai-infra
└── com.cynexus.ai.infra.tool
```

示例：

```text
MarketDataTools
DocumentTools
StrategyTools
AiToolRegistry
```

Tool 是 AI 服务调用其他服务的适配层，不是领域对象。

---

## Job

如果是 HTTP 无关的任务，放：

```text
xxx-application-job
└── com.cynexus.xxx.application.job
```

如果是技术层定时同步，比如行情数据源同步，也可以放 infra job：

```text
xxx-infra
└── com.cynexus.xxx.infra.job
```

建议：

```text
业务调度入口：application-job
技术同步实现：infra/job
```

---

## MQ Consumer

放：

```text
xxx-application-mq
└── com.cynexus.xxx.application.mq
```

不要放 domain。

---

# 13. 依赖方向规范

必须遵守：

```text
starter
  ↓
application
  ↓
infra
  ↓
domain
  ↓
common
```

以及：

```text
api 独立给其他服务依赖
```

推荐依赖关系：

```text
xxx-domain
  依赖 common-core
  不依赖 infra
  不依赖 application
  不依赖 starter

xxx-infra
  依赖 xxx-domain
  依赖 xxx-api
  依赖 common-db / common-cloud / common-redis

xxx-application-controller
  依赖 xxx-api
  依赖 xxx-domain
  依赖 xxx-infra
  依赖 common-web

xxx-starter
  依赖 xxx-application-controller
  依赖 xxx-infra
  依赖 common-web / common-cloud / common-db
```

---

# 14. 禁止规则

为了防止项目变乱，建议一开始就定这些规则：

```text
1. starter 只放启动类和配置，不放业务代码
2. api 只放契约，不放实现
3. domain 不允许依赖 Spring AI / MyBatis / Feign / Redis
4. Controller 不直接调用 Mapper
5. Controller 不拼 Prompt
6. Controller 不做模型路由
7. PO 不允许返回给前端
8. Domain Entity 不允许直接作为 HTTP Response
9. FeignClient 只放 api 模块
10. Tool 只放 ai-infra
11. 业务服务之间不能直接查对方数据库
12. AI 服务不能直接拥有 market/document/strategy 的领域逻辑
```

---

# 15. 第一版最小落地目录

如果你现在让 Codex 先建第一版，最小结构可以是：

```text
cynexus
├── cynexus-common
│   ├── cynexus-common-core
│   ├── cynexus-common-web
│   ├── cynexus-common-security
│   ├── cynexus-common-db
│   └── cynexus-common-cloud
│
├── cynexus-ai
│   ├── cynexus-ai-api
│   ├── cynexus-ai-common
│   ├── cynexus-ai-domain
│   ├── cynexus-ai-infra
│   ├── cynexus-ai-application
│   │   └── cynexus-ai-application-controller
│   └── cynexus-ai-starter
│
└── cynexus-market
    ├── cynexus-market-api
    ├── cynexus-market-common
    ├── cynexus-market-domain
    ├── cynexus-market-infra
    ├── cynexus-market-application
    │   └── cynexus-market-application-controller
    └── cynexus-market-starter
```

---

# 16. 可以直接给 Codex 的 folder 规则

你可以把这段贴给 Codex：

```text
Follow this folder convention strictly:

1. Root project is cynexus, groupId is com.cynexus.

2. Each business service must be a Maven aggregate module:
   cynexus-{service}
   - cynexus-{service}-api
   - cynexus-{service}-common
   - cynexus-{service}-domain
   - cynexus-{service}-infra
   - cynexus-{service}-application
     - cynexus-{service}-application-controller
   - cynexus-{service}-starter

3. api module:
   package: com.cynexus.{service}.api
   contains only DTO, request, response, enums, feign client.
   No implementation classes.

4. application-controller module:
   package: com.cynexus.{service}.application
   contains controller, application service, command, query, assembler.
   Controller must not call Mapper directly.

5. domain module:
   package: com.cynexus.{service}.domain
   contains entity, value object, domain service, repository interface, domain event.
   Domain must not depend on MyBatis, Feign, Spring AI, Redis, or Controller.

6. infra module:
   package: com.cynexus.{service}.infra
   contains persistence entity, mapper, repository implementation, external client, cache, adapter, converter.
   For cynexus-ai, infra also contains AI model adapters, dynamic chat client factory, tool registry, and tool implementations.

7. starter module:
   package: com.cynexus.{service}
   contains only Spring Boot application class and application.yml.
   No business code.

8. common modules:
   cynexus-common-core: Result, Exception, ErrorCode, ID generator, utils.
   cynexus-common-web: global exception handler, validation, traceId, web config.
   cynexus-common-security: single-user token, CurrentOperator.
   cynexus-common-db: MyBatis-Plus config, BasePO, auto fill.
   cynexus-common-cloud: Nacos, OpenFeign, LoadBalancer config.

9. Feign client should be placed in the provider service api module.
   Example:
   cynexus-market-api/com.cynexus.market.api.feign.MarketFeignClient

10. AI tools should be placed only in:
    cynexus-ai-infra/com.cynexus.ai.infra.tool

11. AI model adapters should be placed only in:
    cynexus-ai-infra/com.cynexus.ai.infra.ai.adapter

12. Do not create document, strategy, gateway, admin, oss, iam in phase 1.
```

最终你可以把它保存成：

```text
docs/architecture/folder-convention.md
```

或者直接追加到：

```text
CODEX_TASK.md
```
