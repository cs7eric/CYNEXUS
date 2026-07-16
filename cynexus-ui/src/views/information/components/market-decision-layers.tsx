import { AlertTriangle, ArrowRight, ArrowUpRight, CalendarClock, GitBranch, ShieldAlert } from "lucide-react"
import { Badge } from "@/components/ui/badge"
import type {
  MarketBriefContent,
  MarketBriefKeyEvent,
  MarketInformationBrief,
  MarketInformationImpactIndustry,
  MarketInformationNews,
} from "@/services/information"

interface DecisionEvent {
  clusterId?: number | null
  title: string
  summary?: string | null
  eventType?: string | null
  direction?: string | null
  materiality?: string | null
  affectedScopes?: string[]
  affectedIndustries?: string[]
  affectedCompanies?: string[]
  relatedStocks?: string[]
  source?: string | null
  issuer?: string | null
}

function parseBrief(value?: MarketInformationBrief | null): MarketBriefContent | null {
  if (!value?.contentJson) return null
  try { return JSON.parse(value.contentJson) as MarketBriefContent } catch { return null }
}

function label(value?: string | null) {
  return (value || "unknown").replaceAll("_", " ")
}

function directionClass(direction?: string | null) {
  if (direction === "positive") return "bg-emerald-500/10 text-emerald-700 dark:text-emerald-300"
  if (direction === "negative") return "bg-rose-500/10 text-rose-700 dark:text-rose-300"
  if (direction === "mixed") return "bg-amber-500/10 text-amber-700 dark:text-amber-300"
  return "bg-muted text-muted-foreground"
}

function toEvent(value: MarketBriefKeyEvent | string): DecisionEvent {
  if (typeof value === "string") return { title: value }
  return value
}

function fallbackEvents(items: MarketInformationNews[]): DecisionEvent[] {
  return items.slice(0, 8).map((item) => ({
    clusterId: item.clusterId,
    title: item.title,
    summary: item.summary,
    eventType: item.category?.toLowerCase(),
    direction: item.marketDirection,
    materiality: item.materiality,
    affectedScopes: (item.impactIndustries ?? []).slice(0, 3).map((industry) => industry.tdxName),
    affectedIndustries: (item.impactIndustries ?? []).slice(0, 3).map((industry) => industry.tdxName),
    relatedStocks: item.issuer ? [item.issuer] : [],
    source: item.source,
    issuer: item.issuer,
  }))
}

