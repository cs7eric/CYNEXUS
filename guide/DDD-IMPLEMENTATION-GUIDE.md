# DevSphere DDD 项目规范与落地指南

> 版本：1.0  
> 适用：基于 Spring Boot + Maven 的新业务模块或微服务  
> 目标：统一业务边界、代码位置、依赖方向、数据流和交付检查标准

## 1. 先说明：本项目采用的 DDD 形态

DevSphere 不是严格意义上的纯六边形架构，而是“按限界上下文拆分的分层 DDD”实现：

```text
dev-sphere-{context}
├── {context}-api                 # 对外契约模块，当前多数为空
├── {context}-application         # 入口与用例编排
│   ├── ...-controller             # HTTP 接口
│   ├── ...-job                    # 定时任务
│   └── ...-mq                     # 消息消费者/生产者
├── {context}-domain               # 领域模型和领域服务
├── {context}-infra                # 数据库、缓存、搜索、第三方适配
├── {context}-common               # 本上下文内共享的基础类型
└── {context}-starter              # 启动类、环境配置、模块装配
```

在现有代码中可以观察到以下稳定约定：

| 责任 | 典型位置 | 典型类型 |
|---|---|---|
| 接口入参和出参 | `*-application-*-controller/.../application/dto` | `SubjectInfoDTO`、`AuthUserDTO` |
| DTO 转换 | `.../application/convert` | `SubjectInfoDTOConverter` |
| 领域对象 | `*-domain/.../domain/entity` 或 `.../bo` | `SubjectInfoBO`、`ShareArticleBO` |
| 领域服务 | `*-domain/.../domain/service` | `SubjectInfoDomainService` |
| 领域策略/分派 | `*-domain/.../domain/handler` | `SubjectTypeHandlerFactory` |
| 持久化对象 | `*-infra/.../infra/basic/entity` | `SubjectInfo`、`AuthUser` |
| DAO 和 SQL | `*-infra/.../infra/basic/mapper`、`src/main/resources/mapper` | `SubjectInfoDao`、`AuthUserDao.xml` |
| 基础设施服务 | `*-infra/.../infra/basic/service` | `SubjectInfoService` |
| Redis、ES、第三方 | `*-infra/.../infra/basic/redis`、`.../es`、`.../adapter` | `RedisUtil`、`SubjectEsService` |
| 通用响应、分页、枚举 | `*-common/.../common/entity`、`common/enums` | `Result`、`PageResult` |
| 启动与环境 | `*-starter/src/main` | `SubjectApplication`、`application-dev.yml` |

新项目应继承这套“按业务上下文拆分、按职责放置代码”的思想，但要采用下文的依赖倒置规则，避免把现有的层间耦合继续扩大。

## 2. 核心规则

### 2.1 先按业务边界拆分，不按技术表拆分

一个顶层业务模块对应一个限界上下文，例如 `auth`、`subject`、`practice`、`circle`。上下文内部再按 application/domain/infra 分层。

不要建立一个全局的 `controller`、`service`、`entity`、`mapper` 大目录，把所有业务揉在一起。这样会让同名概念、事务边界和团队责任边界逐渐失控。

拆分上下文时至少回答：

1. 谁拥有这个业务概念和数据？
2. 哪些规则必须在同一个事务内完成？
3. 哪些交互是同步调用，哪些可以通过事件异步完成？
4. 对外暴露的是业务能力，还是数据库字段？

### 2.2 依赖方向必须单向

新项目采用以下依赖关系：

```text
starter ───────► application ───────► domain
   │                    │                 ▲
   └──────────────► infra ───────────────┘
                         
common 只放稳定的跨层基础类型，不依赖具体业务层
api    只放跨服务/跨模块契约，不放实现
```

更精确的规则如下：

| 层 | 可以依赖 | 禁止依赖 |
|---|---|---|
| `application` | `domain`、`common`、输入输出适配器 | DAO、PO、SQL、Redis 客户端、ES 客户端 |
| `domain` | JDK、领域库、`common` 中无业务基础类型 | Spring Web、MyBatis、PO、DAO、Redis、ES、第三方 SDK |
| `infra` | `domain` 定义的端口、`common`、数据库/缓存/搜索 SDK | application controller |
| `starter` | application、infra、配置组件 | 在启动类中编写业务逻辑 |
| `common` | 通用基础库 | 任一具体业务上下文的 domain/infra |
| `api` | DTO/命令/查询/事件契约依赖 | 数据库实体、业务实现、Spring Bean |

推荐做法是由 domain 定义端口，infra 提供实现：

