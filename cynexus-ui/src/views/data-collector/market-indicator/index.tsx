import { useCallback, useEffect, useMemo, useState } from "react"
import { CircleAlert, Radio, RefreshCw, Search } from "lucide-react"

import { WorkspaceShell } from "@/layout/workspace-shell"
import { notification } from "@/components/ui/notification"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { confirmMarketIndicatorWatchlist, fetchMarketIndicatorIndustryDetail, fetchMarketIndicatorReplayTimeline, fetchMarketIndicatorTransmission, fetchMarketIndicators, previewMarketIndicatorWatchlist, type MarketIndicatorIndustryDetail, type MarketIndicatorIndustryRelation, type MarketIndicatorReplayTimelineResponse, type MarketIndicatorResponse, type MarketIndicatorSnapshot, type MarketIndicatorTransmissionResponse } from "@/services/market/indicator"
import { EventFeed } from "./components/event-feed"
import { IndicatorTable } from "./components/indicator-table"
import { IndustryIndicatorTable } from "./components/industry-indicator-table"
import { IndustryDetailPanel } from "./components/industry-detail-panel"
import { LimitBoardList } from "./components/limit-board-list"
import { TransmissionPanel } from "./components/transmission-panel"
import { MarketContextStrip } from "./components/market-context-strip"
import { ReplayTimeline } from "./components/replay-timeline"
import { ReplayReviewPanel } from "./components/replay-review-panel"

/**
 * Maintenance Notice:
 * Before modifying this page, review the related documentation under:
 * - design/front/infra/index.md
 * - design/front/infra/market-indicator.detail.md
 * - design/front/reuse/reuse.md
 *
 * If this page's route, data source, API usage, component structure,
 * or reusable logic changes, update the corresponding design documents.
 */

export type IndicatorView = "focus" | "all" | "pool" | "limit_up" | "limit_down" | "rapid"
type IndicatorMode = "radar" | "review"

const VIEW_OPTIONS: Array<{ value: IndicatorView; label: string }> = [
  { value: "focus", label: "重点异动" },
  { value: "all", label: "全市场" },
  { value: "pool", label: "涨跌停池" },
  { value: "limit_up", label: "涨停" },
  { value: "limit_down", label: "跌停" },
  { value: "rapid", label: "快速变化" },
]

const EMPTY_SNAPSHOTS: MarketIndicatorSnapshot[] = []

function isRapid(row: MarketIndicatorSnapshot, rapidCodes: Set<string>) {
  return rapidCodes.has(row.code)
}

function filterRows(rows: MarketIndicatorSnapshot[], view: IndicatorView, rapidCodes: Set<string>) {
  const filtered = rows.filter((row) => {
    if (view === "limit_up") return row.limitStatus?.startsWith("limit_up")
    if (view === "limit_down") return row.limitStatus?.startsWith("limit_down")
    if (view === "pool") return row.limitStatus?.startsWith("limit_up") || row.limitStatus?.startsWith("limit_down")
    if (view === "rapid") return isRapid(row, rapidCodes)
    if (view === "focus") return (row.limitStatus != null && row.limitStatus !== "normal") || isRapid(row, rapidCodes)
    return true
  })

  return [...filtered].sort((left, right) => {
    if (view === "all") return Math.abs(right.changePct ?? 0) - Math.abs(left.changePct ?? 0)
    return Math.abs(right.changePct ?? 0) - Math.abs(left.changePct ?? 0)
  })
}

function formatTime(value?: string | null) {
  return value
    ? new Date(value).toLocaleTimeString("zh-CN", { hour: "2-digit", minute: "2-digit", second: "2-digit" })
    : "等待数据"
}

