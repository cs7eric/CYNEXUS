# Findings

- The repository contains Java services, a Python Job Executor, a React/Vite UI, and Docker Release infrastructure.
- Codex automatically discovers AGENTS.md along the current working-directory path, not arbitrary Markdown files.
- Repository skills are discovered under `.agents/skills`; complete skill instructions load only when a skill is selected.
- Project custom agents use `.codex/agents/*.toml`.
- Project command execution policies use `.codex/rules/*.rules`; these are not thematic documentation.
- Project hooks use `.codex/hooks.json` or inline config hooks and require project trust.
- `cynexus-ui/DESIGN.md` is the canonical UI design system; `FRONTEND_UI_IMPLEMENTATION_GUIDE.md` is the detailed implementation guide.
- `design/front/market-information.md` mixes page design and data-flow information; it remains unchanged in this migration to avoid unnecessary content churn.
- `design/frontend/ai-provider.md` is primarily a runtime/architecture note rather than a visual design document; the new AI guidance routes to `docs/agent/ai-capability.md`.
- The existing singular `AGENT.md` duplicated Java DDD guidance and was not a default Codex instruction filename. Its content is preserved in `docs/agent/backend-ddd.md`.
