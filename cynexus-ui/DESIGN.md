---
name: Cynexus UI
description: A restrained, dense workbench for market research, finance, media, knowledge, and AI operations.
colors:
  background: "#ffffff"
  foreground: "#252525"
  card: "#ffffff"
  primary: "#343434"
  primary-foreground: "#fafafa"
  muted: "#f7f7f7"
  muted-foreground: "#757575"
  border: "#ebebeb"
  sidebar: "#fafafa"
  sidebar-accent: "#f2f2f2"
  destructive: "#d85b47"
  information: "#0071e3"
  positive: "#059669"
  negative: "#e11d48"
  warning: "#d97706"
typography:
  body:
    fontFamily: "Montserrat, -apple-system, BlinkMacSystemFont, Segoe UI, PingFang SC, Microsoft YaHei, sans-serif"
    fontSize: "14px"
    fontWeight: 400
    lineHeight: 1.5
  title:
    fontFamily: "Montserrat, -apple-system, BlinkMacSystemFont, Segoe UI, PingFang SC, Microsoft YaHei, sans-serif"
    fontSize: "24px"
    fontWeight: 600
    lineHeight: 1.25
    letterSpacing: "-0.025em"
  section:
    fontFamily: "Montserrat, -apple-system, BlinkMacSystemFont, Segoe UI, PingFang SC, Microsoft YaHei, sans-serif"
    fontSize: "16px"
    fontWeight: 600
    lineHeight: 1.4
  label:
    fontFamily: "Montserrat, -apple-system, BlinkMacSystemFont, Segoe UI, PingFang SC, Microsoft YaHei, sans-serif"
    fontSize: "12px"
    fontWeight: 500
    lineHeight: 1.4
  data:
    fontFamily: "ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace"
    fontSize: "12px"
    fontWeight: 400
    lineHeight: 1.4
rounded:
  sm: "6px"
  md: "8px"
  lg: "10px"
  xl: "14px"
  2xl: "16px"
  full: "9999px"
spacing:
  1: "4px"
  2: "8px"
  3: "12px"
  4: "16px"
  5: "20px"
  6: "24px"
  8: "32px"
components:
  button-primary:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.primary-foreground}"
    rounded: "{rounded.md}"
    padding: "8px 16px"
    height: "36px"
  button-secondary:
    backgroundColor: "{colors.muted}"
    textColor: "{colors.foreground}"
    rounded: "{rounded.md}"
    padding: "8px 16px"
    height: "36px"
  input:
    backgroundColor: "{colors.muted}"
    textColor: "{colors.foreground}"
    rounded: "{rounded.md}"
    padding: "4px 12px"
    height: "36px"
  card:
    backgroundColor: "{colors.card}"
    textColor: "{colors.foreground}"
    rounded: "{rounded.lg}"
    padding: "24px"
  badge:
    backgroundColor: "{colors.muted}"
    textColor: "{colors.muted-foreground}"
    rounded: "{rounded.full}"
    padding: "2px 8px"

## Overview

The shell has Personal and Development navigation spaces, selected in the sidebar header. Keep the existing neutral tokens and density in both spaces. Shared Home, Agent and Knowledge Base entries remain accessible in either space. Navigation behavior and menu ownership are documented in `../design/frontend/workspace-navigation.md`.

**Creative North Star: "A quiet research desk"**

Cynexus UI is a task-first application workbench. It should feel like a well-organized research desk during a busy trading session: the navigation is stable, the content is dense but legible, and every accent exists to identify a state or make the next action obvious. The interface serves finance, A-share market research, media transcription, knowledge management, scheduler operations, and AI configuration; it is not a marketing page or a decorative dashboard.

The canonical visual system is the neutral shadcn New York setup in `src/index.css` and `src/components/ui`. New pages start from `WorkspaceShell`, use semantic Tailwind tokens, and compose the existing Radix-backed primitives. The application currently contains two intentional visual exceptions: the media transcription flow has a legacy glass/gradient canvas, and the home page uses small illustrative gradient tiles. Those exceptions are local patterns and must not become the default for ordinary data pages.

