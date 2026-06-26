# Cynexus Layer and Object Convention

## 1. Background

Cynexus follows the layered Maven module style inspired by the previous `devs-phere` project.

The old project uses a structure similar to:

```text
dev-sphere-{service}
├── dev-sphere-{service}-api
├── dev-sphere-{service}-application
├── dev-sphere-{service}-common
├── dev-sphere-{service}-domain
├── dev-sphere-{service}-infra
└── dev-sphere-{service}-starter
```

Cynexus should follow the same idea, but with stricter class placement rules.

The purpose of this document is to prevent Codex or other AI coding tools from placing `Controller`, `DTO`, `BO`, `PO`, `Mapper`, `Converter`, `Result`, `Exception`, `Context`, and `Config` classes in random folders.

---

# 2. Global Module Structure

Initial Cynexus modules:

```text
cynexus
├── cynexus-common
├── cynexus-ai
└── cynexus-market
```

Future modules can be added later:

```text
cynexus-document
cynexus-strategy
cynexus-oss
cynexus-admin
cynexus-gateway
cynexus-iam
```

Do not create future modules in phase 1 unless explicitly requested.

---

# 3. Common Module Structure

The previous project had service-level common modules such as `dev-sphere-subject-common`, which contained packages like:

```text
context
entity
enums
util
```

Cynexus should split common responsibilities more clearly.

Recommended common structure:

```text
cynexus-common
├── cynexus-common-core
├── cynexus-common-web
├── cynexus-common-security
├── cynexus-common-db
├── cynexus-common-cloud
├── cynexus-common-redis
└── cynexus-common-observability
```

Phase 1 required modules:

```text
cynexus-common-core
cynexus-common-web
cynexus-common-security
cynexus-common-db
cynexus-common-cloud
```

Optional later:

```text
cynexus-common-redis
cynexus-common-observability
```

---

# 4. Common Core Convention

## 4.1 Location

```text
cynexus-common-core
└── src/main/java/com/cynexus/common/core
    ├── result
    ├── exception
    ├── enums
    ├── constant
    ├── context
    ├── util
    ├── id
    └── page
```

`common-core` must not strongly depend on Spring MVC, MyBatis, Redis, Nacos, Feign, or Spring AI.

It can depend on lightweight utilities, validation annotations if needed, and Jackson annotations only when necessary.

---

## 4.2 Result Objects

Location:

```text
cynexus-common-core
└── com.cynexus.common.core.result
```

Classes:

```text
Result.java
PageResult.java
ResultCode.java
ResultStatus.java
```

Recommended structure:

```java
package com.cynexus.common.core.result;

import java.io.Serializable;

public class Result<T> implements Serializable {

    private String code;
    private String message;
    private T data;
    private boolean success;
    private Long timestamp;

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.code = ResultCode.SUCCESS.getCode();
        result.message = ResultCode.SUCCESS.getMessage();
        result.data = data;
        result.success = true;
        result.timestamp = System.currentTimeMillis();
        return result;
    }

    public static <T> Result<T> fail(String code, String message) {
        Result<T> result = new Result<>();
        result.code = code;
        result.message = message;
        result.success = false;
        result.timestamp = System.currentTimeMillis();
        return result;
    }

    public static <T> Result<T> fail(ResultCode resultCode) {
        return fail(resultCode.getCode(), resultCode.getMessage());
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isFailed() {
        return !success;
    }

    // getters and setters
}
```

Recommended `ResultCode`:

```java
package com.cynexus.common.core.result;

public enum ResultCode {

    SUCCESS("0", "success"),

    BAD_REQUEST("400", "bad request"),
    UNAUTHORIZED("401", "unauthorized"),
    FORBIDDEN("403", "forbidden"),
    NOT_FOUND("404", "not found"),

    VALIDATION_ERROR("100001", "validation error"),
    BUSINESS_ERROR("100002", "business error"),
    SYSTEM_ERROR("100003", "system error"),

    REMOTE_SERVICE_ERROR("200001", "remote service error"),
    AI_MODEL_ERROR("300001", "ai model error"),
    AI_TOOL_ERROR("300002", "ai tool error"),
    AI_OUTPUT_PARSE_ERROR("300003", "ai output parse error");

    private final String code;
    private final String message;

    ResultCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
```

Rules:

```text
Result is the unified API response wrapper.
Controller should return Result<Response> or Response depending on global ResponseBodyAdvice strategy.
Result must not be placed inside service api modules.
Result must not be duplicated in each service.
Result must not contain business logic.
Result must not contain HTTP framework-specific logic.
```

Forbidden:

```text
cynexus-ai-api/result/Result.java
cynexus-market-common/result/Result.java
cynexus-ai-application/controller/Result.java
```

Correct:

```text
cynexus-common-core/com.cynexus.common.core.result.Result
```

---

## 4.3 PageResult

Location:

```text
cynexus-common-core
└── com.cynexus.common.core.result.PageResult
```

Recommended structure:

```java
package com.cynexus.common.core.result;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

public class PageResult<T> implements Serializable {

    private List<T> records = Collections.emptyList();
    private long total;
    private long pageNo;
    private long pageSize;

    public static <T> PageResult<T> of(List<T> records, long total, long pageNo, long pageSize) {
        PageResult<T> result = new PageResult<>();
        result.records = records;
        result.total = total;
        result.pageNo = pageNo;
        result.pageSize = pageSize;
        return result;
    }

    // getters and setters
}
```

Rules:

```text
PageResult is only for API response pagination.
Do not use MyBatis-Plus Page directly in Controller response.
Do not expose Page<PO> or IPage<PO>.
Application Assembler should convert Page<BO> or IPage<PO> into PageResult<Response>.
```

---

## 4.4 Exception Objects

Location:

```text
cynexus-common-core
└── com.cynexus.common.core.exception
```

Classes:

```text
BizException.java
SystemException.java
RemoteServiceException.java
AiException.java
ErrorCode.java
```

Recommended:

```java
package com.cynexus.common.core.exception;

public class BizException extends RuntimeException {

    private final String code;

    public BizException(String message) {
        super(message);
        this.code = "100002";
    }

    public BizException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
```

Rules:

```text
BizException is for expected business errors.
SystemException is for unexpected system errors.
RemoteServiceException is for Feign or external service failure.
AiException is for AI model/tool/output failure.
Do not throw raw RuntimeException for known business errors.
Do not define duplicate BizException inside each service.
```

---

## 4.5 Common Enums

Location:

```text
cynexus-common-core
└── com.cynexus.common.core.enums
```

Classes:

```text
BaseEnum.java
EnabledStatus.java
DeletedStatus.java
YesNoEnum.java
```

Recommended:

```java
package com.cynexus.common.core.enums;

public interface BaseEnum<T> {

    T getCode();

    String getDesc();
}
```

```java
package com.cynexus.common.core.enums;

public enum EnabledStatus implements BaseEnum<Integer> {

    DISABLED(0, "disabled"),
    ENABLED(1, "enabled");

    private final Integer code;
    private final String desc;

    EnabledStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    @Override
    public Integer getCode() {
        return code;
    }

    @Override
    public String getDesc() {
        return desc;
    }
}
```

Rules:

```text
Global enums go to common-core/enums.
Service-specific enums go to {service}-common/enums or {service}-api/enums.
API-visible enums should be placed in api/enums.
Internal-only enums should be placed in service common or domain.
```

---

## 4.6 Common Constants

Location:

```text
cynexus-common-core
└── com.cynexus.common.core.constant
```

Classes:

```text
CommonConstants.java
OperatorConstants.java
HeaderConstants.java
TraceConstants.java
DateTimeConstants.java
```

Recommended:

```java
package com.cynexus.common.core.constant;

public final class OperatorConstants {

    private OperatorConstants() {
    }

    public static final String SINGLE_USER_ID = "self";
    public static final String SINGLE_USER_NAME = "Cyrus";
}
```

Rules:

```text
Only truly global constants go to common-core/constant.
Service-specific constants go to {service}-common/constant.
Do not create one giant Constants class for everything.
```

---

## 4.7 Context Objects

The old project had `common/context`. Cynexus should keep this idea but separate global context and service context.

Global context location:

```text
cynexus-common-core
└── com.cynexus.common.core.context
```

Classes:

```text
RequestContext.java
RequestContextHolder.java
OperatorContext.java
```

Recommended:

```java
package com.cynexus.common.core.context;

public class RequestContext {

    private String traceId;
    private String operatorId;
    private String operatorName;
    private String requestSource;

    // getters and setters
}
```

