# AI Usage

## Overview

Inspect AI request volume, token usage, estimated cost, provider health, individual failure reasons, and persisted Agent request/token limits.

## Route

`/ai/usage`

## Module

`cynexus-ui/src/views/ai/usage/index.tsx`

## Data Sources

- Langfuse observations through the AI usage API
- Agent quota policies from the AI configuration API

## APIs

- `fetchAiUsage`: fetch usage observations for the selected date range
- `fetchAgentQuotaPolicies`, quota policy CRUD: manage the persisted request/token limits

## Components

- `SummaryMetric`: render top-level usage metrics.
- `CapabilityRow`: render usage grouped by capability.
- `ProviderHealthRow`: render provider success health.
- Quota policy list/editor: explains matching precedence and manages global, tenant, and user scope policies.

## Data Flow

Langfuse observations -> `fetchAiUsage` -> page aggregation -> summary, health, and recent-call table.

PostgreSQL quota policy rows -> quota policy API -> scope-aware list/editor -> Agent runtime matching (user -> tenant -> global).

## Maintenance Notes

Before modifying this page, review:

- `design/front/infra/index.md`
- `design/front/infra/ai-usage.detail.md`
- `design/front/reuse/reuse.md`
