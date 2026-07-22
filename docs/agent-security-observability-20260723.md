# Agent Security / Observability Verification

Date: 2026-07-23

Scope: Provider and Langfuse outbound endpoint policy, rate-limit abuse/high-concurrency verification, and Prometheus Agent metric registration. UI, task workflow, and dynamic capability behavior are outside this change.

## Implemented Boundary

- Added the domain-owned `AgentOutboundEndpointPolicy`.
- Provider base URLs must use HTTP(S), contain no credentials/query/fragment/control characters, and match an exact configured origin.
- Langfuse ingestion and usage-query URLs use the same policy. Invalid Langfuse configuration is fail-open for the Agent request path: reporting/usage reads are disabled and the request is not failed by observability.
- Configuration is deployment-controlled through `CYNEXUS_AI_PROVIDER_ALLOWED_ORIGINS` and `CYNEXUS_AI_LANGFUSE_ALLOWED_ORIGINS`. The Release defaults contain only the known Provider origins and the internal Langfuse service origin. Development may explicitly add loopback origins.
- Prometheus Agent counters are registered at startup, so the Release endpoint exposes the Agent metric family at zero before the first Agent run. Runtime events still add the status/provider/model/tool labels through the existing observation port.

## Focused Results

| Gate | Result | Evidence |
| --- | --- | --- |
| Outbound endpoint policy | PASS | Domain policy tests: 3/3 |
| Provider adapter regression | PASS | `OpenAiCompatibleChatAdapterAgentTest`: 7/7 |
| Langfuse allowlist/fail-open | PASS | `LangfuseEndpointPolicyTest`: 2/2 |
| Micrometer registration | PASS | `MicrometerAgentObservabilityTest`: 1/1 |
| Redis rate-limit abuse gate | PASS | 32 concurrent workers, 256 calls, one owner, exactly 30 allowed |
| Real Provider adversarial test | NOT RUN | No real Provider request was used for this security gate |

The Redis gate ran against the shared local Redis container with a unique owner key. The Lua rate-limit operation remained atomic under concurrent calls and did not allow more than the configured window quota.

## Release Metric Check

The versioned Release `1.1.184-260723-RELEASE` was rebuilt from the `1.1.184-SNAPSHOT` artifacts and started through Docker. All six application/UI health endpoints returned HTTP 200. The AI Prometheus endpoint exposed the Agent metric families before any Agent run:

```text
deploy/release/start-release.ps1 -Version 1.1.184-260723-RELEASE
GET http://127.0.0.1:18081/actuator/prometheus
```

Observed:

```text
# HELP cynexus_ai_agent_provider_calls_total
# TYPE cynexus_ai_agent_provider_calls_total counter
cynexus_ai_agent_provider_calls_total{model="unknown",outcome="unknown",provider="unknown"} 0.0
# HELP cynexus_ai_agent_runs_started_total
# TYPE cynexus_ai_agent_runs_started_total counter
cynexus_ai_agent_runs_started_total 0.0
# HELP cynexus_ai_agent_tool_calls_total
# TYPE cynexus_ai_agent_tool_calls_total counter
cynexus_ai_agent_tool_calls_total{outcome="unknown",tool="unknown"} 0.0
```

No real Provider success, streaming success, or model/tool execution is claimed by this document.

## Known Limits

- The allowlist prevents configuration to an unapproved origin; it is not a full DNS-rebinding or network-egress firewall. Production network policy should still restrict outbound traffic at the container/cluster boundary.
- The Redis test is opt-in and requires the shared Redis runtime: `-Dcynexus.redis.integration=true`.
- The metric endpoint proves registration and export. A non-zero Provider/tool series requires a separately authorized Agent run and is intentionally not fabricated here.
- Full reactor `mvn test-compile` now passes. The Release runtime image was built with `-Dmaven.test.skip=true`; the full reactor test suite was not run, while focused task-4 tests remain green.