**Key Characteristics:**

- Restrained light surfaces with dark neutral text and a single primary action color.
- Stable density: `12px` labels, `14px` body copy, `16px` section titles, `24px` page titles.
- Flat or tonal depth first; borders and muted surfaces carry hierarchy before shadows.
- High scanability for tables, metrics, status badges, filters, and time-series data.
- State is explicit: loading, stale, real-time, snapshot, unavailable, empty, error, and saved.

**The Shell-First Rule.** A new route is not complete until it uses the same workspace shell, breadcrumb, sidebar behavior, content scrolling model, and notification root as neighboring pages.

## Colors

The current theme is a neutral grayscale system with restrained semantic accents. In code, use `bg-background`, `text-foreground`, `bg-card`, `bg-muted`, `text-muted-foreground`, `border-border`, `bg-primary`, and the matching semantic tokens before reaching for a literal color.

### Neutral roles

- **Background / Card:** white (`#ffffff`) for the main canvas and raised content surfaces.
- **Foreground:** near-black (`#252525`) for headings, body copy, and primary data.
- **Muted:** very light neutral (`#f7f7f7`) for secondary panels, filters, inactive controls, and skeletons.
- **Muted foreground:** medium neutral (`#757575`) for labels, descriptions, timestamps, and supporting metadata.
- **Border:** low-contrast neutral (`#ebebeb`) for separators, table rows, inputs, and panel edges.
- **Sidebar:** near-white (`#fafafa`) with a slightly darker active/hover surface (`#f2f2f2`).

### Semantic roles

- **Primary / information:** blue (`#0071e3`) is reserved for links, refresh/active information, focus-adjacent emphasis, and market information cues. The global `primary` token remains neutral black; do not silently replace it with blue.
- **System positive:** emerald (`#059669`) means a successful or healthy system state.
- **System negative:** red/rose (`#e11d48` / the destructive token) means failure, danger, or destructive action.
- **Warning:** amber (`#d97706`) means attention, stale data, incomplete routing, or a recoverable risk.
- **A-share market direction:** red means an upward move and green means a downward move. This domain convention is different from generic system success/error colors; pair it with a sign, label, arrow, or value rather than color alone.

**The Semantic Accent Rule.** Accent colors may identify an action, selection, direction, health state, or warning. They must not be used as ambient decoration across a whole page.

### Exceptions

The gradient and glass treatment in `src/index.css` and `src/views/mp4-to-word/styles.ts` belongs to the MP4-to-Word transcription/summary experience. The small gradient tiles and hand-drawn SVG illustrations in `src/views/home.tsx` belong to the Application Hub. A new finance, settings, knowledge, AI, or market page should not copy those treatments.

## Typography

The intended UI family is Montserrat, with system and Chinese fallbacks. `src/index.css` declares self-hosted Montserrat weights 400 and 700, but the current repository only contains the font README under `public/fonts/montserrat`; verify the font assets are present before relying on the custom face in a release image. Do not introduce a display serif or decorative font into labels, buttons, tables, or navigation.

### Hierarchy

- **Page title** (`text-2xl font-semibold tracking-tight`, approximately `24px`): one per page, usually in a compact header with an icon and a short description.
- **Section title** (`text-base` or `text-sm font-semibold`, `14–16px`): identifies a data group, table, chart, or panel. Keep it scannable, not promotional.
- **Body** (`text-sm`, `14px`, around `1.5` line-height): default UI copy, rows, and descriptions. Descriptive prose may use `leading-6` or `leading-7`.
- **Label / metadata** (`text-xs`, `12px`): filters, timestamps, source names, status detail, helper copy, and secondary counts.
- **Micro label** (`text-[10px]` or `text-[11px]`): dense table headers, compact badges, chart annotations, and status chips only.
- **Data / code** (`font-mono tabular-nums`): stock symbols, IDs, timestamps, amounts, percentages, latency, token counts, and other values where column alignment matters.

