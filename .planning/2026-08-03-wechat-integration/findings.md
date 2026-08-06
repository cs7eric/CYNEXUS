# Findings: Cynexus WeChat Integration

## Research

- `E:/project-repo/DevSphere/dev-sphere-wx` is a standalone Spring Boot 2 service. It verifies the official-account callback with SHA-1, parses inbound XML, replies to `event.subscribe` and `text`, and uses Redis only for the `验证码` interaction.
- DevSphere hard-codes a WeChat token and Redis credentials; those must become environment/Nacos-backed configuration in Cynexus. Its current callback returns `unknown` for unsupported messages and does not implement outbound customer-service push.
- Cynexus uses a six-part DDD service layout: `api`, `common`, `domain`, `infra`, `application` (with controller submodule), and `starter`; the root `pom.xml` lists each top-level service.
- Cynexus has JDK `HttpClient` adapters in infra and Spring Data Redis available in existing starters. WeChat outbound calls belong in infra behind a domain gateway.
- Release wiring currently enumerates service images explicitly. A new service will need root Maven registration and release/startup/gateway wiring if it is intended to run in the Release stack.
- CodeGraph was available for Cynexus and was synced before impact inspection. DevSphere was not initialized in CodeGraph, so its local `rg` file inspection was used as the fallback required by repository guidance.

## Scope Decision

- Implement an official-account integration slice with callback verification, secure XML parsing, subscribe/text interaction, access-token caching, and customer-service text push to one or many openids.
- Keep subscriber identity persistence out of this first slice: WeChat openid is the stable subscriber handle supplied by the platform, and no Cynexus business aggregate currently owns follower lifecycle. The push API is bounded and operationally safe for an explicitly supplied openid list.
