import { useCallback, useEffect, useMemo, useState } from "react"
import { BookOpenText, BrainCircuit, CalendarDays, ExternalLink, FileText, LoaderCircle, MessageCircleMore, Newspaper, RefreshCw, ScrollText, Sparkles } from "lucide-react"

import { Button } from "@/components/ui/button"
import {
  fetchStockInformation,
  fetchStockInformationAnalysis,
  fetchStockInformationContent,
  runStockInformationAnalysis,
  type StockInformationAnalysis,
  type StockInformationContent,
  type StockInformationFeed,
  type StockInformationItem,
  type StockInformationKind,
} from "@/services/market/stock-information"

const TYPE_META: Record<StockInformationKind, { label: string; icon: typeof ScrollText; className: string }> = {
  announcement: { label: "Announcement", icon: ScrollText, className: "bg-amber-50 text-amber-700" },
  news: { label: "News", icon: Newspaper, className: "bg-sky-50 text-sky-700" },
  research: { label: "Research", icon: FileText, className: "bg-violet-50 text-violet-700" },
  interaction: { label: "Interaction", icon: MessageCircleMore, className: "bg-rose-50 text-rose-700" },
  roadshow: { label: "Roadshow", icon: CalendarDays, className: "bg-emerald-50 text-emerald-700" },
}

const FILTERS: Array<StockInformationKind | "all"> = ["all", "announcement", "news", "research", "interaction", "roadshow"]

function formatDate(value: string | null): { group: string; time: string } {
  if (!value) return { group: "Undated", time: "" }
  const compact = value.match(/^(\d{4})(\d{2})(\d{2})(?:\s*(\d{2}:\d{2}(?::\d{2})?))?$/)
  if (compact) return { group: `${compact[1]}-${compact[2]}-${compact[3]}`, time: compact[4] ?? "" }
  const normalized = value.replace(/\./g, "-")
  const parsed = new Date(normalized)
  if (Number.isNaN(parsed.getTime())) return { group: value.slice(0, 10), time: value.slice(11) }
  return {
    group: new Intl.DateTimeFormat("sv-SE", { timeZone: "Asia/Shanghai" }).format(parsed),
    time: new Intl.DateTimeFormat("en-GB", { timeZone: "Asia/Shanghai", hour: "2-digit", minute: "2-digit", hour12: false }).format(parsed),
  }
}

type AnalysisEvidence = { claim: string; quote: string }

function analysisDetails(value: StockInformationAnalysis | null): { summary: string | null; sentiment: string | null; materiality: string | null; relevance: string | null; horizon: string | null; confidence: string | null; evidence: AnalysisEvidence[] } {
  if (!value?.resultJson) return { summary: null, sentiment: null, materiality: null, relevance: null, horizon: null, confidence: null, evidence: [] }
  try {
    const parsed = JSON.parse(value.resultJson) as { summary?: unknown; sentiment?: unknown; materiality?: unknown; company_relevance?: unknown; impact_horizon?: unknown; confidence?: unknown; evidence?: unknown; key_facts?: unknown }
    const evidence = Array.isArray(parsed.evidence)
      ? parsed.evidence.flatMap((item) => {
        if (!item || typeof item !== "object") return []
        const value = item as { claim?: unknown; quote?: unknown }
        return typeof value.claim === "string" && typeof value.quote === "string" ? [{ claim: value.claim, quote: value.quote }] : []
      }).slice(0, 3)
      : Array.isArray(parsed.key_facts)
        ? parsed.key_facts.filter((item): item is string => typeof item === "string").slice(0, 3).map((claim) => ({ claim, quote: "" }))
        : []
    return {
      summary: typeof parsed.summary === "string" && parsed.summary.trim() ? parsed.summary.trim() : null,
      sentiment: typeof parsed.sentiment === "string" ? parsed.sentiment : null,
      materiality: typeof parsed.materiality === "string" ? parsed.materiality : null,
      relevance: typeof parsed.company_relevance === "string" ? parsed.company_relevance : null,
      horizon: typeof parsed.impact_horizon === "string" ? parsed.impact_horizon : null,
      confidence: typeof parsed.confidence === "string" ? parsed.confidence : null,
      evidence,
    }
  } catch {
    return { summary: value.resultJson, sentiment: null, materiality: null, relevance: null, horizon: null, confidence: null, evidence: [] }
  }
}

