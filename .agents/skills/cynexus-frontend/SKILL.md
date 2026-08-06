---
name: cynexus-frontend
description: Use when modifying React, TypeScript, Vite, Tailwind, shadcn, frontend pages, components, charts, responsive layouts, accessibility, or visual design under cynexus-ui.
---

# Cynexus Frontend Workflow

1. Read `cynexus-ui/AGENTS.md`.
2. For visual, layout, or component changes, read `cynexus-ui/DESIGN.md`.
3. For implementation, state, responsive, chart, or accessibility changes, read `cynexus-ui/FRONTEND_UI_IMPLEMENTATION_GUIDE.md`.
4. Read `cynexus-ui/PRODUCT.md` when product intent or page purpose is unclear.
5. Inspect neighboring routes and shared primitives before editing.
6. Use `WorkspaceShell`, semantic tokens, shadcn/Radix primitives, and Lucide icons.
7. Update route, sidebar, breadcrumb, loading/error/empty/stale/success states, and mobile behavior together.
8. Keep ordinary pages quiet and task-focused; keep MP4 glass styles and Home gradient tiles local exceptions.
9. Run the relevant `pnpm lint` and `pnpm build` checks from `cynexus-ui` after implementation.