```text
domain.port.UserRepository
        ▲
        │ implements
infra.repository.mybatis.MyBatisUserRepository
        │
        └── UserMapper / UserPO / UserMapper.xml
```

现有 `auth`、`subject` 的 domain POM 直接依赖 infra，controller 也直接依赖 infra。这是当前项目的事实，不应作为新项目的目标规范。若暂时无法改造旧模块，至少禁止新增 application 对 PO/DAO 的直接调用。

### 2.3 每层只使用自己的模型

模型边界必须清楚：

| 模型 | 所属层 | 用途 | 能否跨层暴露 |
|---|---|---|---|
| DTO | application/api | HTTP、RPC、消息契约 | 只在适配器边界使用 |
| Command/Query | application/api | 表达用例输入 | 只传给 application 用例 |
| BO/Domain Model | domain | 表达业务语义和规则 | 不直接作为 HTTP 合同 |
| Aggregate/Entity/Value Object | domain | 维护不变量和状态变化 | 不直接作为持久化模型 |
| PO/DO | infra | 数据库或搜索文档映射 | 不得返回 controller |
| Result/PageResult | application/common | 统一接口协议 | 不得进入 domain |

每种转换都放在源模型所属的边界附近：

- `DTO ↔ BO`：`application/convert`
- `PO ↔ Domain Model`：`infra` 或 `domain` 中明确命名的 adapter/converter
- `Command ↔ Domain`：application 用例内部完成
- `Domain Event ↔ Integration Event`：在事件适配层转换

不要为了“少写一次转换”直接把 PO 返回给 controller，也不要让 domain service 接收 DTO。

### 2.4 领域目录必须按职责分组，不能按一个业务名堆放

`domain` 下面的第一层目录必须表达技术职责或 DDD 构件类型，而不是把某个业务能力的所有类直接放进一个目录。

禁止以下组织方式：

```text
domain/
└── agent/
    ├── AgentRunBO.java
    ├── AgentQuotaPort.java
    ├── AgentCitationFactory.java
    ├── AgentFeedbackPolicy.java
    ├── AgentRuntimeLeasePort.java
    ├── AgentCapabilityAuditEvent.java
    └── ...
```

这个目录同时混入了实体/BO、端口、工厂、策略、事件和异常。文件名虽然带有业务前缀，但目录没有表达职责，随着业务增长后无法扫描和维护。

统一采用“职责优先，业务概念其次”的结构：

```text
domain/
├── entity/
│   ├── agent/
│   ├── conversation/
│   ├── execution/
│   └── market-information/
├── value-object/
│   ├── citation/
│   ├── execution-context/
│   └── runtime-lease/
├── service/
│   ├── agent/
│   ├── conversation/
│   └── execution/
├── repository/
│   ├── agent/
│   └── execution/
├── gateway/
│   ├── quota/
│   ├── rate-limit/
│   └── runtime/
├── handler/
│   ├── model-turn/
│   └── tool-call/
├── policy/
│   ├── capability/
│   ├── feedback/
│   └── sensitive-data/
├── factory/
├── event/
│   ├── capability-audit/
│   └── execution/
├── exception/
└── convert/
```

这里的 `agent`、`conversation`、`execution`、`market-information` 只是业务概念示例，不是固定目录。任何业务都必须套用同一规则，例如：

```text
订单领域：domain/entity/order、domain/service/order、domain/repository/order
库存领域：domain/entity/inventory、domain/service/inventory、domain/gateway/warehouse
市场信息：domain/entity/market-information、domain/service/market-information
```

判断一个文件放在哪里，只问两个问题：

1. 它是什么职责：实体、值对象、领域服务、仓储端口、网关端口、策略、事件、异常还是转换器？
2. 它服务于哪个业务概念：订单、库存、市场信息、Agent、会话还是执行？

第一问决定第一层目录，第二问决定第二层目录。不能反过来先创建 `domain/{business}`，再把所有类型塞进去。

每个目录只能承载同一类职责：

| 目录 | 只允许放置 | 不允许混入 |
|---|---|---|
| `entity/{business}` | 聚合根、实体 | Repository、Policy、Event |
| `value-object/{business}` | 值对象、不可变语义类型 | PO、DTO、Service |
| `service/{business}` | 领域服务 | DAO、Mapper、Controller |
| `repository/{business}` | 仓储端口接口 | MyBatis 实现、XML |
| `gateway/{business}` | 外部能力端口接口 | Redis/ES 客户端实现 |
| `handler/{business}` | 策略处理器、分派器 | 持久化 SQL |
| `policy/{business}` | 领域规则策略 | Spring Controller、PO |
| `event/{business}` | 领域事件 | 消息消费者实现 |
| `exception/{business}` | 领域异常 | HTTP 异常响应对象 |
| `convert/{business}` | 领域模型转换器 | SQL 和业务编排 |