function WhatMattersToday({ brief, items, onSelectEvent }: { brief?: MarketInformationBrief | null; items: MarketInformationNews[]; onSelectEvent: (clusterId: number) => void }) {
  const content = parseBrief(brief)
  const briefEvents = content?.keyEvents?.slice(0, 8).map(toEvent) ?? []
  const itemEvents = fallbackEvents(items)
  const events: DecisionEvent[] = itemEvents.length ? [...itemEvents, ...briefEvents.filter((event) => !itemEvents.some((item) => item.title === event.title))].slice(0, 8) : briefEvents
  return <section className="min-w-0 rounded-xl border border-border/70 bg-card px-4 py-3 shadow-none">
    <div className="mb-2.5 flex items-center justify-between gap-3 border-b border-border/70 pb-2.5"><div><div className="flex items-center gap-2"><ArrowUpRight className="size-4 text-primary" /><h2 className="text-sm font-semibold">What matters today</h2></div><p className="mt-1 text-xs text-muted-foreground">Priority events with a clear market or industry connection.</p></div><span className="shrink-0 text-xs tabular-nums text-muted-foreground">{events.length} events</span></div>
    {events.length ? <div className="divide-y divide-border/70">{events.map((event, index) => { const sourceItem = event.clusterId ? items.find((item) => item.clusterId === event.clusterId) : undefined; const industries = [...new Set([...(event.affectedIndustries ?? []), ...((sourceItem?.impactIndustries ?? []).map((industry) => industry.tdxName))])].slice(0, 3); const stocks = [...new Set([...(event.affectedCompanies ?? []), ...(event.relatedStocks ?? []), ...(event.issuer ? [event.issuer] : []), ...(sourceItem?.issuer ? [sourceItem.issuer] : []), ...((sourceItem?.impactIndustries ?? []).flatMap((industry) => (industry.relatedStocks ?? []).map((stock) => stock.name || stock.code)))])].slice(0, 3); const direction = event.direction ?? sourceItem?.marketDirection; const materiality = event.materiality ?? sourceItem?.materiality; return <button key={`${event.clusterId ?? event.title}-${index}`} type="button" className="group flex w-full items-start gap-3 py-3 text-left transition-colors first:pt-1 last:pb-1 hover:bg-muted/30" onClick={() => event.clusterId && onSelectEvent(event.clusterId)}><span className="mt-0.5 flex size-5 shrink-0 items-center justify-center rounded-md bg-muted text-[11px] font-semibold tabular-nums text-muted-foreground">{index + 1}</span><span className="min-w-0 flex-1"><span className="flex min-w-0 items-start gap-2"><span className="line-clamp-2 min-w-0 flex-1 text-sm font-medium leading-5 group-hover:text-primary">{event.title}</span>{direction ? <Badge variant="secondary" className={`shrink-0 border-0 text-[10px] capitalize ${directionClass(direction)}`}>{label(direction)}</Badge> : null}</span><span className="mt-1 flex flex-wrap items-center gap-x-2 gap-y-1 text-[11px] text-muted-foreground"><span className="capitalize">{label(event.eventType)}</span>{event.source ? <span>· {event.source}</span> : null}</span><span className="mt-2 flex min-w-0 flex-wrap items-center gap-1 text-[10px] text-muted-foreground"><span className="rounded bg-muted px-1.5 py-0.5 font-medium text-foreground">Event</span><ArrowRight className="size-3 shrink-0" /><span className="max-w-36 truncate rounded bg-sky-500/10 px-1.5 py-0.5 text-sky-700 dark:text-sky-300" title={industries.join(", ")}>{industries.length ? industries.join(" / ") : "Unmapped industry"}</span><ArrowRight className="size-3 shrink-0" />{direction ? <span className={`rounded px-1.5 py-0.5 capitalize ${directionClass(direction)}`}>{label(direction)}</span> : <span className="rounded bg-muted px-1.5 py-0.5">Unknown direction</span>}<ArrowRight className="size-3 shrink-0" /><span className="rounded bg-muted px-1.5 py-0.5 capitalize">{label(materiality)} impact</span><ArrowRight className="size-3 shrink-0" /><span className={`max-w-48 truncate rounded px-1.5 py-0.5 ${stocks.length ? "bg-violet-500/10 text-violet-700 dark:text-violet-300" : "bg-muted text-muted-foreground"}`} title={stocks.join(", ")}>Stocks: {stocks.length ? stocks.join(" / ") : "Unmapped"}</span></span></span><ArrowUpRight className="mt-1 size-3.5 shrink-0 text-muted-foreground opacity-0 transition-opacity group-hover:opacity-100" /></button> })}</div> : <p className="py-5 text-sm text-muted-foreground">No priority events are available.</p>}
  </section>
}

function UpcomingTriggers() {
  return <section className="rounded-xl border border-border/70 bg-card px-4 py-3 shadow-none"><div className="flex items-center gap-2"><CalendarClock className="size-4 text-primary" /><h2 className="text-sm font-semibold">Upcoming triggers</h2><span className="ml-auto text-[11px] text-muted-foreground">Calendar</span></div><div className="mt-2 flex items-start gap-2.5 border-t border-border/70 pt-2.5"><CalendarClock className="mt-0.5 size-3.5 shrink-0 text-muted-foreground" /><p className="text-xs leading-5 text-muted-foreground">No verified trigger dates are connected yet. Calendar sources will appear here once confirmed.</p></div></section>
}

