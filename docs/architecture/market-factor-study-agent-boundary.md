# Market Factor Study Agent Boundary

`market_factor_study_synthesis` and the market factor study Agent tools are read-only. They may read persisted study statistics, evidence references, outcomes, coverage and freshness metadata within the authenticated tenant boundary.

Agent writes are explicitly excluded: creating or changing Factor Sets, strategies, backtests, or watchlists changes durable user research state and must remain an explicit UI operation. No write tool is registered, so there is no preview/confirmation/audit path to bypass.