当一个业务很小，不需要第二层业务目录时，可以直接放在职责目录中；一旦同一职责下出现两个以上业务概念，就必须建立业务子目录。禁止为了减少目录数量而恢复平铺。

## 3. 新项目目录模板

以下模板是推荐的完整形态；没有定时任务或消息能力时，可以删除对应模块，但不要创建空壳目录。

```text
dev-sphere-{context}/
├── pom.xml
├── {context}-api/
│   ├── pom.xml
│   └── src/main/java/com/cccs7/{context}/api/
│       ├── command/
│       ├── query/
│       ├── dto/
│       └── event/
├── {context}-application/
│   ├── pom.xml
│   ├── {context}-application-controller/
│   │   └── src/main/java/com/cccs7/{context}/application/
│   │       ├── controller/
│   │       ├── dto/
│   │       ├── convert/
│   │       ├── assembler/
│   │       ├── interceptor/
│   │       └── exception/
│   ├── {context}-application-job/
│   │   └── src/main/java/com/cccs7/{context}/application/job/
│   └── {context}-application-mq/
│       └── src/main/java/com/cccs7/{context}/application/mq/
├── {context}-domain/
│   └── src/main/java/com/cccs7/{context}/domain/
│       ├── entity/                  # 聚合根、实体、值对象、BO
│       ├── service/                 # 用例无法归属单一实体时的领域服务
│       ├── repository/              # 仓储端口
│       ├── gateway/                 # 外部能力端口
│       ├── handler/                 # 策略、类型处理器、工厂
│       ├── policy/                  # 领域规则策略
│       ├── factory/                 # 聚合/策略/对象工厂
│       ├── event/                   # 领域事件
│       ├── convert/                 # 领域内部转换
│       └── exception/
├── {context}-infra/
│   ├── src/main/java/com/cccs7/{context}/infra/
│   │   ├── repository/              # 仓储端口的实现
│   │   ├── basic/entity/            # PO/DO/ES Document
│   │   ├── basic/mapper/            # DAO/Mapper 接口
│   │   ├── basic/service/           # 纯基础设施访问服务
│   │   ├── basic/redis/
│   │   ├── basic/es/
│   │   ├── adapter/                 # 第三方或外部系统适配
│   │   ├── config/
│   │   └── convert/
│   └── src/main/resources/
│       ├── mapper/
│       └── application-infra.yml
├── {context}-common/
│   └── src/main/java/com/cccs7/{context}/common/
│       ├── entity/                  # PageInfo 等基础协议
│       ├── enums/                   # 稳定且跨层使用的枚举
│       ├── context/                 # 当前用户、租户等上下文
│       └── util/                    # 无业务副作用的工具
└── {context}-starter/
    ├── src/main/java/com/cccs7/{context}/{Context}Application.java
    ├── src/main/resources/application.yml
    └── src/main/resources-env/{dev,prod}/
        ├── application-{dev,prod}.yml
        ├── bootstrap.yaml
        └── log4j2-spring.xml
```

包名必须和目录一一对应。新项目统一使用：

```text
com.cccs7.{context}.application
com.cccs7.{context}.domain
com.cccs7.{context}.infra
com.cccs7.{context}.common
com.cccs7.{context}.api
```

现有项目同时存在 `com.cccs7.auth.service`、`com.cccs7.circle.service` 与 `com.cccs7.subject.domain.service` 两种风格。新项目统一使用 `domain.service`、`domain.entity`、`domain.convert`，避免按模块出现不同包约定。

## 4. 组件放置规则

### 4.1 Application 层

#### Controller

Controller 只做协议适配：

1. 接收 DTO/Command。
2. 做格式、必填、权限和幂等入口校验。
3. 转换为 application command 或 domain input。
4. 调用一个明确的 application use case。
5. 把结果转换为 DTO，并返回统一响应。

Controller 不得：

- 直接注入 DAO、Mapper、PO 或 Redis/ES 客户端。
- 拼 SQL、处理事务编排、修改多个聚合。
- 把异常全部吞掉后返回无法定位的通用错误。
- 把领域对象或持久化对象直接作为接口返回值。

推荐的控制器调用形态：

```java
@PostMapping("/subjects")
public Result<SubjectDTO> create(@RequestBody CreateSubjectRequest request) {
    CreateSubjectCommand command = converter.toCommand(request);
    SubjectView view = createSubjectUseCase.execute(command);
    return Result.ok(assembler.toDTO(view));
}
```

