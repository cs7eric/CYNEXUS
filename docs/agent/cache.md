# Redis Cache Rules

Redis is an infrastructure concern. Services must use the shared
`cynexus-common-cache` module instead of importing `StringRedisTemplate`,
calling `opsForValue()` directly, building keys, or parsing cache bundles in
arbitrary packages.

## Ownership

- `cynexus-common-cache` owns Redis client primitives (`get`, `set`,
  `setIfAbsent`, `exists`, `delete`, and script execution), canonical key
  builders, atomic capture-bundle reading, and cache-failure tolerance.
- A service's `infra/cache` package owns feature-specific JSON-to-PO mapping,
  filtering, and durable-store fallback orchestration.
- `domain` and `application` must not depend on Redis, Spring Data Redis, or
  cache key formats.
- A producer may publish an immutable bundle and its latest pointer, but the
  key contract must use `RedisCaptureBundleKeys` semantics:
  `latest-trade-date`, `latest:<tradeDate>`, `capture:<tradeDate>:<captureId>`,
  and `pending:<tradeDate>` under a configured feature prefix.

## Service integration

1. Add `cynexus-common-cache` to the service module that implements the cache
   adapter.
2. Import `CynexusCacheAutoConfiguration` from the service starter (or expose
   the equivalent approved auto-configuration when the service adopts the
   repository-wide auto-configuration mechanism).
3. Keep the feature adapter under `infra/cache`; inject
   `RedisCacheOperations` for ordinary operations or
   `RedisJsonBundleCacheReader` for capture bundles. Use a configured,
   feature-scoped prefix.
4. On a cache miss, malformed bundle, or Redis failure, fall back to the
   service's durable repository. Cache data must never be the only source of
   truth.
5. Keep TTL, publication, and persistence-acknowledgement behavior explicit
   in the producer; readers must not mutate the published bundle.

## Review checks

```text
rg -n "StringRedisTemplate|RedisTemplate|org.springframework.data.redis" \
  <service>/src/main/java/**/infra/cache
```

Feature data-cache code must not access Redis directly; it should use the
shared reader injected into the feature adapter. Redis coordination primitives
(for example leases, rate limits, and locks) are a separate concern and must
remain in a clearly named infrastructure package until a matching common
primitive is introduced. New cache access requires an explicit common-module
extension and an update to this document.
