# Market Information

## Purpose

Market Information is a market-level briefing surface. It combines the Cailian Press telegraph, CNInfo/Juchao policy announcements, and leader-linked events with the latest limit-pool, hot-rank, and Dragon Tiger snapshots. Individual stock news sources are intentionally excluded from the macro feed.

## Layout

- Header: page title, short scope statement, snapshot time, and refresh action.
- Market Pulse: compact market stance, event count, source freshness, focus industries, headline, and key risk.
- Decision workspace: left-side priority event stream; right-side Upcoming Triggers, Impact Map, and Risk / Watchlist.
- Full information feed: task-oriented filters, paginated table, and collapsed Raw 7x24 stream.
- Event Sheet: source members, deterministic mappings, structured analysis, evidence, risks, and uncertainties.

## Data flow

`cynexus-data` persists `cailian_telegraph`, `cninfo_market_announcements`, `eastmoney_hot_rank`, `eastmoney_limit_pool`, and `eastmoney_daily_dragon_tiger` into `cynexus_appl_market` fact tables. The Java market modules expose them through `/api/market/information`; the Information page consumes that endpoint from `cynexus-ui/src/services/information.ts`.

## Module Location

- Route: `/information`
- Page entry: `cynexus-ui/src/views/information/index.tsx`
- Components: `cynexus-ui/src/views/information/components/`
- Compatibility: `/market/information` redirects to `/information`

## Visual rules

Use the existing workspace tokens (`bg-background`, `bg-card`, `bg-muted`, and semantic positive/negative colors). Panels are quiet grouping surfaces with a subtle ring, not nested decorative cards. The priority event stream owns the primary decision space; supporting panels stay compact and scannable.