function ImpactMap({ items }: { items: MarketInformationNews[] }) {
  const groups = new Map<string, { industry: MarketInformationImpactIndustry; count: number; directions: Set<string>; stocks: Set<string> }>()
  items.forEach((item) => (item.impactIndustries ?? []).forEach((industry) => {
    const key = industry.tdxCode || industry.tdxName
    const current = groups.get(key)
    const directions = current?.directions ?? new Set<string>()
    const stocks = current?.stocks ?? new Set<string>()
    if (item.marketDirection && item.marketDirection !== "unknown") directions.add(item.marketDirection)
    if (item.issuer) stocks.add(item.issuer)
    for (const stock of industry.relatedStocks ?? []) stocks.add(stock.name || stock.code)
    groups.set(key, current ? { ...current, count: current.count + 1, directions, stocks } : { industry, count: 1, directions, stocks })
  }))
  const top = [...groups.values()].sort((a, b) => b.count - a.count || a.industry.tdxName.localeCompare(b.industry.tdxName)).slice(0, 6)
  return <section className="rounded-xl border border-border/70 bg-card px-4 py-3 shadow-none"><div className="flex items-center gap-2"><GitBranch className="size-4 text-primary" /><h2 className="text-sm font-semibold">Impact map</h2><span className="ml-auto text-[11px] text-muted-foreground">Event → industry → stocks</span></div>{top.length ? <div className="mt-2 divide-y divide-border/70">{top.map(({ industry, count, directions, stocks }) => <div key={industry.tdxCode || industry.tdxName} className="py-2 first:pt-2 last:pb-0"><div className="flex items-center gap-2"><span className="size-1.5 shrink-0 rounded-full bg-primary/65" /><span className="min-w-0 flex-1 truncate text-xs font-medium" title={industry.tdxName}>{industry.tdxName}</span><span className={`shrink-0 text-[10px] capitalize ${directionClass(directions.size === 1 ? [...directions][0] : "mixed")}`}>{directions.size ? [...directions].map(label).join(" / ") : "unknown"}</span><span className="w-5 shrink-0 text-right text-[11px] tabular-nums text-muted-foreground">{count}</span></div>{stocks.size ? <div className="mt-1 pl-3 text-[10px] text-muted-foreground">Stocks: {[...stocks].slice(0, 3).join(" / ")}</div> : null}</div>)}</div> : <p className="mt-2 text-xs leading-5 text-muted-foreground">No deterministic industry relationship is available.</p>}</section>
}

function RiskWatchlist({ brief }: { brief?: MarketInformationBrief | null }) {
  const content = parseBrief(brief)
  const risks = content?.riskSignals ?? []
  const uncertainty = content?.uncertainties?.[0]
  return <section className="rounded-xl border border-border/70 bg-card px-4 py-3 shadow-none"><div className="flex items-center gap-2"><ShieldAlert className="size-4 text-rose-600" /><h2 className="text-sm font-semibold">Risk / watchlist</h2><span className="ml-auto text-[11px] text-muted-foreground">What could change the read</span></div>{risks.length ? <div className="mt-2 divide-y divide-border/70">{risks.slice(0, 3).map((risk, index) => <div key={`${risk.summary}-${index}`} className="py-2 first:pt-2 last:pb-0"><div className="flex items-center gap-2 text-[10px] font-medium uppercase tracking-[0.08em] text-muted-foreground"><AlertTriangle className="size-3 text-rose-600" />{risk.severity} · {risk.timeHorizon}</div><p className="mt-1 line-clamp-2 text-xs leading-5">{risk.summary}</p></div>)}</div> : <p className="mt-2 text-xs text-muted-foreground">No material risk flagged.</p>}{uncertainty ? <p className="mt-2 border-t border-border/70 pt-2 text-xs leading-5 text-muted-foreground"><span className="font-medium text-foreground">Uncertainty:</span> {uncertainty}</p> : null}</section>
}

export function MarketDecisionWorkspace({ brief, items, onSelectEvent }: { brief?: MarketInformationBrief | null; items: MarketInformationNews[]; onSelectEvent: (clusterId: number) => void }) {
  return <section className="grid min-w-0 gap-4 lg:grid-cols-[minmax(0,1.55fr)_minmax(18rem,0.85fr)]"><WhatMattersToday brief={brief} items={items} onSelectEvent={onSelectEvent} /><aside className="min-w-0 space-y-4"><UpcomingTriggers /><ImpactMap items={items} /><RiskWatchlist brief={brief} /></aside></section>
}
