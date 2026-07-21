# Cynexus AI Agent Architecture

## 1. Product Goal

Cynexus should accept a natural-language request, understand the user's intent, select the required read capabilities, execute them in a useful order, interpret the returned data, and answer with an executable plan and traceable evidence.

Example request:

> 今天 A 股市场为什么走弱？重点看成交额、行业资金流和北向资金，并给我三个需要继续观察的风险。

Expected behavior:

1. The model identifies the market-review objective and missing date context.
2. It selects the relevant registered capabilities instead of following a hard-coded branch.
3. It calls independent capabilities in parallel where possible.
4. It uses returned data to decide whether another capability is needed.
5. It produces an answer with data dates, sources, assumptions, risks, and a concise executable plan.

The model owns planning and sequencing. The application owns policy, validation, permissions, execution limits, persistence, and error handling.

## 2. Current-State Assessment

The current `cynexus-ai` service has a useful foundation but is not an Agent runtime yet:

- `ScenarioExecutionAppService` accepts `AiExecuteRequest` and routes a scenario to a model.
- `AgentExecutor` calls the model for zero tools, but returns a mock JSON string whenever tool codes are present.
- `AiToolCatalog` only returns a list of strings. It does not expose input schemas, output schemas, permissions, or handlers.
- `MarketDataTools` can call the existing Market Feign APIs, but it is not exposed as model-callable functions.
- `OpenAiCompatibleChatAdapter` supports ordinary chat and JSON output, but does not send or parse native tool calls.
- `workspace_chat` deliberately bypasses tools and sends a plain-text prompt.
- `ai_execution_log` records model executions, but there is no conversation, plan, tool-step, or replay model.
- The UI has a global chat entry, but no Agent run view showing selected capabilities, steps, returned data, or failures.

Therefore the missing capability is the Agent control plane, not another chat component or another model provider.

## 3. Technology Decision

Keep the existing Spring AI and LangChain4j dependencies. Do not add a second orchestration framework for the first implementation.

The runtime will use a provider-neutral tool-call protocol at the domain boundary:

- The domain defines `AgentModelGateway`, `AgentToolCatalog`, `AgentToolExecutor`, and typed Agent run objects.
- The application implements the bounded Agent loop and policy checks.
- Infra adapts the current OpenAI-compatible adapter to native tool calls and keeps provider-specific JSON formats out of domain/application contracts.
- A strict JSON action protocol remains the fallback when a provider does not support native function calling. The fallback is still model-selected, schema-validated, and bounded; it is not a fixed keyword router.
- LangChain4j remains available for provider implementations that need its model/tool abstractions. It is not allowed to leak into API or domain packages.

This preserves the existing dynamic provider routing and makes the first workflow work with the current custom endpoints. Native function calling is preferred; protocol fallback keeps the workflow usable with compatible providers that only return text.

## 4. Runtime Contract

### 4.1 Tool definition

Every callable capability is a Spring bean implementing a domain-neutral contract through an infra/application adapter:

```text
code: market.overview
name: Market overview
module: market
description: Read the latest market breadth, turnover, index and fund-flow overview.
inputSchema: JSON Schema
outputSchema: JSON Schema
readOnly: true
requiresConfirmation: false
freshness: daily snapshot
source: Market API
```

The tool registry is assembled from beans, not a switch statement. Each handler validates its own typed input and delegates through an existing domain port or service gateway.

### 4.2 Agent input

```json
{
  "conversationNo": "optional-stable-id",
  "userInput": "自然语言请求",
  "context": {
    "workspace": "market-sentiment",
    "tradeDate": "2026-07-22",
    "locale": "zh-CN"
  },
  "preferredModelCode": "optional-model"
}
```

The server derives authenticated owner and tenant context. The client cannot grant a tool permission by sending a tool code.

### 4.3 Model turn

The model may return either:

```json
{
  "type": "tool_calls",
  "calls": [
    {
      "callId": "call-1",
      "toolCode": "market.overview",
      "arguments": {"tradeDate": "2026-07-22"}
    }
  ]
}
```

or:

```json
{
  "type": "final",
  "answer": "面向用户的回答",
  "plan": [
    {"step": 1, "toolCode": "market.overview", "purpose": "确认市场整体状态"}
  ],
  "assumptions": [],
  "evidence": [
    {"toolCode": "market.overview", "dataDate": "2026-07-22", "summary": "..."}
  ]
}
```

All model output is parsed and validated before it changes Agent state. Markdown is allowed only in the final answer, never in tool arguments.

### 4.4 Execution limits

- Maximum 8 model turns per run.
- Maximum 12 tool calls per run.
- Maximum 4 parallel read calls in one turn.
- Per-tool timeout and total run timeout.
- Tool arguments validated against the registered JSON Schema.
- Only registered, enabled, read-only tools are available in the first release.
- Unknown tools, malformed arguments, duplicate call IDs, and oversized outputs become explicit step failures.
- A model cannot call a write capability without a future confirmation policy and a user confirmation event.

## 5. Initial Capability Set

The first end-to-end Agent release exposes existing Market capabilities already reachable from `MarketDataGateway`:

| Capability | Inputs | Output | Source |
| --- | --- | --- | --- |
| `market.overview` | `tradeDate?` | breadth, turnover, index/fund-flow overview | Market API |
| `market.overview_history` | `start?`, `end?` | historical market overview rows | Market API |
| `market.sector_rank` | `tradeDate?`, `topN?` | sector ranking and direction | Market API |
| `market.stock_daily_feature` | `symbol`, `tradeDate?` | daily stock factors and snapshot | Market API |
| `market.stock_intraday_feature` | `symbol`, `tradeDate?` | intraday stock factors | Market API |
| `market.stock_chart_klines` | `targetType`, `symbol`, `period`, `adjust` | K-line series | Market API |

The registry metadata will also link each capability to the existing provider/data catalog where one exists. This lets the UI explain freshness, source and data limitations without pretending that every catalog row is already a callable runtime tool.

Subsequent tool packs will add read-only finance, knowledge-base, application-analysis, and market-information tools through service APIs. They must be added through the same registry contract, not by growing a central conditional method.

The first extension now follows that contract:

| Capability | Inputs | Output | Source |
| --- | --- | --- | --- |
| `finance.overview` | `start?`, `end?` | income, expense, net assets, accounts, category and daily summary | Core Finance API |
| `finance.transactions` | `start?`, `end?`, bounded `limit?` | ledger transaction rows | Core Finance API |
| `knowledge.document` | `documentId` | approved stored document and metadata | Knowledge Base API |
| `market.clickhouse_daily_bars` | `market?`, `symbol`, `start?`, `end?`, bounded `limit?` | historical OHLCV bars | ClickHouse `daily_raw` |

ClickHouse access is a typed, allow-listed query template. The Agent does not receive arbitrary SQL or a database credential, and unavailable downstream services produce an explicit failed tool step.

## 6. Agent Loop

```text
HTTP request
  -> AgentApplicationService creates run
  -> AgentPolicy validates request and builds tool snapshot
  -> AgentModelGateway asks model to plan or call tools
  -> AgentTurnParser validates model output
  -> AgentToolExecutor validates and runs selected tools
  -> step results are persisted and summarized back to the model
  -> repeat until final / limit / failure
  -> final answer + plan + evidence returned and persisted
```

Independent calls are executed with `CompletableFuture` and a bounded executor. The model decides which calls exist; the application only decides that safe independent calls may run concurrently.

The application must never expose hidden chain-of-thought. The UI receives a short purpose and tool result summary, not private reasoning tokens.

## 7. Persistence Design

All tables belong to `cynexus_appl_ai` and follow the existing BIGINT, soft-delete, JSONB, timestamp and index conventions.