#### Job 和 MQ

- 定时任务只负责触发 application use case，不直接访问数据库。
- 消费者只负责反序列化、幂等检查、调用 use case、确认消息或进入重试/死信流程。
- 消息体使用 `api/event` 中的契约，不能使用 PO。
- 重试、幂等、顺序性和失败补偿必须记录在用例设计中。

### 4.2 Domain 层

#### Domain Entity / Aggregate

把必须始终一起满足的规则放进同一个聚合边界。实体状态通过业务方法改变，不鼓励在 application 中直接 set 一组字段。

```java
public class Subject {
    public void publish(Publisher publisher) {
        if (options.isEmpty()) {
            throw new DomainException("题目至少需要一个选项");
        }
        this.status = SubjectStatus.PUBLISHED;
        this.events.add(new SubjectPublishedEvent(id));
    }
}
```

不要把数据库表一对一地机械当作聚合。一个聚合可以对应多张表，一个简单表也可以只由仓储直接读写。

#### Domain Service

领域服务用于：

- 一个业务动作横跨多个实体/聚合。
- 需要调用仓储端口或领域网关。
- 业务规则不适合放入单一实体。
- 需要用策略、工厂、处理器分派复杂类型。

`SubjectInfoDomainServiceImpl` 中“保存题目、按题型选择 handler、保存标签映射、同步搜索和排行”的编排方式体现了本项目的领域服务风格。新项目应进一步把外部系统抽象成 domain port，并将最终一致性处理为领域事件或 outbox，而不是让 domain 直接依赖 ES/Redis 实现。

#### Handler / Factory

当同一个用例按类型出现多套算法时，放在 `domain/handler`：

- `XxxHandler`：单一策略。
- `XxxHandlerFactory`：根据枚举、类型或业务键选择策略。
- `XxxPolicy`：无状态规则或校验策略。

`SubjectTypeHandlerFactory` 是可复用的实现样例。handler 不应包含 SQL；需要持久化时调用 domain 定义的端口。

#### Repository / Gateway

接口属于 domain：

```java
public interface SubjectRepository {
    Subject findById(SubjectId id);
    void save(Subject subject);
}

public interface SubjectSearchGateway {
    Page<SubjectSummary> search(SubjectSearchQuery query);
}
```

接口命名使用业务语言，不要把 `selectByCondition`、`RedisUtil`、`SubjectInfoDao` 直接暴露给领域层。

### 4.3 Infra 层

#### PO/DO/Document

`infra/basic/entity` 只描述存储结构，包括表字段、审计字段、逻辑删除字段、ES 文档字段。它不承载业务不变量，不调用外部服务。

推荐命名：

- `UserPO`：关系数据库行。
- `UserDocument`：搜索文档。
- `UserCacheValue`：缓存结构。

现有代码使用 `AuthUser`、`SubjectInfo` 等不带 `PO` 后缀的生成实体。新项目建议补充后缀，防止和 domain entity 混淆；迁移旧模块时可以保持旧命名以降低改动范围。

#### Mapper/DAO 和 XML

- Mapper 接口与 XML 放在 `infra/basic/mapper` 和 `src/main/resources/mapper`。
- XML namespace 必须与 Mapper 全限定名一致。
- SQL 只做持久化查询和更新，不在 XML 中实现跨聚合业务流程。
- 查询方法使用业务可读的命名，复杂查询参数使用专门 Query 对象，不要无限扩张 PO 作为筛选参数。
- 逻辑删除、租户隔离、乐观锁等数据约束要通过统一拦截器或仓储实现，避免各 XML 手写遗漏。

#### Infra Service

Infra service 是 DAO 的薄封装，负责存储技术细节、结果转换和基础设施异常映射。它不应取代 domain service，也不应放业务规则。

现有 `AuthUserServiceImpl`、`SubjectInfoServiceImpl` 的“调用 DAO 并返回结果”属于这一类。

#### Redis / ES / 外部系统

Redis、ES、对象存储、远程 RPC、消息客户端全部属于 infra：

- `basic/redis`：缓存、分布式锁、计数等。
- `basic/es`：搜索文档、索引、查询客户端。
- `adapter`：第三方 API、对象存储、远程服务。
- `config`：客户端和 Spring Bean 装配。

对 domain 暴露业务端口，例如 `RankingGateway`、`SubjectSearchGateway`，不要暴露 `RedisUtil` 或 ES SDK 类型。

### 4.4 Common、API、Starter

#### Common

适合放：`Result`、分页协议、错误码、稳定枚举、登录/租户上下文、无业务副作用的工具。

不适合放：跨上下文业务 BO、某张表的 PO、某个领域的 service、所有人都能调用的万能 util。

