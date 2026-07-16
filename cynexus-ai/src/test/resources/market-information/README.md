# Market Information Evaluation Set

`evaluation-60.json` contains 60 persisted Market Information messages captured from the local fact tables.

- Cases `mi-001` through `mi-020` are the manually reviewable gold partition.
- Cases `mi-021` through `mi-060` are structural and verbatim-evidence regression cases.
- Offline evaluation is the default and performs no provider calls.
- Live evaluation requires `CYNEXUS_MARKET_INFORMATION_LIVE_EVAL=true` and `CYNEXUS_AI_CUSTOM_API_KEY`.
- Live execution must run five smoke cases first, stop after three consecutive provider failures, and only proceed to the full set after the smoke succeeds.
- DeepSeek is fallback-only. Set `CYNEXUS_AI_DEEPSEEK_LIVE_LIMIT=20` in development; repair calls count toward the same limit.

Acceptance thresholds are JSON Schema 100%, verbatim evidence at least 95%, no investment advice 100%, event type at least 85%, and key entities at least 90%.
