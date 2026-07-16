import { Activity, AlertTriangle, Gauge, Radio } from "lucide-react"
import { Badge } from "@/components/ui/badge"
import type {
  MarketBriefContent,
  MarketBriefDirection,
  MarketBriefKeyEvent,
  MarketInformationBrief,
} from "@/services/information"

function parseBrief(value?: MarketInformationBrief | null): MarketBriefContent | null {
  if (!value?.contentJson) return null
  try { return JSON.parse(value.contentJson) as MarketBriefContent } catch { return null }
}

function label(value?: string | null) {
  return (value || "unknown").replaceAll("_", " ")
}

function toneClass(direction: MarketBriefDirection | string) {
  if (direction === "positive") return "bg-emerald-500/10 text-emerald-700 dark:text-emerald-300"
  if (direction === "negative") return "bg-rose-500/10 text-rose-700 dark:text-rose-300"
  if (direction === "mixed") return "bg-amber-500/10 text-amber-700 dark:text-amber-300"
  return "bg-muted text-muted-foreground"
}

function tone(content: MarketBriefContent) {
  if (typeof content.marketTone === "string") return { direction: content.marketTone, confidence: null, rationale: "" }
  return {
    direction: content.marketTone?.direction ?? "neutral",
    confidence: content.marketTone?.confidence ?? null,
    rationale: content.marketTone?.rationale ?? "",
  }
}

type BriefEventPreview = Pick<MarketBriefKeyEvent, "title" | "summary" | "affectedScopes"> & {
  materiality?: MarketBriefKeyEvent["materiality"]
}

function eventValue(value: MarketBriefKeyEvent | string): BriefEventPreview {
  return typeof value === "string" ? { title: value, summary: "", affectedScopes: [] as string[] } : value
}

function Metric({ label: metricLabel, value, detail }: { label: string; value: string; detail?: string }) {
  return <div className="min-w-0 border-l border-border/70 pl-3 first:border-l-0 first:pl-0"><div className="text-[10px] font-medium uppercase tracking-[0.08em] text-muted-foreground">{metricLabel}</div><div className="mt-1 truncate text-sm font-semibold text-foreground">{value}</div>{detail ? <div className="mt-0.5 truncate text-[11px] text-muted-foreground">{detail}</div> : null}</div>
}

export function MarketBrief({ brief }: { brief?: MarketInformationBrief | null }) {
  const content = parseBrief(brief)
  if (!content) return <section className="rounded-xl border border-border/70 bg-card px-4 py-3"><div className="flex items-center gap-2.5"><Radio className="size-4 text-muted-foreground" /><div><h2 className="text-sm font-semibold">Market pulse</h2><p className="text-xs text-muted-foreground">Awaiting high-value event analysis.</p></div><Badge variant="secondary" className="ml-auto border-0 text-xs">Pending</Badge></div></section>

  const marketTone = tone(content)
  const events = (content.keyEvents ?? []).slice(0, 4).map(eventValue)
  const focusScopes = [...new Set(events.flatMap((event) => event.affectedScopes ?? []))].slice(0, 4)
  const eventCount = content.analysisWindow?.eventCount ?? content.eventReferences?.length ?? events.length
  const sourceCount = Object.keys(content.sourceCoverage ?? {}).length
  const risk = content.riskSignals?.[0]

  return <section className="rounded-xl border border-border/70 bg-card px-4 py-3 shadow-none">
    <div className="flex flex-wrap items-center justify-between gap-3 border-b border-border/70 pb-3">
      <div className="flex min-w-0 items-center gap-2.5"><Activity className="size-4 shrink-0 text-primary" /><h2 className="text-sm font-semibold">Market pulse</h2><Badge variant="secondary" className={`border-0 capitalize ${toneClass(marketTone.direction)}`}>{label(marketTone.direction)}</Badge>{marketTone.confidence !== null ? <span className="text-xs tabular-nums text-muted-foreground">{Math.round(marketTone.confidence * 100)}% confidence</span> : null}</div>
      <div className="text-xs text-muted-foreground">{brief?.briefDate || "-"} · updated {brief?.updatedAt ? new Date(brief.updatedAt).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" }) : "-"}</div>
    </div>

    <div className="grid gap-3 border-b border-border/70 py-3 sm:grid-cols-4">
      <Metric label="Market status" value={label(marketTone.direction)} detail={marketTone.rationale || "Current read from analyzed events"} />
      <Metric label="Key events" value={String(eventCount)} detail={`${events.filter((event) => event.materiality === "high").length} high impact in view`} />
      <Metric label="Focus industries" value={focusScopes.length ? focusScopes.slice(0, 2).join(" · ") : "Not identified"} detail={focusScopes.length > 2 ? `+${focusScopes.length - 2} more scopes` : "Deterministic and AI signals"} />
      <Metric label="Sources" value={String(sourceCount)} detail="Current analysis coverage" />
    </div>

    <div className="grid gap-4 pt-3 lg:grid-cols-[minmax(0,1.35fr)_minmax(16rem,0.85fr)_minmax(14rem,0.75fr)]">
      <div className="min-w-0"><div className="mb-1 flex items-center gap-2 text-[11px] font-medium uppercase tracking-[0.08em] text-muted-foreground"><Radio className="size-3.5" />Read first</div><h3 className="line-clamp-2 text-base font-semibold leading-6">{content.headline || "Market information overview"}</h3><p className="mt-1.5 line-clamp-2 text-sm leading-5 text-muted-foreground">{content.executiveSummary || marketTone.rationale || "No concise market read is available yet."}</p></div>
      <div className="min-w-0 border-l border-border/70 pl-4"><div className="mb-1 flex items-center gap-2 text-[11px] font-medium uppercase tracking-[0.08em] text-muted-foreground"><Gauge className="size-3.5" />Primary focus</div><div className="flex flex-wrap gap-1.5">{focusScopes.length ? focusScopes.map((scope) => <span key={scope} className="rounded-md bg-muted px-2 py-1 text-[11px] text-foreground">{scope}</span>) : <span className="text-sm text-muted-foreground">No scope identified</span>}</div></div>
      <div className="min-w-0 border-l border-border/70 pl-4"><div className="mb-1 flex items-center gap-2 text-[11px] font-medium uppercase tracking-[0.08em] text-muted-foreground"><AlertTriangle className="size-3.5 text-rose-600" />Key risk</div><p className="line-clamp-3 text-sm leading-5">{risk?.summary || content.risks?.[0] || "No material risk flagged"}</p></div>
    </div>
  </section>
}