function InformationRow({ symbol, item }: { symbol: string; item: StockInformationItem }) {
  const meta = TYPE_META[item.kind]
  const Icon = meta.icon
  const date = formatDate(item.occurredAt)
  const [content, setContent] = useState<StockInformationContent | null>(null)
  const [analysis, setAnalysis] = useState<StockInformationAnalysis | null>(null)
  const [loadingContent, setLoadingContent] = useState(false)
  const [loadingAnalysis, setLoadingAnalysis] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const canRead = item.kind !== "roadshow" && Boolean(item.source && item.id)
  const ai = analysisDetails(analysis)

  const readContent = useCallback(async () => {
    setLoadingContent(true)
    setError(null)
    try {
      const next = await fetchStockInformationContent(symbol, item)
      setContent(next)
      if (next.status === "ready") setAnalysis(await fetchStockInformationAnalysis(symbol, item))
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Content request failed")
    } finally {
      setLoadingContent(false)
    }
  }, [item, symbol])

  const analyzeContent = useCallback(async () => {
    setLoadingAnalysis(true)
    setError(null)
    try {
      setAnalysis(await runStockInformationAnalysis(symbol, item))
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Analysis request failed")
    } finally {
      setLoadingAnalysis(false)
    }
  }, [item, symbol])

  const row = (
    <>
      <div className={`mt-0.5 flex size-7 shrink-0 items-center justify-center rounded-lg ${meta.className}`}>
        <Icon className="size-3.5" />
      </div>
      <div className="min-w-0 flex-1">
        <div className="flex min-w-0 items-center gap-2">
          <span className="shrink-0 text-[10px] font-medium text-muted-foreground">{date.time || "--:--"}</span>
          <span className="min-w-0 truncate text-sm font-medium text-foreground">{item.title}</span>
        </div>
        <div className="mt-1 flex min-w-0 flex-wrap items-center gap-x-2 gap-y-0.5 text-[11px] text-muted-foreground">
          <span>{meta.label}</span>
          {item.detail ? <span className="truncate">{item.detail}</span> : null}
          {item.source ? <span className="font-mono text-[10px]">{item.source}</span> : null}
        </div>
      </div>
      {item.href ? <ExternalLink className="mt-1 size-3.5 shrink-0 text-muted-foreground" /> : null}
    </>
  )

  return (
    <div className="px-3 py-2.5">
      <div className="flex min-w-0 items-start gap-2">
        {item.href ? <a className="flex min-w-0 flex-1 items-start gap-3 rounded-md transition hover:text-foreground" href={item.href} target="_blank" rel="noreferrer">{row}</a> : <div className="flex min-w-0 flex-1 items-start gap-3">{row}</div>}
        {canRead ? (
          <Button type="button" variant="ghost" size="icon" className="mt-0.5 size-7 shrink-0 text-muted-foreground hover:text-foreground" onClick={() => void readContent()} disabled={loadingContent} title="Read content">
            {loadingContent ? <LoaderCircle className="size-3.5 animate-spin" /> : <BookOpenText className="size-3.5" />}
          </Button>
        ) : null}
      </div>
      {content ? (
        <div className="ml-10 mt-2 border-l border-border/70 pl-3">
          {content.status === "ready" ? (
            <>
              <div className="max-h-64 overflow-y-auto whitespace-pre-wrap pr-2 text-xs leading-5 text-foreground/80">{content.text}</div>
              <div className="mt-2 flex items-center gap-2">
                <Button type="button" variant="secondary" size="sm" className="h-7 gap-1.5 px-2.5 text-[11px]" onClick={() => void analyzeContent()} disabled={loadingAnalysis}>
                  {loadingAnalysis ? <LoaderCircle className="size-3.5 animate-spin" /> : <BrainCircuit className="size-3.5" />}
                  Analyze
                </Button>
                {analysis?.status === "ready" ? <span className="text-[10px] text-muted-foreground">{analysis.modelCode || "AI"}</span> : null}
              </div>
              {ai.summary ? (
                <div className="mt-2 space-y-1.5 text-xs leading-5 text-muted-foreground">
                  <div className="flex flex-wrap items-center gap-1.5 text-[10px] font-medium uppercase tracking-wide text-muted-foreground">
                    {[ai.relevance, ai.horizon, ai.sentiment, ai.materiality, ai.confidence].filter(Boolean).map((value) => <span key={value}>{value}</span>)}
                  </div>
                  <p className="whitespace-pre-wrap">{ai.summary}</p>
                  {ai.evidence.length ? (
                    <div className="space-y-1 pt-0.5">
                      {ai.evidence.map((item) => <div key={`${item.claim}:${item.quote}`}><span className="text-foreground/80">{item.claim}</span>{item.quote ? <span className="block text-[11px] text-muted-foreground">"{item.quote}"</span> : null}</div>)}
                    </div>
                  ) : null}
                </div>
              ) : null}
              {analysis?.status === "failed" ? <p className="mt-2 text-[11px] text-destructive">{analysis.errorMessage || "Analysis failed"}</p> : null}
            </>
          ) : <p className="text-[11px] leading-5 text-muted-foreground">{content.message || content.status.replace("_", " ")}</p>}
        </div>
      ) : null}
      {error ? <p className="ml-10 mt-2 text-[11px] text-destructive">{error}</p> : null}
    </div>
  )
}

