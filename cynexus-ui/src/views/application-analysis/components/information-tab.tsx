import { useCallback, useEffect, useMemo, useState } from "react"
import { CalendarDays, ExternalLink, FileText, LoaderCircle, Newspaper, RefreshCw, ScrollText, Sparkles } from "lucide-react"

import { Button } from "@/components/ui/button"
import {
  fetchStockInformation,
  type StockInformationFeed,
  type StockInformationItem,
  type StockInformationKind,
} from "@/services/market/stock-information"

const TYPE_META: Record<StockInformationKind, { label: string; icon: typeof ScrollText; className: string }> = {
  announcement: { label: "Announcement", icon: ScrollText, className: "bg-amber-50 text-amber-700" },
  news: { label: "News", icon: Newspaper, className: "bg-sky-50 text-sky-700" },
  research: { label: "Research", icon: FileText, className: "bg-violet-50 text-violet-700" },
  roadshow: { label: "Roadshow", icon: CalendarDays, className: "bg-emerald-50 text-emerald-700" },
}

const FILTERS: Array<StockInformationKind | "all"> = ["all", "announcement", "news", "research", "roadshow"]

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

function InformationRow({ item }: { item: StockInformationItem }) {
  const meta = TYPE_META[item.kind]
  const Icon = meta.icon
  const date = formatDate(item.occurredAt)
  const content = (
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

  return item.href ? (
    <a className="flex min-w-0 items-start gap-3 rounded-lg px-3 py-2.5 transition hover:bg-muted/70" href={item.href} target="_blank" rel="noreferrer">
      {content}
    </a>
  ) : (
    <div className="flex min-w-0 items-start gap-3 rounded-lg px-3 py-2.5">{content}</div>
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

  const failures = Object.entries(feed?.failures ?? {})

  return (
    <section className="min-w-0 space-y-3">
      <div className="flex min-w-0 items-center justify-between gap-3">
        <div className="min-w-0">
          <div className="flex items-center gap-2">
            <span className="flex size-7 shrink-0 items-center justify-center rounded-lg bg-slate-100 text-slate-600"><Newspaper className="size-3.5" /></span>
            <h2 className="text-sm font-semibold text-foreground">Information</h2>
          </div>
          <p className="mt-1 text-[11px] text-muted-foreground">Announcements, news, research, and roadshows for the selected stock.</p>
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
                {items.map((item) => <InformationRow key={item.id} item={item} />)}
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

      {failures.length ? <p className="text-[11px] text-muted-foreground">Unavailable sources: {failures.map(([kind]) => TYPE_META[kind as StockInformationKind].label).join(", ")}</p> : null}
    </section>
  )
}
