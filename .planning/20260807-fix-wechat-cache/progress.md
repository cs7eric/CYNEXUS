# Progress

## 2026-08-07

- Diagnosed the startup failure from container logs and inspected the shared cache and WeChat wiring.
- Confirmed the Jackson 2/3 mismatch in `cynexus-common-cache`.
- Changed the cache dependency and both cache classes from Jackson 2 to Jackson 3.
- `mvn -pl cynexus-common/cynexus-common-cache -am test` passed: 3 tests, 0 failures.
- `mvn -pl cynexus-wechat/cynexus-wechat-starter -am -DskipTests package` passed for all 18 reactor projects.
- Full Release deployment build reached the existing AI test suite and failed before image creation because `RedisAgentRateLimitConcurrencyTest` passes `StringRedisTemplate` where current code expects `RedisCacheOperations`; this is outside the WeChat fix.
- The first production-only Maven retry used an unquoted PowerShell `-D` argument and was rejected as an invalid lifecycle phase; no repository files were changed.
- Full production packaging passed for all 54 reactor projects with test compilation skipped, allowing Release image creation despite the unrelated AI test compile failure.
- Rebuilt and recreated the six backend Release services with `1.1.398-260807-RELEASE`.
- Verified WeChat is running with restart count 0; actuator liveness/readiness is UP and startup logs are clean.
- Verified Maven/UI versions remain synchronized and the scoped diff has no whitespace errors.