如果一个类型只被一个上下文使用，就留在该上下文；如果只被 application 使用，也不要放 common。

#### API

只有在以下情况使用 `api`：

- 被其他微服务依赖的 RPC/Feign 契约。
- 跨模块使用的 command/query/event/DTO。
- 需要独立版本管理的公开接口。

HTTP Controller 的内部 DTO 可以留在 application-controller。`api` 不能包含 PO、DAO、domain service 实现或 Spring 启动逻辑。

现有 `*-api` 模块多数只有 POM，没有实际类。新项目如果保留该模块，必须放入真实契约；没有契约时可以不创建，避免空模块误导依赖关系。

#### Starter

Starter 只负责：

- `@SpringBootApplication` 启动类。
- 各模块扫描、配置和资源装配。
- 环境配置、Nacos/注册中心/日志等运行时集成。
- 打包和部署入口。

业务模块不能依赖 starter，starter 依赖 application、infra 和必要的外部 starter。

## 5. 数据流转规范

### 5.1 写入流程

```text
HTTP/RPC Request
    ↓
Controller DTO 校验
    ↓ DTO → Command/BO
Application Use Case
    ↓ 调用 domain service / aggregate
Domain 规则校验、状态变化、产生事件
    ↓ 调用 repository/gateway port
Infra Adapter
    ↓ Domain → PO
DAO/Mapper/XML
    ↓
Database transaction commit
    ↓
Outbox / event consumer
    ↓
Redis、ES、消息、第三方系统
    ↓ PO/Domain → View → DTO
Response
```

同一个数据库事务内的多表写入由 application use case 或 domain service 统一控制，使用 `@Transactional(rollbackFor = Exception.class)` 的位置应经过事务边界评审。

跨数据库、ES、Redis、消息和第三方系统的操作默认不是原子事务。不要因为方法上有 `@Transactional` 就认为所有外部操作可回滚。需要可靠同步时采用：

1. 本地事务写业务数据和 outbox 事件。
2. 事件发布器投递消息。
3. 消费者更新 ES/Redis/第三方系统。
4. 失败重试、幂等、死信和人工补偿。

现有 `auth` 注册流程在一个 domain service 中同时写数据库和 Redis，`subject` 创建流程同时写数据库、ES 和 Redis。它们体现了业务编排，但新项目必须补充一致性策略和失败处理。

### 5.2 查询流程

```text
Request DTO
  → Query/BO
  → Application use case
  → Domain query service
  → Repository / Search Gateway
  → PO/Document
  → Domain View
  → Response DTO
```

列表查询可以使用专门的 read model，避免为了查询把所有 PO 字段暴露到 domain。分页对象应统一使用 `PageQuery`/`PageResult`，并在入口校验页码、页大小上限和排序字段白名单。

### 5.3 错误流

- 参数错误：application 层校验，返回可识别的参数错误码。
- 业务不允许：domain 抛出明确的 `DomainException`。
- 数据不存在：由用例决定返回空、业务错误还是 404 语义。
- 基础设施失败：infra 转换为基础设施异常，交由全局异常处理器记录 traceId 并返回统一错误。
- 不要在每个 Controller 重复 `try/catch(Exception)`；使用统一异常处理器，保留原始堆栈和错误上下文。

## 6. 命名和编码规范

### 6.1 类型命名

| 类型 | 命名示例 |
|---|---|
| HTTP 入参 | `CreateSubjectRequest`、`UpdateUserRequest` |
| HTTP 出参 | `SubjectDTO`、`SubjectView` |
| 用例 | `CreateSubjectUseCase`、`CreateSubjectService` |
| 领域对象 | `Subject`、`SubjectBO`、`SubjectId` |
| 仓储端口 | `SubjectRepository` |
| 数据库对象 | `SubjectPO` |
| Mapper | `SubjectMapper` 或 `SubjectDao`，一个上下文内只能选一种 |
| 仓储实现 | `MyBatisSubjectRepository` |
| 外部端口 | `SubjectSearchGateway` |
| 外部实现 | `ElasticsearchSubjectSearchGateway` |
| 领域事件 | `SubjectCreatedEvent` |
| 集成事件 | `SubjectCreatedIntegrationEvent` |

### 6.2 包命名

统一使用 `com.cccs7.{context}.{layer}`。同一上下文内不要混用：

- `domain.service` 与根级 `service`。
- `domain.entity` 与根级 `bo`，除非迁移旧模块。
- `infra.basic.entity` 与 application 下的 entity。
- `convert` 和 `converter` 两套目录名。

### 6.3 目录命名必须使用 kebab-case