Rules:

```text
RequestContext stores traceId, operatorId, operatorName, request source.
RequestContext must not depend on HttpServletRequest directly.
HTTP extraction belongs to common-web filter/interceptor.
Business services can read RequestContextHolder.
Do not pass HttpServletRequest into domain/application services.
```

---

## 4.8 Utility Classes

Location:

```text
cynexus-common-core
└── com.cynexus.common.core.util
```

Classes:

```text
JsonUtils.java
DateTimeUtils.java
StringUtils.java
CollectionUtils.java
DigestUtils.java
```

Rules:

```text
Only generic pure utility methods go to common-core/util.
Do not put business calculation in util.
Do not put market indicator calculation in common util.
Do not put AI prompt logic in common util.
```

---

## 4.9 ID Generator

Location:

```text
cynexus-common-core
└── com.cynexus.common.core.id
```

Classes:

```text
IdGenerator.java
SnowflakeIdGenerator.java
PublicIdGenerator.java
```

Rules:

```text
Internal primary key should be BIGINT.
Use distributed ID generator for id.
Use public_id only for externally exposed resources.
Do not use UUID as default primary key.
```

---

# 5. Common Web Convention

Location:

```text
cynexus-common-web
└── src/main/java/com/cynexus/common/web
    ├── advice
    ├── config
    ├── filter
    ├── interceptor
    └── validation
```

## 5.1 Global Exception Handler

Location:

```text
cynexus-common-web
└── com.cynexus.common.web.advice.GlobalExceptionHandler
```

Rules:

```text
GlobalExceptionHandler converts exceptions to Result.
BizException -> Result.fail(code, message)
Validation exception -> Result.fail(VALIDATION_ERROR)
RemoteServiceException -> Result.fail(REMOTE_SERVICE_ERROR)
Unexpected exception -> Result.fail(SYSTEM_ERROR)
```

Controller should not manually catch every exception.

---

## 5.2 Response Body Advice

Optional location:

```text
cynexus-common-web
└── com.cynexus.common.web.advice.GlobalResponseAdvice
```

Rules:

```text
If enabled, Controller can return Response directly and advice wraps it as Result.
If disabled, Controller should explicitly return Result<Response>.
Do not mix both styles randomly.
Choose one style per project.
```

Recommended for Cynexus phase 1:

```text
Use explicit Result<Response> in Controller.
Do not enable automatic wrapping until project stabilizes.
```

---

## 5.3 Trace Filter

Location:

```text
cynexus-common-web
└── com.cynexus.common.web.filter.TraceIdFilter
```

Rules:

```text
Read X-Trace-Id from request header.
Generate traceId if absent.
Store traceId in RequestContextHolder.
Write traceId into response header.
MDC should include traceId.
```

---

# 6. Common Security Convention

Location:

```text
cynexus-common-security
└── src/main/java/com/cynexus/common/security
    ├── config
    ├── context
    ├── filter
    └── mode
```

Phase 1 is single-user mode.

Classes:

```text
CynexusSecurityProperties.java
AdminTokenAuthFilter.java
CurrentOperator.java
SecurityMode.java
```

Rules:

```text
Do not create user-service in phase 1.
Do not create auth-service in phase 1.
Do not create RBAC in phase 1.
Use single-user mode.
Keep created_by and updated_by as self.
```

---

# 7. Common DB Convention

Location:

```text
cynexus-common-db
└── src/main/java/com/cynexus/common/db
    ├── config
    ├── entity
    ├── handler
    ├── mapper
    └── typehandler
```

## 7.1 BasePO

Location:

```text
cynexus-common-db
└── com.cynexus.common.db.entity.BasePO
```

Recommended fields:

```java
package com.cynexus.common.db.entity;

import java.time.LocalDateTime;

public class BasePO {

    private Long id;
    private String createdBy;
    private String updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;

    // getters and setters
}
```

Rules:

```text
PO can extend BasePO.
BO must not extend BasePO.
DTO must not extend BasePO.
Response must not extend BasePO.
BasePO belongs to common-db, not common-core.
```

---

## 7.2 MyBatis-Plus Auto Fill

Location:

```text
cynexus-common-db
└── com.cynexus.common.db.handler.MetaObjectFillHandler
```

Rules:

```text
Automatically fill id, created_by, updated_by, created_at, updated_at when possible.
operator should be read from CurrentOperator or RequestContext.
Phase 1 operator is self.
```

---

## 7.3 BaseMapperX

Location:

```text
cynexus-common-db
└── com.cynexus.common.db.mapper.BaseMapperX
```

Rules:

```text
Optional extension over MyBatis-Plus BaseMapper.
Do not put business queries in BaseMapperX.
Business SQL belongs to concrete Mapper XML or Mapper interface.
```

---

# 8. Common Cloud Convention

Location:

```text
cynexus-common-cloud
└── src/main/java/com/cynexus/common/cloud
    ├── config
    ├── feign
    └── loadbalancer
```

Classes:

```text
FeignConfig.java
FeignRequestInterceptor.java
FeignErrorDecoder.java
LoadBalancerConfig.java
```

Rules:

```text
Nacos Discovery and Nacos Config dependencies belong to common-cloud.
OpenFeign dependencies belong to common-cloud.
Common Feign interceptors belong to common-cloud.
Service-specific FeignClient interfaces belong to provider service api module.
```

---

# 9. Service Module Structure

Every business service must follow:

```text
cynexus-{service}
├── cynexus-{service}-api
├── cynexus-{service}-application
│   ├── cynexus-{service}-application-controller
│   ├── cynexus-{service}-application-job
│   └── cynexus-{service}-application-mq
├── cynexus-{service}-common
├── cynexus-{service}-domain
├── cynexus-{service}-infra
└── cynexus-{service}-starter
```

Phase 1 can skip `application-job` and `application-mq` unless needed.

---

# 10. API Module Convention

Location:

```text
cynexus-{service}-api
└── src/main/java/com/cynexus/{service}/api
    ├── dto
    ├── request
    ├── response
    ├── enums
    ├── feign
    └── constant
```

Allowed:

```text
DTO
Request
Response
FeignClient
API-visible enum
API-visible constant
```

Forbidden:

```text
Controller
ServiceImpl
Mapper
PO
BO
Repository
RepositoryImpl
Spring AI Tool
Application class
```

Rules:

```text
api is only cross-service contract.
api must not contain business implementation.
api must not contain database objects.
api must not contain domain objects.
```

---

# 11. Service Common Module Convention

The old project had service-level common modules, for example `subject-common/context`, `subject-common/entity`, `subject-common/enums`, `subject-common/util`.

Cynexus service-common should be stricter.

Location:

```text
cynexus-{service}-common
└── src/main/java/com/cynexus/{service}/common
    ├── constant
    ├── enums
    ├── exception
    ├── context
    └── util
```

Allowed:

```text
service-specific constants
service-specific enums
service-specific exception subclasses
service-specific context
small pure util methods only for this service
```

Forbidden:

```text
Controller
Mapper
PO
BO
DTO
Request
Response
DomainService
Repository
Spring AI Adapter
```

Rules:

```text
Global common objects go to cynexus-common.
Service-specific common objects go to cynexus-{service}-common.
Do not duplicate Result in service-common.
Do not duplicate BizException in service-common.
Do not create service-specific DateUtils or JsonUtils unless absolutely necessary.
```

Example:

```text
cynexus-market-common/enums/TradeMarketEnum.java
cynexus-market-common/constant/MarketConstants.java
cynexus-ai-common/enums/AiScenarioTypeEnum.java
cynexus-ai-common/constant/AiConstants.java
```

---

# 12. Application Module Convention

Location:

```text
cynexus-{service}-application
└── cynexus-{service}-application-controller
    └── src/main/java/com/cynexus/{service}/application
        ├── controller
        ├── service
        ├── command
        ├── query
        └── assembler
```

## 12.1 Controller

Location:

```text
application/controller
```

Rules:

```text
Controller receives Request.
Controller validates parameters.
Controller calls AppService.
Controller returns Result<Response>.
Controller must not call Mapper.
Controller must not call Redis.
Controller must not call FeignClient directly.
Controller must not call Spring AI directly.
Controller must not return PO.
Controller must not return BO directly.
```

Correct flow:

```text
Request -> Controller -> AppService -> BO -> Assembler -> Response -> Result
```

Forbidden flow:

```text
Request -> Controller -> Mapper -> PO -> Result
Request -> Controller -> FeignClient -> DTO -> Result
Request -> Controller -> ChatClient -> String -> Result
```

