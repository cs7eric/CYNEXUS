import { Activity, Filter, Radio, RefreshCw } from "lucide-react"
import { useCallback, useEffect, useState } from "react"
import { Button } from "@/components/ui/button"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { WorkspaceShell } from "@/layout/workspace-shell"
import {
  fetchMarketInformation,
  fetchMarketInformationEvent,
  fetchMarketInformationAnalysisStatuses,
  requestMarketInformationAnalysis,
  type MarketInformationEventDetail,
  type MarketInformationFeed,
} from "@/services/information"
import { MarketBrief } from "./components/market-brief"
import { MarketDecisionWorkspace } from "./components/market-decision-layers"
import { MarketEventSheet } from "./components/market-event-sheet"
import { MarketInformationTable } from "./components/market-information-table"

/**
 * Maintenance Notice:
 * Before modifying this page, review the related documentation under:
 * - design/front/infra/index.md
 * - design/front/infra/information.detail.md
 * - design/front/reuse/reuse.md
 *
 * If this page's route, data source, API usage, component structure,
 * or reusable logic changes, update the corresponding design documents.
 */

const wireSources = new Set(["cls.telegraph", "eastmoney.7x24"])

type FeedView = "focus" | "all" | "macro" | "company" | "industry" | "calendar" | "wire"

const feedViewOptions: Array<{ value: FeedView; label: string }> = [
  { value: "focus", label: "Focus" },
  { value: "all", label: "All" },
  { value: "macro", label: "Macro / Policy" },
  { value: "company", label: "Company" },
  { value: "industry", label: "Industry" },
  { value: "calendar", label: "Calendar / Events" },
  { value: "wire", label: "7x24" },
]

function publishedAt(value?: string | null) {
  if (!value) return "-"
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString([], { month: "2-digit", day: "2-digit", hour: "2-digit", minute: "2-digit" })
}

function priorityRank(item: MarketInformationFeed["news"][number]) {
  const materiality = item.materiality === "high" ? 0 : item.materiality === "medium" ? 1 : 2
  const category = item.category === "Policy" ? 0 : item.category === "Regulation" ? 1 : item.category === "Industry" ? 2 : item.category === "Macro" ? 3 : 4
  return materiality * 10 + category
}

function RawWireList({ items, onSelectEvent }: { items: MarketInformationFeed["news"]; onSelectEvent: (clusterId: number) => void }) {
  return <details className="border-t border-border/60 pt-3">
    <summary className="flex cursor-pointer list-none items-center justify-between gap-3 text-sm font-semibold [&::-webkit-details-marker]:hidden"><span className="flex items-center gap-2"><Radio className="size-4 text-muted-foreground" />Raw 7x24 feed</span><span className="text-xs font-normal text-muted-foreground">{items.length} items on this page · expand for live wires</span></summary>
    <div className="mt-2 divide-y divide-border/60">
      {items.length ? items.map((item) => <div key={`${item.source}-${item.id}`} className="flex items-start gap-3 py-2.5"><span className="w-24 shrink-0 text-[11px] tabular-nums text-muted-foreground">{publishedAt(item.publishedAt)}</span><button className="min-w-0 flex-1 text-left text-sm leading-5 text-foreground hover:text-primary" onClick={() => item.clusterId && onSelectEvent(item.clusterId)}>{item.title}</button><span className="shrink-0 text-[11px] text-muted-foreground">{item.source === "cls.telegraph" ? "CLS" : "EastMoney"}</span></div>) : <p className="py-3 text-sm text-muted-foreground">No raw wires on this page.</p>}
    </div>
  </details>
}