所有业务模块、业务能力目录、资源目录和非 Java 包目录统一使用小写 kebab-case：

```text
market-information
runtime-lease
rate-limit
tool-call
sensitive-data
```

禁止以下形式：

```text
marketInformation   # camelCase
MarketInformation   # PascalCase
market_information  # snake_case
marketinformation   # 多个英文单词直接粘连
mi                  # 无明确约定的缩写
```

目录命名要求：

- 多个英文单词必须用 `-` 分隔，例如 `market-information`，不能写成 `marketInformation` 或 `marketinformation`。
- 业务目录使用完整单词，不能用 `ab`、`mi`、`rt` 等无法从目录名直接理解的缩写。
- `api`、`mq`、`es`、`dto`、`bo`、`po` 等已经在本项目模块结构中固定存在的技术术语属于兼容例外；新建业务目录不得仿照这些缩写。
- 同一业务概念在所有层中保持同一拼写：`entity/market-information`、`service/market-information`、`repository/market-information`。
- 模块目录也使用 kebab-case，例如 `dev-sphere-market-information`、`market-information-domain`。

Java 有一个必须遵守的语言限制：`src/main/java` 下的 package 名称不能包含 `-`。因此：

- Maven 模块、项目目录、`resources` 目录和 Java 包外的业务目录使用 `market-information`。
- Java package 必须是合法的 Java 标识符，例如 `com.cccs7.marketinformation.domain`，或使用层级 package `com.cccs7.market.information.domain`。
- 不要为了强行显示 kebab-case 而创建与 package 声明不一致的 Java 源码路径；目录规则不能破坏编译规则。

代码生成器和 AI 生成代码必须先读取这个命名规则，再生成目录。生成前应先输出“文件 → 职责目录”的映射；如果出现 `domain/{business}` 平铺目录、camelCase 目录、单词粘连或无意义缩写，应视为结构校验失败。

### 6.4 事务和状态

- 事务边界围绕一个业务用例，不围绕一个 DAO 方法。
- 领域服务负责业务状态迁移，DAO 只负责落库。
- 逻辑删除、状态码、审计字段要通过统一规范处理，禁止在各 Controller 随意写数字常量。
- 密码、token、隐私字段不得写入普通日志；请求日志要使用脱敏对象。

## 7. 一个新用例的落地顺序

以“创建某业务对象”为例：

1. 画出上下文边界，确认数据归属和事务范围。
2. 在 domain 定义聚合、值对象、不变量、仓储端口和外部网关端口。
3. 在 infra 定义 PO、Mapper、XML、仓储实现和外部适配器。
4. 在 application 定义 command、use case、DTO、assembler 和 Controller。
5. 如果需要异步同步 ES/缓存，定义领域事件和 outbox/消息契约。
6. 在 starter 完成扫描、配置和资源装配。
7. 补齐 domain 单元测试、repository 集成测试、Controller 契约测试和消息幂等测试。
8. 运行依赖检查，确认 domain 没有反向依赖 infra，application 没有直接访问 PO/DAO。

推荐的提交顺序：

```text
1. domain model + port + unit test
2. infra adapter + persistence test
3. application use case + converter
4. controller/job/mq adapter + contract test
5. starter/config + integration verification
```

## 8. 测试分层

| 测试 | 重点 | 依赖 |
|---|---|---|
| Domain unit test | 不变量、状态变化、策略分派、边界条件 | 不启动 Spring，不连 DB |
| Application test | 用例编排、事务触发、权限、幂等 | mock domain port 或使用测试容器 |
| Infra integration test | SQL、Mapper 映射、索引、缓存序列化 | 测试数据库/Redis/ES |
| Controller contract test | DTO 校验、HTTP 状态和响应结构 | MockMvc/WebTestClient |
| MQ test | 反序列化、重复消息、重试和死信 | 测试 broker 或契约 fixture |
| End-to-end test | 关键业务主链路 | 完整运行环境 |

领域规则必须有不依赖数据库的测试；只有 DAO 测试而没有 domain 测试，不能证明 DDD 规则落地。

## 9. AI 和代码生成器的结构约束

AI 或脚手架生成一个业务能力时，必须按照“生成前分析 → 受约束实现 → 生成后验收”的顺序工作，不能收到一句需求后直接堆文件。

### 9.1 生成前分析

生成代码前必须先完成并输出：

1. 现有模块、POM、相似实现和可复用公共组件的分析。
2. 本次任务范围、明确不修改的范围，以及与现有代码的兼容要求。
3. 业务上下文、聚合/子域、核心用例和业务不变量。
4. 请求、领域、持久化、外部系统之间的数据流。
5. 事务边界、幂等策略、缓存/搜索一致性、消息重试和失败补偿方案。
6. 目录树和“类名 → 职责 → 目标目录”映射。
7. 验收标准、测试范围和需要执行的验证命令。

