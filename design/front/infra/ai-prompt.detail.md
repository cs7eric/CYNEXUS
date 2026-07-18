# AI Prompt

## Overview

Manage versioned AI prompts, scenario metadata, variables, templates, and output constraints.

## Route

`/ai/prompt`

## Module

`cynexus-ui/src/views/ai/prompt/index.tsx`

## Data Sources

- AI prompt registry APIs

## APIs

- `fetchAiPrompts`: list prompts with filters
- `createAiPrompt` / `updateAiPrompt` / `deleteAiPrompt`: maintain prompt records

## Components

- Prompt list and editor dialog are composed in the page module.

## Data Flow

Prompt registry API -> page state -> prompt list and editor -> UI.

## Maintenance Notes

Before modifying this page, review:

- `design/front/infra/index.md`
- `design/front/infra/ai-prompt.detail.md`
- `design/front/reuse/reuse.md`
