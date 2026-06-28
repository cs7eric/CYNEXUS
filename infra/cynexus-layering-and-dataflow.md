# Cynexus 分层与数据流转落地说明

本文档用于统一 `cynexus` 项目的分层职责、依赖方向、对象命名、数据流转方式，以及数据库 / Redis / 三方 SDK / Feign 的落点规范。

---

## 1. 总体目标

`cynexus` 采用的是一套偏工程实用主义的分层结构：

- 保留 `api / application / domain / infra` 四层
- 允许 `domain service impl` 直接 Spring 化
- 不允许 `domain` 直接依赖具体技术实现
- 持久化、缓存、HTTP、SDK 等技术细节统一收敛到 `infra`

核心原则只有一句：

**domain 依赖能力抽象，infra 提供技术实现。**

---

## 2. 推荐模块依赖方向

推荐依赖方向：

```text
api
application -> domain
infra -> domain
starter -> application + infra + api
```

补充说明：

- `api`：对外契约层，放 DTO / Request / FeignClient
- `application`：应用编排层，承接 controller，请求转换、事务边界、流程编排
- `domain`：业务规则层，放 BO / Entity / DomainService / Repository 接口 / Gateway 接口
- `infra`：技术实现层，放 PO / Mapper / BasicService / RepositoryImpl / GatewayImpl
- `starter`：Spring Boot 启动层

不推荐：

```text
domain -> infra
```

因为这样会让领域层直接感知技术实现，后续会导致边界混乱。

---

## 3. 各层职责

### 3.1 api 层

目录示例：

```text
cynexus-market-api
  └─ api
      ├─ dto
      ├─ request
      └─ feign
```

职责：

- 对外暴露跨服务调用契约
- 放 `DTO`
- 放 `Request`
- 放 `FeignClient`

可放内容：

- `XXXDTO`
- `XXXRequest`
- `XXXFeignClient`

不放内容：

- BO
- PO
- Mapper
- RepositoryImpl
- RedisTemplate

---

### 3.2 application 层

目录示例：

```text
cynexus-market-application-controller
  ├─ controller
  ├─ service
  └─ convert
```

职责：

- 提供 HTTP 接口
- 调用 `AppService`
- 做 DTO 和 BO 转换
- 处理应用层编排
- 放事务边界（推荐）

可放内容：

- `XXXController`
- `XXXAppService`
- `XXXDTOConverter`

不放内容：

- PO
- Mapper
- Redis/HTTP/SDK 直接调用

---

### 3.3 domain 层

目录示例：

```text
cynexus-market-domain
  ├─ entity
  ├─ service
  ├─ service/impl
  ├─ repository
  └─ gateway
```

职责：

- 表达业务模型和业务规则
- 聚合多个仓储、多个外部能力完成业务处理
- 定义对持久化和外部能力的抽象接口

当前项目约定：

- `domain service impl` 可以使用 `@Service`
- `domain` 模块允许依赖 Spring 基础能力（当前已补 `spring-context`）
- 但是 `domain` 仍然不能直接依赖 RedisTemplate、HTTP Client、三方 SDK、Mapper、PO

可放内容：

- `XXXBO`
- `XXXDomainService`
- `XXXDomainServiceImpl`
- `XXXRepository`（接口）
- `XXXGateway` / `XXXPort`（接口）

不放内容：

- `XXXPO`
- `XXXMapper`
- `RedisTemplate`
- `WebClient`
- `OpenAIClient`
- `PythonCrawlerHttpClient`
- `RepositoryImpl`

---

### 3.4 infra 层

目录示例：

```text
cynexus-market-infra
  ├─ basic
  │   ├─ entity
  │   ├─ mapper
  │   └─ service
  ├─ repository
  ├─ gateway
  ├─ convert
  └─ config
```

职责：

- 实现 domain 定义的能力接口
- 落地数据库访问
- 落地 Redis / HTTP / SDK / MQ / 文件存储等技术集成

可放内容：

- `XXXPO`
- `XXXMapper`
- `XXXMapper.xml`
- `XXXBasicService`
- `XXXBasicServiceImpl`
- `XXXRepositoryImpl`
- `XXXGatewayImpl`
- `XXXPOConverter`

---

## 4. 对象后缀规范

### 4.1 DTO

含义：

- 对外传输对象
- 给 Controller / Feign 使用

位置：

```text
api/dto
```

命名：

- `MarketOverviewDTO`
- `MaCountDailyDTO`

---

### 4.2 Request

含义：

- 入参对象
- 用于创建、修改、查询等接口请求

位置：

```text
api/request
```

命名：

- `AiExecuteRequest`
- `SchedulerJobCreateRequest`

---

### 4.3 BO

含义：

- 领域业务对象
- 在 domain 内流转

位置：

```text
domain/entity
```

命名：

- `MarketOverviewBO`
- `MaCountDailyBO`

---

### 4.4 PO

含义：

- 持久化对象
- 与数据库表结构直接对应