如果现有实现、业务规则或数据结构不足以支持决定，AI 必须列出假设和风险，不能自行编造接口、表字段、配置或第三方行为。

### 9.2 实现中约束

1. 不得直接创建 `domain/{business}`，`domain` 第一层必须是职责目录。
2. 目录树必须使用固定职责目录：`entity`、`value-object`、`service`、`repository`、`gateway`、`handler`、`policy`、`factory`、`event`、`exception`、`convert`。
3. 每个类只能归属一个主要职责，不能因为类名有同一个业务前缀就放在同一个目录。
4. Domain 只承载业务规则；Controller 不写业务编排；Infra 不决定业务流程。
5. DTO、BO、PO、VO、Command、Event 必须明确区分，禁止为了少写转换而跨层直接复用。
6. Domain 不得依赖 Infra、MyBatis、Spring Web、Redis、ES 或第三方 SDK；Application 不得直接调用 DAO/Mapper。
7. 优先复用已有组件；只有在已有组件不满足需求时才新增抽象，禁止创建没有调用方的空接口、空工厂和万能工具类。
8. 一个类过大或同时承担多个变化原因时，必须拆分为实体、策略、Policy、Handler、Port 或 UseCase。
9. 只修改任务范围内的文件，禁止顺手升级依赖、重命名公共接口或重构无关模块。
10. 禁止生成假实现、硬编码返回值、无依据的默认数据或用 TODO 冒充完整功能。
11. 密码、Token、隐私数据不得写入日志；异常不得被静默吞掉。

### 9.3 生成后验收

生成完成后必须逐项检查并报告：

1. 目录职责、kebab-case 命名和 Java package 合法性。
2. Domain/Application/Infra 依赖方向和 DTO/BO/PO 边界。
3. 是否存在重复逻辑、无调用抽象、空实现、硬编码和未处理异常。
4. 数据库变更脚本、旧数据兼容和回滚考虑。
5. 正常流程、边界条件、异常流程、幂等和重复消息测试。
6. 修改文件清单、依赖变化、执行过的命令、真实测试结果和剩余风险。

AI 不得声称“已验证”或“测试通过”，除非确实执行了对应命令并取得结果。

以任意业务能力为例，生成结果必须接近下面的职责映射，而不是把类名按前缀平铺：

| 类别 | 示例 | 目标目录 |
|---|---|---|
| 聚合/实体 | `AgentRun`、`MarketInformation` | `domain/entity/{business}` |
| 值对象 | `ExecutionContext`、`RuntimeLease` | `domain/value-object/{business}` |
| 领域服务 | `AgentExecutionDomainService` | `domain/service/execution` |
| 仓储端口 | `AgentRunRepository` | `domain/repository/execution` |
| 外部能力端口 | `AgentQuotaPort`、`WarehouseGateway` | `domain/gateway/quota`、`domain/gateway/warehouse` |
| 策略/规则 | `FeedbackPolicy`、`SensitiveDataPolicy` | `domain/policy/feedback`、`domain/policy/sensitive-data` |
| 处理器/工厂 | `CitationFactory`、`ModelTurnHandler` | `domain/factory`、`domain/handler/model-turn` |
| 领域事件 | `CapabilityAuditEvent` | `domain/event/capability-audit` |
| 领域异常 | `QuotaExceededException` | `domain/exception/quota` |

这张表中的 Agent 仅用于说明映射方式。订单、库存、内容、市场信息、支付、通知等任何业务都必须遵循同样的“职责优先”规则。

## 10. Code Review 检查清单

### 目录与模块

- [ ] 新代码归属明确的业务上下文。
- [ ] Controller、domain、infra、starter 放置正确。
- [ ] 已说明本次任务范围，没有夹带无关重构或依赖升级。
- [ ] `domain` 第一层是职责目录，没有把多个职责堆进 `domain/{business}`。
- [ ] 同一职责下按业务概念建立第二层目录，目录内没有混放不同职责的类型。
- [ ] 业务目录和模块目录使用小写 kebab-case，没有 camelCase、PascalCase、snake_case 或无意义缩写。
- [ ] 没有为单张表创建无业务意义的独立上下文。
- [ ] `api` 模块中的类型确实是公开契约。

### 依赖与模型

