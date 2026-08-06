# Design Material Map

## Frontend Page Designs

`design/front/` is the current source for page-level frontend design material. Existing source comments and links use this path, so it remains canonical during the agent-configuration migration.

These files describe page purpose, layout, interaction, visual hierarchy, and feature-specific exceptions. They are read only when working on the relevant page; they are not global UI rules.

## Frontend/Architecture Notes

`design/frontend/` contains a smaller set of frontend-related notes. Treat each file according to its content. For example, `ai-provider.md` describes runtime provider and pricing behavior and should be consulted alongside `docs/agent/ai-capability.md` when changing that page or capability routing.

## Global UI Rules

Use the canonical UI documents under `cynexus-ui/`:

- `cynexus-ui/DESIGN.md` for design tokens and visual language.
- `cynexus-ui/FRONTEND_UI_IMPLEMENTATION_GUIDE.md` for React implementation and verification.
- `cynexus-ui/AGENTS.md` for short, always-applicable UI constraints.