Use fixed product-ui sizes instead of viewport-scaled headings. The global `h1` rule and legacy media typography are historical defaults; new pages should use explicit Tailwind classes so page titles do not change size unexpectedly inside dense layouts. Use `truncate`, `line-clamp-2`, `min-w-0`, and `break-words` deliberately where data can be long.

**The Data-First Type Rule.** A user should be able to tell a value, its label, its unit, and its freshness without reading a paragraph. Use weight and spacing for hierarchy; do not make every label uppercase or heavily tracked.

## Elevation

Cynexus uses a flat/tonal hierarchy. The default `Card` primitive has `rounded-xl border bg-card shadow-sm`, but many operational surfaces intentionally override it with `shadow-none`, `border-0`, or `bg-muted/*` to keep dense pages calm. Prefer tonal separation and one-pixel borders. Shadows are state or layer cues, not decoration.

### Shadow vocabulary

- **Resting surface:** no shadow or the primitive `shadow-sm` when a card must separate from the canvas.
- **Hover / selected surface:** a subtle `shadow-sm` or `data-[state=active]:shadow-sm`, usually combined with `bg-background` or `bg-muted`.
- **Popover / dialog / drawer:** use the Radix/shadcn primitive's existing elevation and portal behavior. Do not hand-build a competing z-index or overlay system.
- **Legacy media canvas:** the larger blur, translucent surfaces, and 20–30px radii in the MP4 workflow are a bounded exception.

**The No-Card-Wall Rule.** Do not nest cards to create hierarchy. Use a section heading, a divider, a muted row, or a single bordered container before adding another card.

## Components

### Page shell and navigation

Every normal page uses `WorkspaceShell` from `src/layout/workspace-shell.tsx`:

- Desktop sidebar uses `16rem`; collapsed icon mode uses `3rem`; mobile sheet uses `18rem`.
- The top header is `h-16`, sticky, lightly translucent, and separated by a bottom border. It contains `SidebarTrigger`, breadcrumb, and the global command trigger.
- The content viewport is independently scrollable. The default inner padding is `p-3 md:p-6` with `gap-6` between top-level blocks.
- Set `fullBleed` only for a page that truly owns a full canvas. Set `contentScrollable={false}` only when the page has a fixed-height editor/table workspace such as Knowledge Base or AI Usage.
- Sidebar groups are collapsible, active routes use the sidebar accent surface, and `Ctrl+B` / `Cmd+B` toggles the sidebar. Preserve the cookie-backed open state and localStorage-backed open groups.
- Add a route to `src/router/index.tsx` and a user-facing entry to `src/config/sidebar.ts` together. Breadcrumb labels should match the sidebar vocabulary.

### Buttons

Use `src/components/ui/button.tsx` and Lucide icons. The default control is a compact `h-9 rounded-md` button with `text-sm font-medium`.

**Prefer `variant="secondary"` whenever it is enough.** Most toolbar, refresh, filter, cancel, retry, and alternate actions should be secondary. Do not leave `variant` unset, and do not reach for `outline` or primary just to make a control look like a button.

- **Secondary:** `variant="secondary"` is the default choice for almost every button.
- **Primary:** `variant="default"` only for the page's single main commit or confirm action, not for every clickable item.
- **Ghost:** `variant="ghost"` for row actions, compact toolbars, and icon-only commands where a filled chip is too heavy.
- **Destructive:** `variant="destructive"` for irreversible or high-risk deletes.
- **Outline / link:** use only when a visible border or inline text action is specifically required; prefer secondary first.
- **Sizes:** `xs h-6`, `sm h-8`, `default h-9`, `lg h-10`; use `icon-sm`/`icon` for icon-only controls.
- Icon-only buttons require `aria-label` and a `title` when the action is not obvious. Text buttons should use direct verbs such as `Refresh`, `Save`, `Open`, `Retry`, or `Delete`.
- Keep focus, disabled, and loading states from the primitive. A loading button keeps its width and swaps the icon for `Loader2` or `RefreshCw` with `animate-spin`.

