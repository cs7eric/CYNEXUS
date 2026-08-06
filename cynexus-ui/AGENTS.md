# Cynexus UI Agent Guide

## Runtime and Structure

Cynexus UI is a React 19 + TypeScript + Vite 8 application using Tailwind CSS v4, shadcn New York components, Radix primitives, and `lucide-react`.

- Normal pages use `src/layout/workspace-shell.tsx` and `WorkspaceShell`.
- Shared primitives live in `src/components/ui`.
- Shared product components live in `src/components/app` or `src/components/cynexus`.
- Page entry points live in `src/views/<feature>/`.
- Page-private components live in `src/views/<feature>/components/`.
- Page-private pure logic lives in `src/views/<feature>/lib/`.
- API access belongs in `src/services`, not duplicated inside views.
- Route, sidebar entry, and breadcrumb label must be updated together.

## Non-negotiable UI Rules

- The default visual language is a restrained, dense research workbench, not a marketing page or decorative dashboard.
- Prefer semantic tokens from `src/index.css` and existing shadcn/Radix primitives before literal colors or new controls.
- Use Lucide icons for common actions. Icon-only controls need accessible labels and a tooltip/title when the action is not obvious.
- Use stable layouts with `min-w-0`, truncation, wrapping, minimum table widths, and independent pane scrolling where needed.
- Every data block must handle loading, error, empty, stale/unavailable, and success states.
- Do not communicate market direction, system health, selection, or failure by color alone.
- A-share market data uses red for upward movement and green for downward movement, accompanied by sign, label, icon, or arrow.
- Do not add ordinary-page glassmorphism, large ambient gradients, heavy shadows, nested card walls, or decorative motion.
- The MP4-to-Word glass/gradient canvas and Home page gradient tiles are local exceptions only.
- New AI capabilities must also update the AI Provider Capability routing list.

## Required References

- Design tokens and visual language: `DESIGN.md`
- React implementation, component, state, responsive, and accessibility rules: `FRONTEND_UI_IMPLEMENTATION_GUIDE.md`
- Product intent and anti-references: `PRODUCT.md`
- Page-specific designs: `../design/frontend/` and `../design/front/`

For frontend work, use `$cynexus-frontend`. It decides which of these references to read for the task.