export default function MarketInformationPage() {
  const [feed, setFeed] = useState<MarketInformationFeed | null>(null)
  const [page, setPage] = useState(1)
  const [impactIndustryCode, setImpactIndustryCode] = useState("all")
  const [impactStatus, setImpactStatus] = useState("all")
  const [feedView, setFeedView] = useState<FeedView>("focus")
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [eventOpen, setEventOpen] = useState(false)
  const [eventLoading, setEventLoading] = useState(false)
  const [eventDetail, setEventDetail] = useState<MarketInformationEventDetail | null>(null)
  const clusterIds = (feed?.newsPage?.items ?? []).filter((item) => ["PENDING", "RUNNING"].includes(item.analysisStatus ?? "")).map((item) => item.clusterId).filter((item): item is number => item != null)
  const clusterIdsKey = clusterIds.join(",")
  const pageItems = feed?.newsPage?.items ?? feed?.news ?? []
  const mappedCount = pageItems.filter((item) => item.impactStatus === "Direct" || item.impactStatus === "Inferred").length
  const keyCount = pageItems.filter((item) => item.materiality === "high").length
  const viewItems = feedView === "focus" ? pageItems.filter((item) => !wireSources.has(item.source ?? "")) : feedView === "wire" ? pageItems.filter((item) => wireSources.has(item.source ?? "")) : feedView === "macro" ? pageItems.filter((item) => ["Macro", "Policy", "Regulation"].includes(item.category ?? "")) : feedView === "company" ? pageItems.filter((item) => item.kind === "announcement" || Boolean(item.issuer) || item.source?.startsWith("cninfo")) : feedView === "industry" ? pageItems.filter((item) => item.category === "Industry") : feedView === "calendar" ? pageItems.filter((item) => item.kind === "announcement" || item.category === "Policy") : pageItems
  const filteredItems = impactStatus === "all" ? viewItems : viewItems.filter((item) => impactStatus === "mapped" ? item.impactStatus !== "Unmapped" : item.impactStatus === "Unmapped")
  const priorityItems = filteredItems.filter((item) => !wireSources.has(item.source ?? "") || item.materiality === "high").sort((left, right) => priorityRank(left) - priorityRank(right) || new Date(right.publishedAt ?? 0).getTime() - new Date(left.publishedAt ?? 0).getTime())
  const rawWireItems = filteredItems.filter((item) => wireSources.has(item.source ?? "")).sort((left, right) => new Date(right.publishedAt ?? 0).getTime() - new Date(left.publishedAt ?? 0).getTime())
  const load = useCallback(async () => { setLoading(true); setError(null); try { setFeed(await fetchMarketInformation(page, 30, impactIndustryCode === "all" ? undefined : impactIndustryCode)) } catch (e) { setError(e instanceof Error ? e.message : "Unable to load market information") } finally { setLoading(false) } }, [page, impactIndustryCode])
  useEffect(() => {
    const timer = window.setTimeout(() => void load(), 0)
    return () => window.clearTimeout(timer)
  }, [load])
  useEffect(() => {
    if (!clusterIdsKey) return
    const activeClusterIds = clusterIdsKey.split(",").map(Number)
    const timer = window.setInterval(() => {
      void fetchMarketInformationAnalysisStatuses(activeClusterIds).then((statuses) => {
        const byCluster = new Map(statuses.map((status) => [status.clusterId, status]))
        setFeed((current) => current?.newsPage ? { ...current, newsPage: { ...current.newsPage, items: current.newsPage.items.map((item) => item.clusterId && byCluster.has(item.clusterId) ? { ...item, ...byCluster.get(item.clusterId) } : item) } } : current)
      })
    }, 5000)
    return () => window.clearInterval(timer)
  }, [clusterIdsKey])
  useEffect(() => {
    const clusterId = eventDetail?.clusterId
    const status = eventDetail?.analysisStatus ?? ""
    if (!eventOpen || clusterId == null || !["PENDING", "RUNNING"].includes(status)) return
    const timer = window.setInterval(() => {
      void fetchMarketInformationEvent(clusterId).then((next) => setEventDetail(next))
    }, 4000)
    return () => window.clearInterval(timer)
  }, [eventDetail?.analysisStatus, eventDetail?.clusterId, eventOpen])
  const openEvent = useCallback(async (clusterId: number) => {
    setEventOpen(true); setEventLoading(true); setEventDetail(null)
    try { setEventDetail(await fetchMarketInformationEvent(clusterId)) }
    catch (e) { setError(e instanceof Error ? e.message : "Unable to load event analysis") }
    finally { setEventLoading(false) }
  }, [])
  const analyzeEvent = useCallback(async () => {
    if (!eventDetail) return
    await requestMarketInformationAnalysis(eventDetail.clusterId, Boolean(eventDetail.analysisJson))
    setEventDetail({ ...eventDetail, analysisStatus: "PENDING" })
    void load()
  }, [eventDetail, load])
  return <WorkspaceShell sectionLabel="Information" sectionUrl="/information" pageTitle="Market Information">
    <main className="flex min-h-[calc(100vh-8rem)] flex-col gap-5">
      <header className="flex flex-wrap items-center justify-between gap-3"><div className="flex items-center gap-2.5"><Activity className="size-5 text-primary" /><div><h1 className="text-xl font-semibold tracking-tight">Market Information</h1><p className="text-xs text-muted-foreground">Prioritized market signals, official releases, and industry events.</p></div></div><div className="flex items-center gap-3"><span className="text-xs text-muted-foreground">{feed?.queriedAt ? `Updated ${new Date(feed.queriedAt).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}` : "Awaiting snapshot"}</span><Button variant="secondary" size="sm" onClick={() => void load()} disabled={loading}><RefreshCw className={`size-3.5 ${loading ? "animate-spin" : ""}`} />Refresh</Button></div></header>
      {error ? <div className="rounded-lg bg-rose-500/10 px-4 py-3 text-sm text-rose-700">{error}</div> : null}
      <MarketBrief brief={feed?.brief} />
      {feed ? <MarketDecisionWorkspace brief={feed.brief} items={priorityItems} onSelectEvent={(clusterId) => void openEvent(clusterId)} /> : null}
      <section className="rounded-xl border border-border/70 bg-card px-3 py-2.5 shadow-none">
        <div className="flex flex-wrap items-center justify-between gap-3"><div className="flex min-w-0 items-center gap-3"><div className="flex items-center gap-1.5 text-xs font-medium text-muted-foreground"><Filter className="size-3.5" />View</div><div className="flex max-w-full flex-wrap items-center gap-1" role="tablist" aria-label="Information views">{feedViewOptions.map((option) => <Button key={option.value} type="button" size="sm" variant={feedView === option.value ? "secondary" : "ghost"} className="h-7 px-2.5 text-xs" onClick={() => { setFeedView(option.value); setPage(1) }}>{option.label}</Button>)}</div></div><div className="flex items-center gap-2"><span className="hidden text-xs text-muted-foreground sm:inline">{feed?.newsPage?.total ?? pageItems.length} records · {mappedCount} mapped · {keyCount} high impact</span><Select value={impactStatus} onValueChange={setImpactStatus}><SelectTrigger className="h-7 w-28 border-0 bg-muted/45 px-2 text-xs shadow-none"><SelectValue /></SelectTrigger><SelectContent><SelectItem value="all">All impact</SelectItem><SelectItem value="mapped">Mapped only</SelectItem><SelectItem value="unmapped">Unmapped</SelectItem></SelectContent></Select><Select value={impactIndustryCode} onValueChange={(value) => { setImpactIndustryCode(value); setPage(1) }}><SelectTrigger className="h-7 w-44 border-0 bg-muted/45 px-2 text-xs shadow-none"><SelectValue placeholder="All industries" /></SelectTrigger><SelectContent><SelectItem value="all">All industries</SelectItem>{(feed?.impactIndustries ?? []).map((industry) => <SelectItem key={industry.tdxCode} value={industry.tdxCode}>{industry.tdxName}</SelectItem>)}</SelectContent></Select></div></div>
      </section>
      {loading && !feed ? <div className="space-y-3 rounded-xl border border-border/70 bg-card px-4 py-5" aria-busy="true"><div className="h-4 w-32 animate-pulse rounded bg-muted motion-reduce:animate-none" /><div className="h-8 w-3/4 animate-pulse rounded bg-muted motion-reduce:animate-none" /><div className="h-4 w-full animate-pulse rounded bg-muted motion-reduce:animate-none" /><div className="h-4 w-2/3 animate-pulse rounded bg-muted motion-reduce:animate-none" /></div> : <>
        <section>
          <div className="mb-2 flex items-center justify-between gap-3"><div><h2 className="text-sm font-semibold">Full information feed</h2><p className="text-xs text-muted-foreground">The complete paginated feed with source, impact, and analysis state.</p></div><span className="text-xs tabular-nums text-muted-foreground">{priorityItems.length} visible</span></div>
          <MarketInformationTable items={priorityItems} pagination={feedView === "focus" && impactStatus === "all" ? feed?.newsPage : undefined} loading={loading} onPageChange={setPage} onSelectEvent={(clusterId) => void openEvent(clusterId)} />
        </section>
        <RawWireList items={rawWireItems} onSelectEvent={(clusterId) => void openEvent(clusterId)} />
      </>}
      <MarketEventSheet open={eventOpen} loading={eventLoading} detail={eventDetail} onOpenChange={setEventOpen} onAnalyze={() => void analyzeEvent()} />
    </main>
  </WorkspaceShell>
}