---

## 12.2 Application Service

Location:

```text
application/service
```

Suffix:

```text
AppService
```

Examples:

```text
ScenarioExecutionAppService
MarketOverviewAppService
SectorRankAppService
```

Rules:

```text
ApplicationService orchestrates use cases.
ApplicationService manages transaction boundary.
ApplicationService calls DomainService.
ApplicationService calls Repository interface.
ApplicationService calls Infra Adapter when necessary.
ApplicationService must not use MyBatis Mapper directly.
ApplicationService must not return PO.
ApplicationService must not contain low-level SQL.
```

---

## 12.3 Command and Query

Location:

```text
application/command
application/query
```

Rules:

```text
Command is for write use cases.
Query is for read use cases.
Command/Query can be converted to BO by domain converter.
Command/Query must not contain persistence annotations.
Command/Query must not be used as PO.
```

---

## 12.4 Assembler

Location:

```text
application/assembler
```

Purpose:

```text
BO -> Response
BO -> DTO
Page<BO> -> PageResult<Response>
```

Rules:

```text
Assembler is for API output.
Assembler must not convert PO directly.
Assembler must not call Mapper.
Assembler must not contain business rules.
```

---

# 13. Domain Module Convention

The old project's domain module had:

```text
convert
entity
handler/subject
service
```

Cynexus keeps this style and adds stricter semantics.

Location:

```text
cynexus-{service}-domain
└── src/main/java/com/cynexus/{service}/domain
    ├── entity
    ├── valueobject
    ├── service
    │   └── impl
    ├── repository
    ├── convert
    ├── handler
    ├── event
    └── policy
```

Allowed:

```text
BO
Value Object
DomainService
DomainServiceImpl
Repository interface
Domain Converter
Domain Handler
Domain Event
Domain Policy
Factory
```

Forbidden:

```text
Controller
FeignClient
MyBatis Mapper
PO
RedisTemplate
Spring AI ChatClient
Spring Boot Application
MQ Consumer
Job
```

Rules:

```text
domain must not depend on infra.
domain must not depend on application.
domain must not depend on starter.
domain must not use Mapper.
domain must not use Feign.
domain must not use Redis.
domain must not use Spring AI.
domain can define repository interfaces.
domain can define handlers and factories.
```

---

# 14. Infra Module Convention

The old project infra layer contains technical code such as `basic/entity`, `basic/mapper`, `basic/service`, `config`, and `resources/mapper`.

Cynexus should follow this style.

Location:

```text
cynexus-{service}-infra
└── src/main/java/com/cynexus/{service}/infra
    ├── basic
    │   ├── entity
    │   ├── mapper
    │   ├── service
    │   │   └── impl
    │   ├── redis
    │   ├── es
    │   ├── thread
    │   └── utils
    ├── config
    │   ├── mybatis
    │   ├── redis
    │   ├── log
    │   ├── feign
    │   └── ai
    ├── repository
    ├── convert
    ├── client
    ├── adapter
    └── tool
```

Mapper XML location:

```text
cynexus-{service}-infra
└── src/main/resources/mapper
```

Allowed:

```text
PO
MyBatis Mapper
Mapper XML
MyBatis-Plus BasicService
RepositoryImpl
Redis implementation
ES document/repository
External SDK client
Feign adapter
Spring AI adapter
AI Tool
Technical config
Infra converter
```

Forbidden:

```text
Controller
Application Command
Application Query
Domain BO as database object
Spring Boot Application
```

---

# 15. Starter Module Convention

Location:

```text
cynexus-{service}-starter
└── src/main
    ├── java/com/cynexus/{service}
    │   └── Cynexus{Service}Application.java
    └── resources
        ├── application.yml
        ├── application-local.yml
        └── logback-spring.xml
```

Rules:

```text
starter contains only boot application and configuration files.
starter must not contain Controller.
starter must not contain Service.
starter must not contain Mapper.
starter must not contain PO.
starter must not contain BO.
starter must not contain business logic.
```

---

# 16. Object Naming Rules

## 16.1 DTO

Location:

```text
{service}-api/dto
```

Meaning:

```text
Cross-service data transfer object.
```

Rules:

```text
DTO can be used by FeignClient.
DTO must not contain business methods.
DTO must not contain ORM annotations.
DTO must not contain domain behavior.
```

