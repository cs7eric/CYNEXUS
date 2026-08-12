# Fix WeChat Release startup failure

## Goal

Make `cynexus-release-wechat` start successfully by aligning the shared cache module with Spring Boot 4 / Jackson 3, then rebuild and verify the affected Release service.

## Phases

- [completed] Align shared cache Jackson dependency and imports
- [completed] Run focused tests and Maven compilation
- [completed] Rebuild/recreate the Release service
- [completed] Verify runtime health and repository cleanliness

## Scope guard

- Preserve all pre-existing user changes outside the shared cache fix and deployment artifacts created by this task.
- Do not start Dev or source-tree application runtimes.

## Errors Encountered

| Error | Attempt | Resolution |
|---|---:|---|
| Full Release build failed in pre-existing `cynexus-ai-infra` test compilation: `StringRedisTemplate` cannot be converted to `RedisCacheOperations` in `RedisAgentRateLimitConcurrencyTest` | 1 | Use an alternative production build with Maven test compilation skipped; do not modify unrelated AI test code. |
| PowerShell split `-Dmaven.test.skip=true` into an invalid Maven lifecycle phase | 1 | Quote Maven `-D` arguments on the next attempt. |

## Verification

- WeChat container is `Up`, image `cynexus-wechat:1.1.398-260807-RELEASE`, `RestartCount=0`.
- WeChat actuator health returned `{"groups":["liveness","readiness"],"status":"UP"}`.
- WeChat logs contain `Started CynexusWechatApplication` and no startup failure.
- Root Maven and UI versions both remain `1.1.398-SNAPSHOT`.
- `git diff --check` passed for the shared cache changes.