位置：

```text
infra/basic/entity
```

命名：

- `MarketOverviewPO`
- `MaCountDailyPO`

---

### 4.5 Converter

含义：

- 对象转换器

位置与方向：

- `application/convert/XXXDTOConverter`
  - `DTO <-> BO`
- `infra/convert/XXXPOConverter`
  - `PO <-> BO`

统一约定：

- 继承 `BaseMapper<S, T>`
- 提供 `INSTANCE = Mappers.getMapper(...)`

---

### 4.6 Repository

含义：

- 面向 domain 的持久化抽象
- 屏蔽数据库细节

位置：

- 接口：`domain/repository`
- 实现：`infra/repository`

命名：

- `MaCountDailyRepository`
- `MaCountDailyRepositoryImpl`

适用场景：

- PostgreSQL / ClickHouse / 其他持久化存取

---

### 4.7 BasicService

含义：

- infra 内部基础单表 CRUD 服务
- 直接面向 PO 和 Mapper

位置：

```text
infra/basic/service
infra/basic/service/impl
```

命名：

- `MaCountDailyBasicService`
- `MaCountDailyBasicServiceImpl`

适用场景：

- 单表基础增删改查
- 不直接暴露给 domain

---

### 4.8 外部能力抽象接口

含义：

- 领域所需外部能力抽象
- 不是数据库仓储，而是缓存、远程调用、三方 SDK、对象存储、消息等能力接口

接口位置建议：

```text
domain/port
domain/client
domain/provider
domain/cache
```

实现位置建议：

```text
infra/port
infra/client
infra/provider
infra/cache
infra/remote
```

命名原则：

- 能按能力分类命名，就不要默认都叫 `Gateway`
- 优先使用更具体的语义名称
- 只有在能力边界很泛、确实不容易归类时，再使用 `Gateway`

分类建议如下：

#### 4.8.1 缓存能力

推荐命名：

- `XXXCache`
- `XXXCachePort`
- `XXXCacheClient`

示例：

- `QuoteCache`
- `AiPromptCache`
- `MarketSnapshotCachePort`

#### 4.8.2 远程 HTTP / Python / 外部系统调用

推荐命名：

- `XXXClient`
- `XXXRemoteClient`
- `XXXPort`

示例：

- `PythonSchedulerClient`
- `CollectorRemoteClient`
- `MarketDataPort`

#### 4.8.3 三方模型 / SDK / 可替换供应商能力

推荐命名：

- `XXXProvider`
- `XXXPort`

示例：

- `TranscriptProvider`
- `LlmProvider`
- `EmbeddingProvider`
- `SpeechRecognitionPort`

#### 4.8.4 文件 / 对象存储 / 消息能力

推荐命名：

- `XXXStorage`
- `XXXPublisher`
- `XXXPort`

示例：

- `AudioStorage`
- `ReportFileStorage`
- `MarketEventPublisher`

#### 4.8.5 难以归类的统一外部能力

只有在下面场景才建议使用：

- 能力本身比较宽泛
- 不适合叫 `Client`
- 不适合叫 `Provider`
- 不适合叫 `Cache`
- 不适合叫 `Storage`

这时才使用：

- `XXXGateway`

---

### 4.9 FeignClient

含义：

- 微服务之间的 RPC 契约

位置：

```text
api/feign
```

命名：

- `MarketFeignClient`
- `MaCountDailyFeignClient`

---

## 5. 数据流转方式

---

### 5.1 HTTP CRUD 数据流

适用于单表或标准业务 CRUD。

```text
Controller
  -> AppService
  -> DomainService
  -> Repository(interface)
  -> RepositoryImpl
  -> BasicService
  -> Mapper / MyBatis XML
  -> PostgreSQL
```

对象流转：

```text
DTO
  -> BO
  -> PO
```

返回时反向：

```text
PO
  -> BO
  -> DTO
```

---

### 5.2 多仓储业务处理流

适用于一个业务聚合多个表、多种仓储能力。

```text
Controller
  -> AppService
  -> DomainService
      -> RepositoryA
      -> RepositoryB
      -> RepositoryC
  -> 业务聚合处理
```

说明：

- 这种场景正是 `domain service` 的职责范围
- `domain service` 可以聚合多个 repository 的数据做业务规则处理

---

### 5.3 Redis / 三方 SDK / HTTP 外部能力流

推荐流转：

```text
AppService
  -> DomainService
  -> 外部能力抽象接口
  -> 外部能力实现(infra)
  -> Redis / SDK / HTTP / Python
```

不推荐：

```text
DomainService -> RedisTemplate
DomainService -> WebClient
DomainService -> SDK Client
```

原因：

- domain 不应该感知技术实现细节

---

### 5.4 跨服务调用流

适用于微服务之间通过 OpenFeign 调用。

```text
Service A
  -> api/feign/XXXFeignClient
  -> Gateway / Controller of Service B
```

FeignClient 放置位置：

