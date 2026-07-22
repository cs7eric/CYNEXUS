# Cynexus Agent Security Audit

审计范围：`cynexus-ai` 的 domain、application、infra，以及对应测试。未修改 UI、API 或数据库 schema。

## 已核验并补强

- Prompt injection boundary：`AutonomousAgentAppService` 的 system prompt 明确将用户输入和工具结果视为不可信数据；`AgentToolRegistry` 只允许已注册、只读且不需要确认的工具。
- Schema fuzzing：工具参数现在校验 JSON object、required、类型、枚举、数值边界、字符串长度、日期格式、数组项、嵌套深度、显式允许的额外字段和总 payload 大小。非法参数不会进入 handler。
- Secret scanning/redaction：文本边界补充 Bearer、`apiKey`、`secret`、OpenAI/GitHub/AWS/JWT 常见凭据，以及 email/mobile 脱敏；JSON 字段脱敏规则保持不变。
- Source/SSRF display boundary：tool 返回的 source URL 和 citations 在落库前只允许 app-relative URL；ClickHouse endpoint 只允许无凭据、无 query/fragment 的 HTTP(S) origin。
- ClickHouse credential transport：数据库密码不再放入请求 URI 或 query string，改用 `X-ClickHouse-Key` header；请求增加 10 秒超时。
- Owner isolation：message/run-step 查询与 run 更新使用 owner scope；对应 persistence contract tests 已覆盖。

## 测试证据

```text
mvn -pl cynexus-ai/cynexus-ai-infra -am \
  -Dtest=AgentToolRegistryTest,AgentMessageBasicServiceImplSecurityTest,
  AgentRunBasicServiceImplSecurityTest,ClickHouseDailyBarsAgentToolHandlerSecurityTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
13 tests, 0 failures, 0 errors

mvn -pl cynexus-ai/cynexus-ai-application/cynexus-ai-application-controller -am \
  -Dtest=AgentSensitiveDataPolicyTest,AgentSourceUrlPolicyTest,
  AgentIdentityResolverTest,AutonomousAgentControllerAuthorizationTest,
  AutonomousAgentWorkflowTest,AgentFeedbackAppServiceTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
20 tests, 0 failures, 0 errors
```

## 仍未覆盖或需要后续决策

- Trusted identity HMAC 当前没有 timestamp/nonce，因此跨请求 replay 保护仍依赖 gateway；需要 API/common-security 合约变更才能补齐。
- 生产 prompt-injection 评测仍是 mock/provider-directed dataset；尚未接入持续的真实模型 adversarial corpus 和人工标注集。
- Retention 的 FK/check/index 完整契约以及历史数据迁移需要与数据库 migration 变更一起验收，本次未改 schema。
- Secret redaction 是模式和字段名策略，不等同于密钥管理；provider secret 的加密存储、轮换、日志扫描仍需部署层证据。
- Rate abuse、Redis lease、metrics 和跨实例并发已有 focused tests，但尚未完成高并发压力测试与 Release 多实例证据。
- 本次 ClickHouse endpoint 校验针对 Agent 的配置型 HTTP client；其他外部 Feign/Provider endpoint 的网络出口策略未在本审计范围内完成验证。
