# Agent Guidance Map

This directory contains detailed project guidance that is intentionally not injected into every task. The root `AGENTS.md`, nested area `AGENTS.md` files, and repository skills route to these documents when relevant.

| Topic | Reference | Primary entry |
| --- | --- | --- |
| CodeGraph | `codegraph.md` | root `AGENTS.md` |
| Java DDD | `backend-ddd.md` | Java module guidance |
| Constants | `constants.md` | Java backend skill |
| AI prompts and capabilities | `ai-capability.md` | `cynexus-ai/AGENTS.md` |
| Agent prompt cache | `agent-prompt-cache.md` | `cynexus-ai/AGENTS.md` |
| Release and versions | `release.md` | `deploy/AGENTS.md` |
| SQL and repository hygiene | `repository-and-database.md` | root/deploy guidance |

UI design and implementation remain canonical under `cynexus-ui/` because they are also human-facing product documentation:

- `cynexus-ui/DESIGN.md`
- `cynexus-ui/FRONTEND_UI_IMPLEMENTATION_GUIDE.md`
- `cynexus-ui/PRODUCT.md`

Page-specific design material currently belongs under `design/front/`; `design/frontend/` contains a smaller set of frontend/AI design notes and should not be treated as automatically loaded agent guidance. Runtime contracts and data-flow notes belong under `docs/architecture/` or the owning domain documentation.
