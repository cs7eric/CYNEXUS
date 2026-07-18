# AI Usage

## Overview

Inspect AI request volume, token usage, estimated cost, provider health, and individual failure reasons.

## Route

`/ai/usage`

## Module

`cynexus-ui/src/views/ai/usage/index.tsx`

## Data Sources

- Langfuse observations through the AI usage API

## APIs

- `fetchAiUsage`: fetch usage observations for the selected date range

## Components

- `SummaryMetric`: render top-level usage metrics.
- `CapabilityRow`: render usage grouped by capability.
- `ProviderHealthRow`: render provider success health.

## Data Flow

Langfuse observations -> `fetchAiUsage` -> page aggregation -> summary, health, and recent-call table.

## Maintenance Notes

Before modifying this page, review:

- `design/front/infra/index.md`
- `design/front/infra/ai-usage.detail.md`
- `design/front/reuse/reuse.md`