---

## 16.2 Request

Location:

```text
{service}-api/request
```

Meaning:

```text
External API input object.
```

Rules:

```text
Request can contain validation annotations.
Request must not be used as BO.
Request must not be persisted.
Request must not contain business behavior.
```

---

## 16.3 Response

Location:

```text
{service}-api/response
```

Meaning:

```text
External API output object.
```

Rules:

```text
Response must not be PO.
Response must not be BO.
Response must not contain ORM annotations.
Response should be built by Application Assembler.
```

---

## 16.4 BO

Location:

```text
{service}-domain/entity
```

Meaning:

```text
Business Object.
```

Examples:

```text
AiScenarioBO
AiModelBO
AiExecutionBO
MarketOverviewBO
SectorRankBO
StockDailyFeatureBO
```

Rules:

```text
BO is used by domain services.
BO can contain business-related fields.
BO can contain simple business methods.
BO must not contain ORM annotations.
BO must not be returned directly by Controller.
BO must not be used as database PO.
```

---

## 16.5 VO / Value Object

Location:

```text
{service}-domain/valueobject
```

Examples:

```text
ScenarioCode
ModelCode
ToolCode
StockCode
TradeDate
PublicId
```

Rules:

```text
VO should be immutable where possible.
VO represents concept, not table.
VO must not have ORM annotations.
VO can validate its own value.
Do not use VO to mean frontend View Object.
Use Response for frontend output.
```

---

## 16.6 PO

Location:

```text
{service}-infra/basic/entity
```

Meaning:

```text
Persistent Object, maps to database table.
```

Examples:

```text
AiModelConfigPO
AiScenarioConfigPO
AiPromptTemplatePO
MarketOverviewPO
SectorRankPO
```

Rules:

```text
PO can use MyBatis-Plus annotations.
PO can extend BasePO.
PO must not contain business logic.
PO must not be returned to Controller.
PO must not be exposed through api module.
PO must not be used in domain service public methods.
```

---

## 16.7 Entity

Avoid ambiguous suffix `Entity`.

Use explicit suffix:

```text
BO       domain business object
PO       database persistent object
DTO      cross-service object
Request  API input
Response API output
VO       domain value object
```

Forbidden:

```text
AiModelEntity
MarketEntity
SubjectEntity
```

Correct:

```text
AiModelBO
AiModelConfigPO
AiModelDTO
AiModelResponse
```

---

## 16.8 DO

Do not use `DO` in this project.

Use `BO` or `PO`.

---

# 17. Converter and MapStruct Rules

Use MapStruct for object conversion.

Do not scatter converters randomly.

There are three allowed conversion locations:

```text
domain/convert
infra/convert
application/assembler
```

## 17.1 Domain Converter

Location:

```text
{service}-domain/convert
```

Purpose:

```text
Command -> BO
Request -> BO
BO -> BO
```

Rules:

```text
Domain Converter must not know PO.
Domain Converter must not know Mapper.
Domain Converter must not know external SDK models.
```

---

## 17.2 Infra Converter

Location:

```text
{service}-infra/convert
```

Purpose:

```text
PO <-> BO
ESDocument <-> BO
External API model <-> BO
```

Rules:

```text
Only infra converter can know PO.
Only infra converter can know external SDK model.
Only infra converter can know ES document object.
```

---

## 17.3 Application Assembler

Location:

```text
{service}-application-controller/application/assembler
```

Purpose:

```text
BO -> Response
BO -> DTO
Page<BO> -> PageResult<Response>
```

Rules:

```text
Assembler is for API output.
Assembler must not convert PO.
Assembler must not call Mapper.
Assembler must not contain business rules.
```

---

## 17.4 Naming

Use:

```text
Converter   for domain and infra conversion
Assembler   for application response assembly
Mapper      only for MyBatis
```

Examples:

```text
AiScenarioConverter
AiScenarioPOConverter
AiScenarioAssembler
AiScenarioConfigMapper
```

Do not name MapStruct classes `Mapper`.

---

# 18. ORM and Database Rules

## 18.1 Mapper Location

```text
{service}-infra/basic/mapper
```

Example:

```text
AiModelConfigMapper
AiScenarioConfigMapper
MarketOverviewMapper
```

Rules:

```text
Mapper must not appear in api.
Mapper must not appear in domain.
Mapper must not appear in application.
Mapper must not appear in starter.
```

---

## 18.2 Mapper XML Location

```text
{service}-infra/src/main/resources/mapper
```

Rules:

```text
Complex SQL should go to mapper XML.
Do not put XML under starter.
Do not put XML under application.
```

---

## 18.3 Repository Pattern

Domain defines repository interface:

```text
{service}-domain/repository
```

Infra implements repository:

```text
{service}-infra/repository
```

Example:

```text
domain.repository.AiModelRepository
infra.repository.AiModelRepositoryImpl
```

Rules:

```text
ApplicationService calls repository interface.
RepositoryImpl calls MyBatis Mapper.
Controller must never call repository or mapper directly.
```

---

## 18.4 MyBatis-Plus BasicService

Location:

```text
{service}-infra/basic/service
{service}-infra/basic/service/impl
```

Examples:

```text
AiModelConfigBasicService
AiModelConfigBasicServiceImpl
```

Rules:

```text
BasicService is technical CRUD service.
BasicService is not domain service.
BasicService must not leak outside infra except through RepositoryImpl.
Do not call BasicService from Controller.
```

---

# 19. Feign Rules

FeignClient belongs to provider service api module.

Example:

```text
cynexus-market-api
└── com.cynexus.market.api.feign.MarketFeignClient
```

Consumer service depends on provider api module.

Example:

```text
cynexus-ai-infra depends on cynexus-market-api
```

Rules:

```text
Internal service FeignClient belongs to provider api module.
ApplicationService should not directly depend on FeignClient.
Infra adapter or AI tool can depend on FeignClient.
Third-party external API client belongs to infra/client.
```

---

# 20. AI-Specific Rules

Only `cynexus-ai` can contain Spring AI model adapter code.

Location:

```text
cynexus-ai-infra
└── com.cynexus.ai.infra.ai
    ├── adapter
    ├── registry
    ├── factory
    └── output
```

Allowed classes:

```text
AiModelAdapter
OpenAiModelAdapter
DeepSeekModelAdapter
QwenModelAdapter
ClaudeModelAdapter
AiModelAdapterRegistry
DynamicChatClientFactory
StructuredOutputParser
JsonSchemaValidator
```

AI tools location:

```text
cynexus-ai-infra
└── com.cynexus.ai.infra.tool
```

Examples:

```text
MarketDataTools
DocumentTools
StrategyTools
AiToolRegistry
```

Rules:

```text
Spring AI ChatClient must not appear in controller.
Spring AI ChatClient must not appear in domain.
Spring AI ChatClient must not appear in api.
Tool classes must only exist in cynexus-ai-infra.
AI tool can call FeignClient.
AI tool should return DTO or compressed result object.
```

---

# 21. Logging and Observability Rules

The old project contains a common log module. Cynexus should place logging and tracing more explicitly.

Location:

```text
cynexus-common-observability
└── src/main/java/com/cynexus/common/observability
    ├── log
    ├── trace
    └── metrics
```

Phase 1 can keep lightweight tracing in:

```text
cynexus-common-web/filter/TraceIdFilter
```

Future classes:

```text
LogAspect
TraceIdFilter
MdcUtils
MetricsRecorder
```

Rules:

```text
TraceId belongs to common-web or common-observability.
Logging aspect belongs to common-observability.
AI execution logs belong to cynexus-ai domain/infra, not common.
Business audit logs belong to each service, not common.
```

---

# 22. Class Placement Quick Reference