### Cards and panels

Use `Card` for a genuinely framed, reusable unit. For dense operational sections, the more common composition is a single `section` with `rounded-lg border border-border/60 bg-card` or `rounded-lg bg-muted/35`.

- Standard radius is `rounded-lg` (`10px`) for panels; `rounded-md` (`8px`) for controls; `rounded-full` for badges and compact chips.
- Standard panel padding is `p-4` or `p-5`; compact rows use `px-3 py-2` or `px-4 py-3`.
- Header pattern: title + optional description on the left, count/status/action on the right, then a border divider if the panel has a body.
- Do not put a full card inside another full card. Use a muted inset row (`bg-muted/45`) for a sub-item.
- Keep `min-w-0` on flex/grid children that contain dynamic text.

### Inputs, filters, tabs, and badges

- **Input:** use `Input`; the current default is `h-9`, `rounded-md`, borderless `bg-muted/60`, `px-3`, with a visible focus ring. Use `border border-input bg-background` only when a field needs a stronger boundary, such as a form dialog.
- **Select / date picker:** use the existing Radix-backed components. Compact filters commonly use `h-7` or `h-8`, `border-0`, `bg-muted/45`, and `text-xs`.
- **Tabs:** use `Tabs` and `TabsList`. The default variant is a muted segmented control; `line` is for navigation where the active underline carries the state. Do not invent button groups with different active behavior.
- **Badge:** use `Badge` for status, counts, categories, and compact context. It is a pill by default; keep text short and do not use a badge as a paragraph.
- **Status labels:** English only on badges, filters, and status chips. Use `Queued`, `Running`, `Waiting review`, `Success`, `Failed`, `Cancelled`, or the equivalent English verb/noun for the status. Do not use Chinese such as `已入队`, `处理中`, `待确认`, `已完成`, `成功`, `失败`, or `已归档`.
- **Filter groups:** place the label/icon first, keep controls in a wrapping row, and reset pagination when a filter changes.

### Tables, metrics, and charts

- Use `Table` for structured data. The primitive is horizontally scrollable; preserve it for wide market/provider tables instead of squeezing columns until they become unreadable.
- Header cells are small, medium-weight, and muted. Rows use a low-contrast bottom border and `hover:bg-muted/50`. Keep numeric cells right-aligned, `font-mono`, and `tabular-nums`.
- Use `min-w-0`, fixed or minimum column tracks, `truncate`, and a sticky header for long lists. If the first identity column must remain visible, make it sticky with a matching background and a one-pixel separator.
- KPI values may use `text-xl` to `text-3xl`, but the label and freshness/source context must remain adjacent. Do not build a generic hero-metric wall.
- Charts should be placed in a constrained, bordered/tonal panel with a visible title, unit, date range, and empty/loading state. Do not let a chart's colors carry the only explanation of direction or status.

### Dialogs, sheets, drawers, and command surfaces

Use the existing `Dialog`, `Sheet`, `Drawer`, `Popover`, `Tooltip`, and command palette components. A drawer is preferred for inspecting a selected row or related stock list without losing context. A dialog is for focused create/edit/confirm flows. Exhaust inline or progressive disclosure before adding a modal.

- Dialogs need a title, description when useful, stable footer actions, loading state, and an explicit close path.
- Large data detail uses a full-height right drawer with a bordered header and internal scrolling, not a page-wide overlay card.
- Icon-only controls get tooltips when the icon is not self-explanatory. Tooltip copy names the action, not the implementation.
- Do not render an absolutely positioned dropdown inside an overflow container; rely on the Radix portal behavior.

### Loading, empty, error, and feedback states

Every data block has an explicit state contract:

1. **Loading:** keep the final geometry and use `Skeleton`; use a spinner only inside the action currently running.
2. **Error:** use `Alert` or the shared `notification` helper. Say what failed, include the useful error detail, and offer `Refresh`, `Retry`, or the relevant recovery action.
3. **Empty:** explain what is absent and what the user can do next. Never leave a blank card.
4. **Stale/unavailable:** label data freshness or provider availability near the affected block; do not imply real-time data when the page is a snapshot.
5. **Success/saved:** use a concise notification or inline `Saved` state. Keep the action vocabulary consistent with the result message.

