# Market Pulse Python -> Java 迁移复盘

这份文档记录 `market-pulse` 页面 API 从 Python 迁到 Java 时踩过的坑、被纠正的点、以及后续迁移需要守住的规则。

## 1. 这次迁移做了什么

- 把 `market-pulse` 页面相关 API 从 Python 迁到 Java。
- Java 侧仍然按项目分层：
  `Controller -> AppService -> Domain Repository -> Infra RepositoryImpl -> BasicService -> MyBatis XML -> DB`
- 前端保持页面级 API 文件，不再把接口散到全局 `api.ts`。
- 本地运行 Java 应用，Docker 只保留基础设施。

## 2. 我踩过的坑

### 2.1 直接在 Java 里写 SQL

一开始把查询逻辑直接塞进 Java，违反了项目既有规范。

问题：
- 维护成本高
- 违背 MyBatis XML 分层
- 后续排查和迁移都很痛

修正：
- SQL 回到 `MyBatis XML`
- Java 只做编排和组装

### 2.2 返回 `Map` / `JsonNode`

最开始的返回值是动态结构，虽然快，但后面维护会崩。

问题：
- 前后端契约不稳定
- IDE 没法补全
- 改字段时难排查

修正：
- 改成显式 `DTO`
- Controller 返回 `Result<XXXDTO>`
- JSONB 只在 AppService 内部做一次短暂解析，不外泄到接口层

### 2.3 `Result` 里放 `JsonNode`

这个本质上还是动态对象，只是换了个壳。

修正：
- 直接定义具体 DTO
- 需要兼容旧字段时，用 `@JsonProperty` / `@JsonIgnoreProperties`

### 2.4 DTO 继承踩坑

连板梯队里 `LeaderDTO`、`BrokenStockDTO` 之前用了继承，JSON 解析和 MapStruct 组装都不稳。

问题：
- 继承字段在序列化/反序列化里更容易出边角问题
- `streak`、`previousStreak`、`latestPrice` 这种字段容易丢

修正：
- 改成显式字段 DTO
- 解析时统一归一化

### 2.5 JSR310 日期解析没配

内部 `ObjectMapper` 解析 `LocalDate` 的 JSON 时失败，导致连板整块回退为空。

修正：
- 给内部 `ObjectMapper` 注册 `JavaTimeModule`
- 同时关闭 `FAIL_ON_UNKNOWN_PROPERTIES`

### 2.6 资金潮汐历史窗口取反了

历史接口最开始是按升序 `limit`，拿到的是窗口里最老的一段，结果图表只停在 5 月底。

修正：
- 先按日期倒序取最新 N 条
- 再在服务层排序成升序返回给前端

### 2.7 涨跌家数和涨跌停口径混了

概况卡和历史图最初直接读 PG 旧值，和实际行情事实不一致。

修正：
- 涨跌家数改用 ClickHouse 日线事实重新覆盖
- 涨跌停数按 `market_limit_daily_snapshots` 口径回填

### 2.8 连板梯队只有 count，没有 stocks

前端点击 `x板` 以后，要渲染 `distribution[].stocks`。
PG 里只存了 `height/count`，没存每个梯队的股票列表。

修正：
- Java 在服务层把 `limitUp.stocks` 按 `streak` 分组，回填到 `distribution[].stocks`
- 这样前端不用猜，也不用再请求第二次

### 2.9 连板晋级率没数据

PG 里的 `promotion.levels` 是空的，前端直接显示空态。

修正：
- 在 Java 服务层根据：
  - 今日涨停股的 `streak`
  - 断板股的 `previousStreak`
  现场补算 `levels` 和 `overallRate`

## 3. 你纠正我的地方

- 不能把 SQL 写进 Java。
- 不能用 `Map` 充当长期接口返回。
- 不能在 `Result` 里放 `JsonNode`。
- 不能把前端 API 都堆到全局 `api.ts`。
- 不能忽略项目已有的 PO / BO / DTO / Converter 体系。
- 不能为了快牺牲分层，否则后面维护会更慢。
- 不能只修显示，不修数据源口径。

这些纠正都对，后面同类迁移应该直接照这个标准做。

## 4. 最终怎么改的

- Controller 只暴露强类型接口。
- AppService 负责：
  - 读取 PG / ClickHouse
  - 解析 persisted JSON
  - 归一化字段
  - 补算晋级率、梯队列表
- Infra 层保留：
  - MyBatis XML
  - BasicService
  - RepositoryImpl
  - ClickHouse client
- 前端页面 API 保持在页面目录下。
- 本地启动 Java 服务，Docker 不跑应用容器。

## 5. 这次迁移后要记住的规则

1. 接口契约先定 DTO，再做实现。
2. SQL 只在 XML。
3. JSONB 只能做内部过渡，不要直接泄到接口层。
4. 需要 UI 展示的字段，必须保证“有值、稳定、可解释”。
5. 复杂派生字段优先在服务层补算，不要丢给前端猜。
6. 本地验证要同时看：
   - Java 直接接口
   - Gateway 转发接口
   - 前端页面实际展示

## 6. 这次最终验证过的结果

- `market-overview-akshare/history` 现在能到最新交易日。
- 概况卡的上涨/下跌数已改成事实口径。
- 连板梯队能展开对应股票列表。
- 连板晋级率已补出。
- 通过 Gateway 和本地 Market 都验证过。

