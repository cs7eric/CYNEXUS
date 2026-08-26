# AI Provider

## Overview

Manage AI providers, models, pricing, and capability bindings.

## Route

`/ai/provider`

## Module

`cynexus-ui/src/views/ai/provider/index.tsx`

## Data Sources

- AI provider and capability configuration APIs

## APIs

- `fetchAiProviders`: load configured providers
- `createAiProvider` / `updateAiProvider` / `deleteAiProvider`: maintain providers
- `fetchAiBindings` / `upsertAiBinding`: maintain capability routing
- `fetchAiCapabilities`: load the backend-owned Agent capability registry snapshot
- `fetchAgentCapabilities` / `updateAgentCapabilityGovernance`: inspect and govern enabled status, availability, and priority

## Components

- Provider editor and capability binding panels are composed in the page module.
- Capability binding rows expose the per-capability group and reasoning effort; both are persisted in the binding params as `route_group` and `reasoning_effort`.

## Data Flow

AI configuration APIs and the backend-owned capability registry -> page state -> provider, binding, and governance forms -> UI.

Binding runtime parameters are preserved during capability route updates; group changes are written to `route_group`, and reasoning effort uses `low`, `medium`, or `high`, with `low` as the default.

## Maintenance Notes

Before modifying this page, review:

- `design/front/infra/index.md`
- `design/front/infra/ai-provider.detail.md`
- `design/front/reuse/reuse.md`