- [ ] domain 没有 import `infra`、MyBatis、Spring Web、Redis、ES SDK。
- [ ] application 没有直接 import PO、DAO、Mapper。
- [ ] Controller 没有返回 PO 或接收 PO。
- [ ] DTO、BO、PO 的转换边界清晰且有显式命名。
- [ ] 跨上下文交互通过 api 契约、RPC 或事件，不共享对方的 PO/BO。
- [ ] 没有为了形式上的 DDD 增加无调用方的空接口、空工厂或万能工具类。

### 业务与数据

- [ ] 业务不变量在聚合或 domain service 中，而不是 Controller/Mapper 中。
- [ ] 事务覆盖完整的业务用例。
- [ ] Redis/ES/消息等外部写入有一致性、重试和幂等策略。
- [ ] SQL 没有承担跨聚合业务编排。
- [ ] 分页、排序、租户和逻辑删除规则统一。
- [ ] 没有硬编码返回值、假实现或未完成的 TODO。

### 可运维性

- [ ] 异常由统一处理器收敛，日志包含上下文和 traceId。
- [ ] 敏感字段已脱敏。
- [ ] 关键事件、重试、补偿和死信可观测。
- [ ] 新增配置按环境放置，敏感配置不提交仓库。
- [ ] 测试覆盖正常、边界、失败、幂等和重复消息场景。
- [ ] AI 报告的测试结果与实际执行结果一致。

## 11. DevSphere 现状与新项目取舍

| 现状 | 新项目规则 |
|---|---|
| `auth`、`subject`、`practice`、`circle` 使用业务模块独立 Maven 聚合 | 保留这种上下文拆分方式 |
| application-controller 直接依赖 domain、infra、common | application 只依赖 domain/api/common；infra 通过端口接入 |
| domain POM 直接依赖 infra | 新项目改为 domain 定义 repository/gateway port |
| domain service 中直接操作 infra PO、Redis、ES | 新项目使用聚合和 port；外部同步用事件/outbox |
| `basic/entity`、`basic/mapper`、`basic/service` 由生成代码批量创建 | 可以继续生成基础设施代码，但生成代码不能替代领域建模 |
| DTO → BO、BO → PO 使用 MapStruct converter | 保留显式转换，并增加返回方向转换 |
| `Result`、`PageResult` 在 common | 保留稳定通用协议，避免把业务模型放进去 |
| `api` 模块多数为空 | 有真实契约才创建或保留，不创建空壳依赖 |
| Controller 逐个捕获异常 | 新项目使用全局异常处理器 |
| 包名有根级 `service/bo` 和 `domain/*` 两种风格 | 新项目统一 `domain.service/entity/convert` |

## 12. 最小可执行验收标准

一个新业务模块只有满足以下条件，才算完成第一版 DDD 落地：

1. 有明确的限界上下文说明、聚合边界和主要用例列表。
2. 代码按 `application/domain/infra/starter` 分层，包名和目录一致。
3. Controller 不直接访问 DAO/PO，domain 不依赖 infra 实现。
4. 至少一个核心用例具备从 Request 到持久化再到 Response 的完整链路。
5. 领域不变量有单元测试，持久化映射有集成测试。
6. 多表写入、缓存/搜索同步、消息重试和幂等策略有明确说明。
7. `api`、`common` 没有变成业务代码的逃生舱。
8. Maven 依赖、数据库脚本、环境配置和启动方式可以由新成员独立运行。

## 13. 参考实现位置

以下代码是本指南的现状依据：

- `dev-sphere-auth/.../application/controller/UserController.java`：DTO 校验、DTO → BO、调用领域服务。
- `dev-sphere-auth/.../domain/service/impl/AuthUserDomainServiceImpl.java`：注册用例的事务和跨资源编排。
- `dev-sphere-auth/.../infra/basic/service/impl/AuthUserServiceImpl.java`：infra service 到 DAO 的薄封装。
- `dev-sphere-auth/.../infra/basic/mapper/AuthUserDao.java` 与 `src/main/resources/mapper/AuthUserDao.xml`：DAO 和 SQL 映射。
- `dev-sphere-subject/.../domain/service/impl/SubjectInfoDomainServiceImpl.java`：类型 handler、标签映射、ES、Redis 的组合用例。
- `dev-sphere-subject/.../domain/handler/subject/SubjectTypeHandlerFactory.java`：按业务类型分派策略。
- `dev-sphere-subject/.../application/controller/SubjectController.java`：完整的 DTO → BO → domain → DTO 查询/写入路径。

这份指南描述的是 DevSphere 已有实践的抽象总结，并对新项目增加了依赖倒置、模型隔离、外部一致性和测试门禁要求。旧模块不需要一次性重构；新功能按新规则逐步收敛即可。