```text
*-api/src/main/java/.../api/feign
```

---

## 6. Repository、BasicService、Mapper 三层怎么走

这三层职责要分清。

### 6.1 Mapper

职责：

- 最底层数据库访问
- 直接执行 SQL
- MyBatis / MyBatis-Plus 映射

面向对象：

- `PO`

---

### 6.2 BasicService

职责：

- 在 infra 内部封装单表基础 CRUD
- 简化 RepositoryImpl

面向对象：

- `PO`

调用关系：

```text
BasicService -> Mapper
```

---

### 6.3 Repository

职责：

- 面向 domain 暴露仓储能力
- 把 `PO` 转成 `BO`
- 聚合必要的持久化调用

调用关系：

```text
DomainService -> Repository(interface)
RepositoryImpl -> BasicService -> Mapper
```

一句话总结：

- `Mapper`：会 SQL
- `BasicService`：会单表 CRUD
- `Repository`：会给 domain 提供业务可用的数据访问能力

---

## 7. Redis / SDK / HTTP 能力怎么命名

推荐按能力分类命名，不要默认全部叫 `Gateway`。

### 7.1 数据库存取

用：

- `Repository`

例如：

- `SchedulerJobRepository`
- `MaCountDailyRepository`

---

### 7.2 缓存能力

优先用：

- `Cache`
- `CachePort`
- `CacheClient`

例如：

- `QuoteCache`
- `AiPromptCache`
- `MarketSnapshotCachePort`

实现类可用：

- `RedisQuoteCache`
- `RedisAiPromptCache`

---

### 7.3 三方模型 / SDK

优先用：

- `Provider`
- `Port`

例如：

- `TranscriptProvider`
- `LlmProvider`
- `EmbeddingProvider`
- `SpeechRecognitionPort`

实现类可用：

- `OpenAiLlmProvider`
- `DashScopeTranscriptProvider`
- `ParaformerSpeechRecognitionProvider`

---

### 7.4 Python 服务 / 外部 HTTP 服务

优先用：

- `Client`
- `RemoteClient`
- `Port`

例如：

- `CollectorRemoteClient`
- `PythonSchedulerClient`
- `MarketDataPort`

实现类可用：

- `HttpCollectorRemoteClient`
- `HttpPythonSchedulerClient`

---

### 7.5 文件 / 对象存储 / 消息能力

优先用：

- `Storage`
- `Publisher`
- `Port`

例如：

- `AudioStorage`
- `ReportFileStorage`
- `MarketEventPublisher`

---

### 7.6 什么时候才用 Gateway

只有在下面情况才建议使用：

- 这个能力本身比较宽泛
- 不适合叫 `Client`
- 不适合叫 `Provider`
- 不适合叫 `Cache`
- 不适合叫 `Storage`

例如：

- `ExternalMarketGateway`
- `TranscriptGateway`

---

## 8. 事务推荐放哪层

推荐优先放在：

- `application service`

原因：

- application 更适合作为一次用例执行边界
- domain service 可以聚焦业务规则

但如果某个事务强绑定某个领域服务，也可以放在：

- `domain service`

项目内需要统一约定，不要混乱分布。

当前建议：

- CRUD / 编排类事务：放 `AppService`
- 强业务聚合类事务：可放 `DomainService`

---

## 9. 当前项目的落地结论

### 9.1 允许

- `domain service impl` 使用 `@Service`
- `domain service` 聚合多个 repository / 外部能力抽象接口
- `api/feign/XXXFeignClient` 放在 `*-api`

### 9.2 不允许

- `domain` 直接依赖 `Mapper`
- `domain` 直接依赖 `BasicService`
- `domain` 直接依赖 `RedisTemplate`
- `domain` 直接依赖 HTTP Client / SDK Client
- `domain` 直接依赖 `infra` 具体实现类

---

## 10. 推荐示例

### 10.1 数据库场景

```text
MaCountDailyController
  -> MaCountDailyAppService
  -> MaCountDailyDomainService
  -> MaCountDailyRepository
  -> MaCountDailyRepositoryImpl
  -> MaCountDailyBasicService
  -> MaCountDailyMapper
```

### 10.2 Redis 场景

```text
MarketOverviewDomainService
  -> QuoteCache
  -> RedisQuoteCache
  -> RedisTemplate
```

### 10.3 Python 服务场景

```text
SchedulerDomainService
  -> PythonSchedulerClient
  -> HttpPythonSchedulerClient
  -> WebClient / OpenFeign / RestClient
```

---

## 11. 最终原则

请始终记住这四句：

1. `DTO` 不进 domain
2. `PO` 不进 domain
3. `domain` 依赖抽象，不依赖技术实现
4. `infra` 负责把技术能力翻译成 domain 可用能力

如果以后新增能力时不知道应该放哪层，优先按这个判断：

- 是“业务规则”还是“技术接入”？
- 是“能力抽象”还是“能力实现”？

抽象进 `domain`，实现进 `infra`。