### `ai_agent_conversation`

Stores a reusable user conversation shell: `id`, `conversation_no`, `owner_key`, `title`, `status`, `last_message_at`, `extra`, timestamps, `deleted_at`.

### `ai_agent_run`

Stores one user request execution: `id`, `run_no`, `conversation_id`, `owner_key`, `user_input`, `context_json`, `model_code`, `status`, `turn_count`, `tool_call_count`, `plan_json`, `answer`, `assumptions_json`, `evidence_json`, `error_message`, started/finished timestamps, `extra`, timestamps, `deleted_at`.

### `ai_agent_run_step`

Stores an auditable model/tool step: `id`, `run_id`, `step_no`, `turn_no`, `step_type`, `call_id`, `tool_code`, `purpose`, `input_json`, `output_json`, `status`, `error_message`, `latency_ms`, started/finished timestamps, `extra`, timestamps, `deleted_at`.

Indexes cover owner/created time, conversation/created time, run/status, and run/step order. Tool output is truncated before persistence and sensitive values are redacted.

## 8. API Surface

### Start Agent run

`POST /api/ai/agent/runs`

Returns the completed run for the first synchronous release. The response includes `runNo`, `status`, `answer`, `plan`, `steps`, `evidence`, and `warnings`.

### Read run

`GET /api/ai/agent/runs/{runNo}`

Returns the persisted plan and steps for replay/debug UI.

### List tools

`GET /api/ai/agent/tools`

Returns enabled tool definitions, schemas, freshness, source, read-only state, and current availability.

### Later streaming extension

`POST /api/ai/agent/runs/stream` will use SSE after the synchronous contract and persistence path are stable. Streaming is not the first dependency because it makes failure recovery, refresh, and replay harder to validate.

## 9. Product Workflow

The global chat remains a fast entry point. A dedicated `/ai/agent` page becomes the inspectable workspace:

- Composer: natural-language request, workspace/date context, run button and stop state.
- Answer pane: final answer, assumptions, evidence and warnings.
- Plan pane: ordered capability steps with purpose, status, duration and data date.
- Tool detail: expandable sanitized input/output summary and source metadata.
- History: prior runs, status, duration, model and replay action.
- Empty state: examples for market overview, single-stock analysis and sector comparison.

The design follows the useful pattern shared by ChatGPT/Claude/Perplexity and data assistants such as ThoughtSpot: answer first, evidence and execution details available on demand, and no raw hidden chain-of-thought. Every write action is excluded until a confirmation design exists.

## 10. Delivery Phases

1. Architecture and contracts document. Commit independently.
2. Domain Agent contracts plus Market tool definitions/registry. Compile and unit test. Commit independently.
3. OpenAI-compatible tool-call protocol and bounded Agent loop. Compile, provider mock tests, commit independently.
4. PostgreSQL persistence, application service, API DTOs/controllers, migration and run retrieval. Package and smoke test, commit independently.
5. Agent page, service client, route/sidebar, global chat integration and responsive UI. ESLint/Vite, commit independently.
6. Release workflow, end-to-end market request, regression checks and final documentation. Commit independently.

Each phase records changed files, test commands, known limitations and the commit hash in `progress.md`.

## 11. Acceptance Scenarios

1. `今天大盘怎么样？` selects `market.overview` and returns date/freshness context.
2. `比较最近五个交易日行业资金流` selects history and sector capabilities, then compares returned rows.
3. `分析 600519 今天走势和盘中表现` selects daily, intraday and K-line capabilities with the symbol normalized.
4. An unknown symbol or missing date produces a clarification or explicit unavailable result, not fabricated data.
5. A provider failure leaves a failed step with a useful warning and does not claim the data was read.
6. A run can be reopened from the UI with the same plan, tool steps and final answer.
7. A model-generated unknown tool or invalid argument is rejected and recorded without executing arbitrary code.