| Class Type                | Suffix                    | Location                                      |
| ------------------------- | ------------------------- | --------------------------------------------- |
| Unified response          | Result                    | `cynexus-common-core/result`                  |
| Page response             | PageResult                | `cynexus-common-core/result`                  |
| Result code               | ResultCode                | `cynexus-common-core/result`                  |
| Business exception        | BizException              | `cynexus-common-core/exception`               |
| System exception          | SystemException           | `cynexus-common-core/exception`               |
| Global enum               | BaseEnum / EnabledStatus  | `cynexus-common-core/enums`                   |
| Global constants          | Constants                 | `cynexus-common-core/constant`                |
| Request context           | RequestContext            | `cynexus-common-core/context`                 |
| Global utility            | XxxUtils                  | `cynexus-common-core/util`                    |
| Base PO                   | BasePO                    | `cynexus-common-db/entity`                    |
| API input                 | Request                   | `{service}-api/request`                       |
| API output                | Response                  | `{service}-api/response`                      |
| Cross-service object      | DTO                       | `{service}-api/dto`                           |
| Feign client              | FeignClient               | `{service}-api/feign`                         |
| Service enum              | Enum                      | `{service}-common/enums`                      |
| Business object           | BO                        | `{service}-domain/entity`                     |
| Value object              | VO / explicit name        | `{service}-domain/valueobject`                |
| Domain service            | DomainService             | `{service}-domain/service`                    |
| Domain service impl       | DomainServiceImpl         | `{service}-domain/service/impl`               |
| Repository interface      | Repository                | `{service}-domain/repository`                 |
| Domain converter          | Converter                 | `{service}-domain/convert`                    |
| Domain handler            | Handler                   | `{service}-domain/handler`                    |
| Database object           | PO                        | `{service}-infra/basic/entity`                |
| MyBatis mapper            | Mapper                    | `{service}-infra/basic/mapper`                |
| Mapper XML                | XML                       | `{service}-infra/src/main/resources/mapper`   |
| Infra CRUD service        | BasicService              | `{service}-infra/basic/service`               |
| Infra CRUD impl           | BasicServiceImpl          | `{service}-infra/basic/service/impl`          |
| Repository implementation | RepositoryImpl            | `{service}-infra/repository`                  |
| PO/BO converter           | POConverter / Converter   | `{service}-infra/convert`                     |
| Redis implementation      | Cache / RedisService      | `{service}-infra/basic/redis`                 |
| ES document/repository    | ESDocument / ESRepository | `{service}-infra/basic/es`                    |
| Controller                | Controller                | `{service}-application-controller/controller` |
| Use case service          | AppService                | `{service}-application-controller/service`    |
| API assembler             | Assembler                 | `{service}-application-controller/assembler`  |
| MQ consumer               | Consumer                  | `{service}-application-mq`                    |
| Job handler               | Job                       | `{service}-application-job`                   |
| Boot app                  | Application               | `{service}-starter`                           |

---

# 23. Dependency Direction

Allowed direction:

```text
starter
  -> application
  -> infra
  -> domain
  -> common
```

Also:

```text
api is depended on by other services.
infra can depend on other service api modules for Feign.
domain should not depend on api unless explicitly required.
```

Recommended:

```text
domain -> common-core
infra -> domain + common-db + common-cloud + provider-api
application -> api + domain + infra + common-web
starter -> application + infra + common modules
```

Forbidden:

```text
domain -> infra
domain -> application
domain -> starter
api -> infra
api -> application
controller -> mapper
controller -> PO
starter -> business implementation
```

---

# 24. Forbidden Examples

Do not generate:

```text
controller/AiModelConfigMapper.java
domain/AiModelConfigPO.java
api/AiScenarioBO.java
starter/AiExecutionController.java
application/MarketOverviewMapper.java
domain/OpenAiModelAdapter.java
infra/AiExecutionController.java
cynexus-ai-api/Result.java
cynexus-market-common/BizException.java
```

Correct placement:

```text
infra/basic/mapper/AiModelConfigMapper.java
infra/basic/entity/AiModelConfigPO.java
domain/entity/AiScenarioBO.java
application/controller/AiExecutionController.java
infra/ai/adapter/OpenAiModelAdapter.java
common-core/result/Result.java
common-core/exception/BizException.java
```

---

# 25. Codex Mandatory Rules

Before creating or modifying any Java file, check:

```text
1. What layer does this class belong to?
2. Is this class global common, service common, API contract, application use case, domain object, or infra implementation?
3. Does the package match its layer?
4. Does the suffix match its role?
5. Does the dependency direction violate the architecture?
6. Is any PO leaking to controller or api?
7. Is any Mapper being called by controller?
8. Is any Result duplicated outside common-core?
9. Is any BizException duplicated outside common-core?
10. Is any Spring AI code placed outside cynexus-ai-infra?
11. Is any FeignClient placed outside provider api module?
12. Is any business logic placed in starter?
```

If any answer violates this document, do not generate the file.

If existing code violates this document, refactor it instead of adding more code.

Do not invent new package styles.