export default function MarketIndicatorPage() {
  const [payload, setPayload] = useState<MarketIndicatorResponse | null>(null)
  const [transmission, setTransmission] = useState<MarketIndicatorTransmissionResponse | null>(null)
  const [activeView, setActiveView] = useState<IndicatorView>("focus")
  const [mode, setMode] = useState<IndicatorMode>("radar")
  const [reviewDate, setReviewDate] = useState("")
  const [query, setQuery] = useState("")
  const [loading, setLoading] = useState(true)
  const [refreshing, setRefreshing] = useState(false)
  const [selectedIndustryCode, setSelectedIndustryCode] = useState<string | null>(null)
  const [industryDetail, setIndustryDetail] = useState<MarketIndicatorIndustryDetail | null>(null)
  const [industryDetailLoading, setIndustryDetailLoading] = useState(false)
  const [replay, setReplay] = useState<MarketIndicatorReplayTimelineResponse | null>(null)
  const [replayLoading, setReplayLoading] = useState(false)

  const load = useCallback(async (manual = false) => {
    if (manual) setRefreshing(true)
    else setLoading(true)
    try {
      const next = await fetchMarketIndicators({ tradeDate: mode === "review" && reviewDate ? reviewDate : undefined, status: "all", limit: 2500 })
      setPayload(next)
      setTransmission(await fetchMarketIndicatorTransmission({ tradeDate: next.tradeDate ?? undefined, limit: 30 }))
    } catch (error) {
      notification.danger({ title: "市场指标暂不可用", description: error instanceof Error ? error.message : String(error) })
    } finally {
      setLoading(false)
      setRefreshing(false)
    }
  }, [mode, reviewDate])

  const loadIndustryDetail = useCallback(async (industryCode: string, page = 0) => {
    setIndustryDetailLoading(true)
    try {
      setIndustryDetail(await fetchMarketIndicatorIndustryDetail({ industryCode, tradeDate: payload?.tradeDate ?? undefined, page, pageSize: 50 }))
    } catch (error) {
      notification.danger({ title: "行业成分暂不可用", description: error instanceof Error ? error.message : String(error) })
    } finally {
      setIndustryDetailLoading(false)
    }
  }, [payload?.capturedAt, payload?.tradeDate])

  const replayTradeDate = mode === "review" ? (reviewDate || payload?.tradeDate || "") : ""
  const loadReplay = useCallback(async () => {
    if (mode !== "review" || !replayTradeDate) return
    setReplayLoading(true)
    try {
      setReplay(await fetchMarketIndicatorReplayTimeline({
        tradeDate: replayTradeDate,
        industryCode: selectedIndustryCode ?? undefined,
        from: "09:30",
        to: "15:00",
        limit: selectedIndustryCode ? 4000 : 3000,
      }))
    } catch (error) {
      notification.danger({ title: "1m回放暂不可用", description: error instanceof Error ? error.message : String(error) })
      setReplay(null)
    } finally {
      setReplayLoading(false)
    }
  }, [mode, replayTradeDate, selectedIndustryCode])

  useEffect(() => {
    if (mode === "review" && replayTradeDate) void loadReplay()
    if (mode !== "review") setReplay(null)
  }, [loadReplay, mode, replayTradeDate])

  const saveNextDayWatchlist = useCallback(async () => {
    const advice = transmission?.advice ?? []
    const items = advice.slice(0, 20).map((item) => ({
      targetCode: item.targetCode ?? item.target_code,
      targetName: item.targetName ?? item.target_name ?? item.sectorName ?? item.sector_name,
      targetType: item.targetType ?? item.target_type ?? "sector",
      actionClass: item.actionClass ?? item.action_class,
      confidence: item.confidence,
      reason: "盘后传导复盘条件化参考",
      evidence: item.payload,
    }))
    if (!items.length) return
    try {
      const preview = await previewMarketIndicatorWatchlist({ targetTradeDate: payload?.tradeDate ?? undefined, items })
      if (!window.confirm(`将保存 ${preview.items.length} 个条件化参考到“Market Indicator 次日关注”，是否继续？`)) return
      await confirmMarketIndicatorWatchlist({ previewToken: preview.previewToken })
      notification.success({ title: "已保存次日关注清单", description: "仅保存辅助参考，不构成无条件买卖指令。" })
    } catch (error) {
      notification.danger({ title: "保存次日关注失败", description: error instanceof Error ? error.message : String(error) })
    }
  }, [payload?.tradeDate, transmission?.advice])

  useEffect(() => {
    const firstLoad = window.setTimeout(() => void load(), 0)
    const cadenceSeconds = Math.max(15, transmission?.runtime?.effectiveCadenceSeconds ?? transmission?.runtime?.targetCadenceSeconds ?? 30)
    const timer = mode === "radar" ? window.setInterval(() => void load(true), cadenceSeconds * 1_000) : undefined
    return () => {
      window.clearTimeout(firstLoad)
      if (timer) window.clearInterval(timer)
    }
  }, [load, mode, transmission?.runtime?.effectiveCadenceSeconds, transmission?.runtime?.targetCadenceSeconds])

  const industries = payload?.industries ?? []
  useEffect(() => {
    if (!selectedIndustryCode && industries[0]?.industryCode) setSelectedIndustryCode(industries[0].industryCode)
  }, [industries, selectedIndustryCode])

  useEffect(() => {
    if (selectedIndustryCode) void loadIndustryDetail(selectedIndustryCode)
  }, [loadIndustryDetail, selectedIndustryCode])

  const snapshotRows = payload?.snapshots ?? EMPTY_SNAPSHOTS
  const limitUpRows = payload?.limitUp ?? EMPTY_SNAPSHOTS
  const limitDownRows = payload?.limitDown ?? EMPTY_SNAPSHOTS
  const rapidCodes = useMemo(() => new Set((payload?.events ?? [])
    .filter((event) => event.eventType === "rapid_rise" || event.eventType === "rapid_fall")
    .map((event) => event.code)), [payload?.events])

  const rows = useMemo(() => {
    const normalized = query.trim().toLowerCase()
    const sourceRows = activeView === "limit_up"
      ? limitUpRows
      : activeView === "limit_down"
        ? limitDownRows
        : activeView === "pool"
          ? [...new Map([...limitUpRows, ...limitDownRows].map((row) => [row.code, row])).values()]
        : activeView === "focus"
          ? [...new Map([...snapshotRows, ...limitUpRows, ...limitDownRows].map((row) => [row.code, row])).values()]
          : snapshotRows
    return filterRows(sourceRows, activeView, rapidCodes).filter((row) => {
      if (!normalized) return true
      return `${row.code} ${row.name ?? ""} ${row.industry ?? ""}`.toLowerCase().includes(normalized)
    })
  }, [activeView, limitDownRows, limitUpRows, query, rapidCodes, snapshotRows])

  const capturedAt = payload?.capturedAt || payload?.breadth?.capturedAt
  const hasData = Boolean(payload?.breadth?.totalCount || payload?.snapshots?.length || payload?.industries?.length)
  return (
    <WorkspaceShell sectionLabel="Data Collector" pageTitle="Market Indicator">
      <div className="space-y-5">
        <header className="flex flex-col justify-between gap-4 border-b border-border/60 pb-4 lg:flex-row lg:items-end">
          <div className="min-w-0">
            <div className="flex items-center gap-2 text-xs font-medium text-muted-foreground">
              <Radio className="size-3.5 text-information" />
              <span>Market Indicator</span>
              <span className="text-border">/</span>
              <span>盘中传导与盘后复盘</span>
            </div>
            <h1 className="mt-2 text-2xl font-semibold tracking-tight text-foreground">盘面传导</h1>
            <p className="mt-1 max-w-2xl text-sm text-muted-foreground">{mode === "radar" ? "快速雷达发现池内线索，全市场确认板块响应；单股异动不会直接等同于板块传导。" : "按交易日回看已保存的板块 Episode、条件化参考和结果评价；不把未来信息带回事件时刻。"}</p>
          </div>
          <div className="flex flex-wrap items-center justify-end gap-2">
            <Tabs value={mode} onValueChange={(value) => setMode(value as IndicatorMode)}>
              <TabsList variant="line" className="h-8">
                <TabsTrigger value="radar" className="px-3 text-xs">盘中雷达</TabsTrigger>
                <TabsTrigger value="review" className="px-3 text-xs">盘后复盘</TabsTrigger>
              </TabsList>
            </Tabs>
            {mode === "review" ? <Input type="date" value={reviewDate} onChange={(event) => setReviewDate(event.target.value)} className="h-8 w-[145px] text-xs" aria-label="选择复盘交易日" /> : null}
            <Button type="button" variant="outline" size="sm" onClick={() => void load(true)} disabled={refreshing} title={mode === "review" ? "加载复盘数据" : "刷新市场指标"}>
              <RefreshCw className={refreshing ? "animate-spin" : undefined} />
              {mode === "review" ? "加载复盘" : "刷新数据"}
            </Button>
          </div>
        </header>

        <div className="flex flex-wrap items-center justify-between gap-3 rounded-lg border border-border/60 bg-muted/25 px-3 py-2.5 text-xs">
          <div className="flex flex-wrap items-center gap-x-4 gap-y-2 text-muted-foreground">
            <span className="inline-flex items-center gap-2 font-medium text-foreground"><span className={`size-1.5 rounded-full ${hasData ? "bg-emerald-500" : "bg-amber-500"}`} />{hasData ? `${payload?.scope === "priority" ? "池内线索" : "全市场快照"}可用` : "等待市场快照"}</span>
            <span>交易日 <strong className="font-mono font-medium text-foreground">{payload?.tradeDate || "-"}</strong></span>
            <span>采集时间 <strong className="font-mono font-medium text-foreground">{formatTime(capturedAt)}</strong></span>
            <span>来源 <strong className="font-mono font-medium text-foreground">{payload?.source || "-"}</strong></span>
            <span>范围 <strong className="font-medium text-foreground">{payload?.scope === "priority" ? "历史涨跌停优先池" : "全市场"}</strong></span>
            <span>覆盖 <strong className="font-mono font-medium text-foreground">{payload?.coverageRatio != null ? `${(payload.coverageRatio * 100).toFixed(1)}%` : `${payload?.snapshotCount ?? 0} 只`}</strong></span>
          </div>
          <span className="text-muted-foreground">页面按后端有效采集节奏刷新；不再以 5 秒重复拉取未更新的全市场大包</span>
        </div>

        {payload?.ok === false ? <div className="flex items-start gap-2 rounded-lg border border-amber-200 bg-amber-50 px-3 py-2.5 text-sm text-amber-800 dark:border-amber-900/60 dark:bg-amber-950/20 dark:text-amber-300"><CircleAlert className="mt-0.5 size-4 shrink-0" /><span>{payload.error || "当前没有可用的市场指标数据，请稍后重试。"}</span></div> : null}

        <MarketContextStrip overview={payload} transmission={transmission} />

        {mode === "review" ? <>
          <div className="relative">{replayLoading ? <div className="absolute right-4 top-4 z-10 text-[11px] text-muted-foreground">正在读取回放证据…</div> : null}<ReplayTimeline data={replay} industryName={industries.find((item) => item.industryCode === selectedIndustryCode)?.industryName} /></div>
          <ReplayReviewPanel tradeDate={replayTradeDate} />
        </> : null}

        <TransmissionPanel data={transmission} overview={payload} loading={loading} mode={mode} onSaveNextDay={() => void saveNextDayWatchlist()} />

        <section className="grid min-w-0 gap-4 xl:grid-cols-[minmax(0,0.95fr)_minmax(0,1.05fr)]" aria-label="Industry research">
          <IndustryIndicatorTable rows={industries} selectedIndustryCode={selectedIndustryCode} onSelect={(row) => { if (row.industryCode) setSelectedIndustryCode(row.industryCode) }} />
          <IndustryDetailPanel
            industry={industries.find((row) => row.industryCode === selectedIndustryCode)}
            detail={industryDetail}
            loading={industryDetailLoading}
            onPageChange={(page) => { if (selectedIndustryCode) void loadIndustryDetail(selectedIndustryCode, page) }}
            onSelectRelation={(relation: MarketIndicatorIndustryRelation) => setSelectedIndustryCode(relation.industryCode)}
          />
        </section>

        <section className="grid min-w-0 gap-4 xl:grid-cols-[minmax(0,1fr)_minmax(360px,0.75fr)]" aria-label="Limit boards">
          <LimitBoardList limitUp={payload?.limitUp ?? []} limitDown={payload?.limitDown ?? []} />
          <div className="rounded-lg border border-border/70 bg-muted/20 px-4 py-4 text-xs text-muted-foreground"><div className="flex items-center justify-between gap-3"><span className="font-medium text-foreground">市场全量快照</span><span className="font-mono tabular-nums">{payload?.snapshotCount ?? 0} 只</span></div><p className="mt-2 leading-5">优先全市场采集；历史涨跌停池仅作为优先级和异常兜底，不替代完整行业成分股确认。</p></div>
        </section>

        <section className="grid min-w-0 gap-4 xl:grid-cols-[minmax(0,1fr)_360px]" aria-label="Indicator workbench">
          <div className="min-w-0 overflow-hidden rounded-lg border border-border/70 bg-card">
            <div className="flex flex-col gap-3 border-b border-border/60 px-4 py-3 sm:flex-row sm:items-center sm:justify-between">
              <div>
                <h2 className="text-sm font-semibold">个股异动监控</h2>
                <p className="mt-1 text-xs text-muted-foreground">按涨跌幅、涨跌停状态和快速变化筛出下一步要看的标的。</p>
              </div>
              <label className="relative block w-full sm:w-52">
                <Search className="pointer-events-none absolute left-2.5 top-1/2 size-3.5 -translate-y-1/2 text-muted-foreground" />
                <Input value={query} onChange={(event) => setQuery(event.target.value)} className="h-8 pl-8 text-xs" placeholder="搜索代码、名称或行业" aria-label="搜索代码、名称或行业" />
              </label>
            </div>
            <Tabs value={activeView} onValueChange={(value) => setActiveView(value as IndicatorView)}>
              <TabsList variant="line" className="w-full justify-start rounded-none border-b border-border/60 px-3 pt-1">
                {VIEW_OPTIONS.map((item) => <TabsTrigger key={item.value} value={item.value} className="flex-none px-3 text-xs">{item.label}</TabsTrigger>)}
              </TabsList>
              <TabsContent value={activeView} className="m-0"><IndicatorTable rows={rows} loading={loading} view={activeView} /></TabsContent>
            </Tabs>
          </div>
          <EventFeed events={payload?.events ?? []} />
        </section>
      </div>
    </WorkspaceShell>
  )
}