### Domain-specific patterns

- **Finance:** amounts use `font-mono tabular-nums`; income/expense or asset/liability distinctions are expressed with labels plus color. Account and transaction detail can open in a drawer.
- **Market:** red is up and green is down for A-share data; show signed values, arrows, dates, source, and manual/automatic provenance where available. Heatmaps and charts use dense, stable geometry.
- **AI Provider:** provider and capability routing are switchboard-like tables. Keep `runtime`, `prompt`, `group`, `description`, and issue status visible together. New AI capabilities must also be registered in this page's Capability routing list as required by the project rules.
- **Knowledge Base:** two-pane layouts are valid when browsing and reading/editing at the same time. Use `contentScrollable={false}`, let each pane own its scroll, and preserve the selected row state.
- **MP4 to Word:** the glass/gradient summary workspace is a contained legacy signature. Keep its classes and styles in the MP4 feature folder; do not export them as general primitives.

## Do's and Don'ts

### Do:

- **Do** start new pages from `WorkspaceShell` and the existing semantic tokens in `src/index.css`.
- **Do** use `@/components/ui` primitives, `cn`, Lucide icons, and the existing Radix behavior before writing a new control.
- **Do** keep the first viewport task-oriented: title, freshness/context, primary action, filters, then the most decision-relevant data.
- **Do** keep page-level spacing at `gap-6`, section spacing at `gap-4`, and compact row spacing at `gap-2` unless the content requires a different rhythm.
- **Do** make dynamic content resilient with `min-w-0`, `truncate`, `line-clamp`, `break-words`, stable grid tracks, and horizontal scrolling for wide tables.
- **Do** represent status with text/icon/shape plus color, and use the A-share red-up/green-down convention only for market direction.
- **Do** provide visible `focus-visible` feedback, keyboard activation for interactive controls, accessible labels, and `motion-reduce` alternatives.
- **Do** use `Skeleton` for content loading and keep the skeleton geometry close to the loaded layout.
- **Do** keep domain-specific CSS beside its feature, as with `src/views/mp4-to-word/styles.ts`; reserve `src/index.css` for global tokens and true global behavior.
- **Do** validate desktop and mobile widths, especially `md` sidebar collapse, `sm` filter wrapping, and `xl` table/chart layouts.

### Don't:

- **Don't** make the product feel like a marketing page, an information site, or a decorative dashboard.
- **Don't** use large decorative gradients, heavy shadows, floating card walls, or glassmorphism on ordinary operational pages.
- **Don't** copy the MP4-to-Word glass/gradient canvas or the home-page illustration tiles into finance, market, AI, settings, or knowledge pages.
- **Don't** create nested cards when a muted row, divider, or single panel is enough.
- **Don't** use display fonts in UI labels, buttons, navigation, data, or tables.
- **Don't** invent custom scrollbars, custom form controls, non-standard modals, or new active/selected states when the shared primitive already solves the interaction.
- **Don't** use literal `bg-[#f9f9f9]`, `text-slate-*`, or one-off hex colors for a new shared pattern; map the need to a semantic token or add a justified token centrally.
- **Don't** use color alone to communicate market direction, system health, loading, failure, or selection.
- **Don't** put an unbounded long table inside a narrow card; use a scroll container and preserve a readable minimum width.
- **Don't** use a modal as the first answer to a workflow problem; try inline editing, a drawer, or progressive disclosure first.
- **Don't** add decorative motion or page-load choreography. Motion should communicate refresh, saving, loading, reveal, selection, or navigation state and usually stay within `150–250ms`.
- **Don't** leave a data section blank during loading or when there is no data; show skeleton, error, or a useful empty state.
- **Don't** forget to update the route, sidebar entry, breadcrumb label, and capability routing when adding a new page or AI capability.