export function InformationTab({ symbol }: { symbol: string }) {
  const [feed, setFeed] = useState<StockInformationFeed | null>(null)
  const [loading, setLoading] = useState(false)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [filter, setFilter] = useState<StockInformationKind | "all">("all")

  const refresh = useCallback(async () => {
    setLoading(true)
    try {
      setFeed(await fetchStockInformation(symbol))
      setLoadError(null)
    } catch (error) {
      setLoadError(error instanceof Error ? error.message : "Information request failed")
    } finally {
      setLoading(false)
    }
  }, [symbol])

  useEffect(() => {
    void refresh()
  }, [refresh])

  const grouped = useMemo(() => {
    const result = new Map<string, StockInformationItem[]>()
    for (const item of feed?.items ?? []) {
      if (filter !== "all" && item.kind !== filter) continue
      const group = formatDate(item.occurredAt).group
      const rows = result.get(group) ?? []
      rows.push(item)
      result.set(group, rows)
    }
    return Array.from(result.entries())
  }, [feed, filter])

  return (
    <section className="min-w-0 space-y-3">
      <div className="flex min-w-0 items-center justify-between gap-3">
        <div className="min-w-0">
          <div className="flex items-center gap-2">
            <span className="flex size-7 shrink-0 items-center justify-center rounded-lg bg-slate-100 text-slate-600"><Newspaper className="size-3.5" /></span>
            <h2 className="text-sm font-semibold text-foreground">Information</h2>
          </div>
          <p className="mt-1 text-[11px] text-muted-foreground">Viewable announcements, news, research, investor interactions, and roadshows.</p>
        </div>
        <Button type="button" size="sm" variant="secondary" className="h-7 shrink-0 gap-1.5 px-2.5 text-[11px]" onClick={() => void refresh()} disabled={loading}>
          {loading ? <LoaderCircle className="size-3.5 animate-spin" /> : <RefreshCw className="size-3.5" />}
          Refresh
        </Button>
      </div>

      <div className="flex items-center gap-1 overflow-x-auto pb-1 [scrollbar-width:none] [&::-webkit-scrollbar]:hidden">
        {FILTERS.map((value) => {
          const active = filter === value
          const count = value === "all" ? feed?.items.length ?? 0 : feed?.counts[value] ?? 0
          const label = value === "all" ? "All" : TYPE_META[value].label
          return (
            <button
              key={value}
              type="button"
              className={`h-7 shrink-0 rounded-md px-2.5 text-[11px] font-medium transition ${active ? "bg-foreground text-background" : "bg-muted/70 text-muted-foreground hover:bg-muted hover:text-foreground"}`}
              onClick={() => setFilter(value)}
            >
              {label} {count}
            </button>
          )
        })}
      </div>

      {loading && !feed ? (
        <div className="space-y-2 rounded-xl bg-muted/40 p-3">
          {[0, 1, 2, 3].map((index) => <div key={index} className="h-12 animate-pulse rounded-lg bg-muted" />)}
        </div>
      ) : grouped.length ? (
        <div className="space-y-4">
          {grouped.map(([date, items]) => (
            <div key={date} className="min-w-0">
              <div className="mb-1 flex items-center gap-2 px-3 text-[11px] font-medium text-muted-foreground">
                <span>{date}</span><span className="h-px flex-1 bg-border/70" />
              </div>
              <div className="divide-y divide-border/70 rounded-xl bg-muted/35 py-1">
                {items.map((item) => <InformationRow key={item.id} symbol={symbol} item={item} />)}
              </div>
            </div>
          ))}
        </div>
      ) : (
        <div className="flex min-h-[240px] flex-col items-center justify-center rounded-xl bg-muted/40 px-6 text-center">
          <Sparkles className="size-4 text-muted-foreground" />
          <p className="mt-2 text-sm font-medium text-foreground">{loadError ? "Information is unavailable" : "No information available"}</p>
          <p className="mt-1 text-[11px] text-muted-foreground">{loadError ?? "This source may not have published items for the selected stock yet."}</p>
        </div>
      )}
    </section>
  )
}
