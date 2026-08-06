# Findings

- The working tree contains 24 concrete AgentToolHandler classes, including the
  newly added futures AI handler; the explicit definition contract test still
  enumerates 23.
- Market indicator and ETF have bounded domain query services and user-facing
  APIs but no Agent handler.
- Futures position has bounded latest/instruments/members/history/coverage APIs,
  while the existing AI handler only analyzes caller-provided snapshots.
- Margin trading has an industry-history endpoint not represented by the
  existing handler schema or operation switch.
- The legacy AiToolRegistry contains four scenario tool codes and is separate
  from the autonomous AgentToolRegistry. It must not be silently conflated with
  governed autonomous tool registration.
- The completed exposure is read-only and bounded. Admin/configuration,
  refresh, write, and data-pipeline operations remain intentionally excluded
  from autonomous invocation.
- Futures AI keeps its database prompt key for routing compatibility while
  using the dotted Agent capability code market.futures_position_analysis.
