import { useCallback, useEffect, useMemo, useState } from "react"
import type { KeyboardEvent } from "react"
import { ChartNoAxesCombined, ChevronRight, CircleAlert, ClipboardCheck, RefreshCw, Upload, WalletCards } from "lucide-react"
import { Link, useSearchParams } from "react-router-dom"
import { WorkspaceShell } from "@/layout/workspace-shell"
import { Button } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import { notification } from "@/components/ui/notification"
import { fetchPortfolioOverview } from "@/services/market/portfolio/portfolio"
import { fetchPortfolioReviews } from "@/services/market/portfolio/reviews"
import type { PortfolioOverview, PortfolioPosition, PortfolioReview } from "@/services/market/portfolio/types"
import { PortfolioReturnBreakdown } from "./components/portfolio-return-breakdown"
import { PortfolioAssetSnapshot } from "./components/portfolio-asset-snapshot"
import { PortfolioImportDrawer } from "./components/portfolio-import-drawer"
import { PortfolioPositionDrawer } from "./components/portfolio-position-drawer"
import { PortfolioReviewDrawer } from "./components/portfolio-review-drawer"
import { PortfolioPerformancePanel, PortfolioPerformanceSummary, PortfolioPerformanceToolbar, type PortfolioChartView } from "./components/portfolio-performance-panel"
import { usePortfolioPerformance } from "./components/portfolio-performance-state"

const money = (value: number | null | undefined) => value == null ? "--" : Number(value).toFixed(2)
const price = (value: number | null | undefined) => value == null ? "--" : Number(value).toFixed(3)
const signedMoney = (value: number | null | undefined) => value == null ? "--" : `${value > 0 ? "+" : ""}${Number(value).toFixed(2)}`
const percent = (value: number | null | undefined) => value == null ? "--" : `${(value * 100).toFixed(2)}%`

function pnlTone(value: number | null | undefined) {
  if (value == null || value === 0) return "text-muted-foreground"
  return value > 0 ? "text-rose-600" : "text-emerald-600"
}

function positionValue(position: PortfolioPosition, key: "marketValue" | "costBasis") {
  return Number(position[key] ?? 0)
}

export default function PersonalPortfolioPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const [overview, setOverview] = useState<PortfolioOverview | null>(null)
  const [reviews, setReviews] = useState<PortfolioReview[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [chartView, setChartView] = useState<PortfolioChartView>("calendar")
  const performanceState = usePortfolioPerformance()
  const importDrawerOpen = searchParams.get("imports") === "1"
  const selectedSymbol = searchParams.get("stock")
  const selectedInstrumentType = searchParams.get("instrumentType") === "ETF" ? "ETF" : "STOCK"
  const reviewDrawerOpen = searchParams.get("review") === "new"
  const reviewSymbol = searchParams.get("symbol")
  const reviewInstrumentType = searchParams.get("instrumentType") === "ETF" ? "ETF" : "STOCK"

  const setQueryState = useCallback((updates: Record<string, string | null>) => {
    const next = new URLSearchParams(searchParams)
    Object.entries(updates).forEach(([key, value]) => value == null ? next.delete(key) : next.set(key, value))
    setSearchParams(next, { replace: true })
  }, [searchParams, setSearchParams])

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const [nextOverview, nextReviews] = await Promise.all([fetchPortfolioOverview(), fetchPortfolioReviews()])
      setOverview(nextOverview)
      setReviews(nextReviews)
    } catch (cause) {
      const message = cause instanceof Error ? cause.message : String(cause)
      setError(message)
      notification.danger({ title: "持仓数据加载失败", description: message })
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    const timer = window.setTimeout(() => { void load() }, 0)
    return () => window.clearTimeout(timer)
  }, [load])

  const positions = useMemo(() => (overview?.positions ?? []).filter((position) => Number(position.quantity) > 0), [overview])
  const largestPosition = useMemo(() => positions.reduce<PortfolioPosition | null>((largest, position) => {
    if (!largest || positionValue(position, "marketValue") > positionValue(largest, "marketValue")) return position
    return largest
  }, null), [positions])

  function openPosition(position: PortfolioPosition) {
    setQueryState({ stock: position.symbol, instrumentType: position.instrumentType })
  }

  function handlePositionKeyDown(event: KeyboardEvent<HTMLTableRowElement>, position: PortfolioPosition) {
    if (event.key !== "Enter" && event.key !== " ") return
    event.preventDefault()
    openPosition(position)
  }

  return (
    <WorkspaceShell sectionLabel="Stock Overview" pageTitle="Personal Portfolio">
      <div className="flex w-full min-w-0 flex-col gap-5">
        <header className="flex flex-wrap items-end justify-between gap-4">
          <div className="min-w-0">
            <div className="flex items-center gap-2 text-xs font-medium text-muted-foreground"><WalletCards className="size-4" />个人持仓</div>
            <h1 className="mt-1 text-2xl font-semibold tracking-tight">持仓看板</h1>
            <p className="mt-1 max-w-2xl text-sm leading-6 text-muted-foreground">查看当前资产、持仓表现和交易复盘。点击任意持仓行查看买卖点与详情。</p>
          </div>
          <div className="flex flex-wrap gap-2"><Button onClick={() => setQueryState({ imports: "1" })}><Upload className="mr-2 size-4" />导入交易</Button><Button variant="outline" onClick={() => void load()} disabled={loading}><RefreshCw className={`mr-2 size-4 ${loading ? "animate-spin" : ""}`} />刷新</Button></div>
        </header>

        {error && !loading ? <div className="flex items-start gap-2 rounded-lg border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700"><CircleAlert className="mt-0.5 size-4 shrink-0" /><div><p className="font-medium">持仓数据暂不可用</p><p className="mt-1 text-xs break-words">{error}</p></div><Button variant="outline" size="sm" className="ml-auto" onClick={() => void load()}>重试</Button></div> : null}
        {!loading && overview?.dataQualityWarnings?.length ? <div className="flex items-start gap-2 rounded-lg border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-900"><CircleAlert className="mt-0.5 size-4 shrink-0" /><div><p className="font-medium">交易历史存在缺口</p><div className="mt-1 space-y-1 text-xs">{overview.dataQualityWarnings.map((warning, index) => <p key={`${warning.code}-${warning.symbol}-${index}`}>{warning.symbol}：{warning.message}</p>)}</div></div></div> : null}

        {loading ? <SnapshotSkeleton /> : <>
          {overview ? <PortfolioAssetSnapshot overview={overview} onSaved={() => void load()} /> : null}
          <section className="overflow-hidden rounded-lg border border-border/60 bg-card" aria-labelledby="performance-heading">
            <div className="flex flex-wrap items-start justify-between gap-3 border-b border-border/60 px-4 py-3.5">
              <div className="min-w-0">
                <div className="flex items-center gap-2"><ChartNoAxesCombined className="size-4 text-muted-foreground" aria-hidden="true" /><h2 id="performance-heading" className="text-base font-semibold">今日收益走势</h2></div>
                <p className="mt-1 text-xs text-muted-foreground">面积为当前视图累计收益，柱体为周期收益变化，红涨绿跌。</p>
              </div>
              <PortfolioPerformanceToolbar range={performanceState.range} onRangeChange={performanceState.setRange} chartView={chartView} onChartViewChange={setChartView} />
            </div>
            <div className="grid divide-y divide-border/60 xl:grid-cols-[minmax(0,4fr)_minmax(0,4fr)_minmax(220px,2fr)] xl:divide-x xl:divide-y-0">
              <PortfolioPerformancePanel state={performanceState} chartView={chartView} embedded onChartViewChange={setChartView} />
              {overview ? <PortfolioReturnBreakdown overview={overview} performance={performanceState.performance} range={performanceState.range} embedded /> : <div className="min-w-0" />}
              <PortfolioPerformanceSummary state={performanceState} embedded />
            </div>
          </section>

          <section className="overflow-hidden rounded-lg border border-border/60 bg-card" aria-labelledby="positions-heading">
            <div className="flex flex-wrap items-start justify-between gap-3 border-b border-border/60 px-4 py-3.5"><div><div className="flex items-center gap-2"><h2 id="positions-heading" className="text-base font-semibold">当前持仓</h2><span className="rounded-full bg-muted px-2 py-0.5 text-[11px] text-muted-foreground">{positions.length} 个标的</span></div><p className="mt-1 text-xs text-muted-foreground">点击任意一行打开详情 Drawer；行情使用市场数据源的最新可用价格。</p></div><div className="text-right text-xs text-muted-foreground">{largestPosition ? `最大持仓：${largestPosition.securityName ?? largestPosition.symbol}` : "暂无已确认持仓"}</div></div>
            <div className="overflow-x-auto">
              <table className="w-full min-w-[1020px] text-sm">
                <thead className="bg-muted/45 text-left text-xs text-muted-foreground"><tr><th className="px-4 py-3">标的</th><th className="px-4 py-3 text-right">持仓</th><th className="px-4 py-3 text-right">成本 / 最新价</th><th className="px-4 py-3 text-right">市值</th><th className="px-4 py-3 text-right">今日盈亏</th><th className="px-4 py-3 text-right">当前盈亏</th><th className="px-4 py-3 text-right">收益率</th><th className="w-10 px-3 py-3" /></tr></thead>
                <tbody className="divide-y divide-border/60">
                  {positions.map((position) => <tr key={`${position.instrumentType}-${position.symbol}`} role="button" tabIndex={0} aria-label={`打开 ${position.securityName ?? position.symbol} 持仓详情`} onClick={() => openPosition(position)} onKeyDown={(event) => handlePositionKeyDown(event, position)} className="group cursor-pointer transition-colors hover:bg-muted/35 focus-visible:bg-muted/50 focus-visible:outline-none">
                    <td className="px-4 py-3.5"><div className="flex min-w-0 items-center gap-3"><span className="flex size-8 shrink-0 items-center justify-center rounded-md bg-muted font-mono text-[11px] font-semibold text-foreground">{position.instrumentType === "ETF" ? "E" : "S"}</span><div className="min-w-0"><p className="font-mono text-sm font-semibold text-foreground">{position.symbol}</p><p className="truncate text-xs text-muted-foreground">{position.securityName ?? "名称待补充"} <span className="ml-1 rounded-full bg-muted px-1.5 py-0.5 text-[10px]">{position.instrumentType === "ETF" ? "ETF" : "股票"}</span></p></div></div></td>
                    <td className="px-4 py-3.5 text-right font-mono tabular-nums">{position.quantity}</td>
                    <td className="px-4 py-3.5 text-right"><p className="font-mono tabular-nums">{price(position.averageCost)}</p><p className="mt-0.5 text-xs text-muted-foreground">{price(position.latestPrice)}</p></td>
                    <td className="px-4 py-3.5 text-right font-mono tabular-nums">{money(position.marketValue)}</td>
                    <td className={`px-4 py-3.5 text-right font-mono font-semibold tabular-nums ${pnlTone(position.todayPnl)}`}>{signedMoney(position.todayPnl)}</td>
                    <td className={`px-4 py-3.5 text-right font-mono font-semibold tabular-nums ${pnlTone(position.unrealizedPnl)}`}>{signedMoney(position.unrealizedPnl)}</td>
                    <td className={`px-4 py-3.5 text-right font-mono tabular-nums ${pnlTone(position.unrealizedReturnRate)}`}>{percent(position.unrealizedReturnRate)}</td>
                    <td className="px-3 py-3.5 text-right"><ChevronRight className="ml-auto size-4 text-muted-foreground transition-transform group-hover:translate-x-0.5" /></td>
                  </tr>)}
                  {!positions.length ? <tr><td colSpan={8} className="px-4 py-14 text-center"><p className="text-sm font-medium">暂无已确认持仓</p><p className="mt-1 text-xs text-muted-foreground">导入截图、校正识别结果并确认交易后，持仓会显示在这里。</p></td></tr> : null}
                </tbody>
              </table>
            </div>
          </section>

          <ReviewCenter reviews={reviews} />
        </>}

        <PortfolioImportDrawer open={importDrawerOpen} onOpenChange={(open) => setQueryState({ imports: open ? "1" : null })} />
        <PortfolioPositionDrawer open={Boolean(selectedSymbol)} symbol={selectedSymbol} instrumentType={selectedInstrumentType} onOpenChange={(open) => setQueryState(open && selectedSymbol ? { stock: selectedSymbol, instrumentType: selectedInstrumentType } : { stock: null, instrumentType: null })} />
        <PortfolioReviewDrawer key={`review-${reviewDrawerOpen ? reviewSymbol ?? "new" : "closed"}`} open={reviewDrawerOpen} symbol={reviewSymbol} instrumentType={reviewInstrumentType} positions={positions} onOpenChange={(open) => setQueryState(open ? { review: "new" } : { review: null, symbol: null, instrumentType: null })} onSaved={() => void load()} />
      </div>
    </WorkspaceShell>
  )
}

function SnapshotSkeleton() {
  return <section className="grid gap-4 lg:grid-cols-[minmax(220px,0.9fr)_minmax(0,2.1fr)]"><Skeleton className="h-40 rounded-lg" /><Skeleton className="h-40 rounded-lg" /></section>
}

function ReviewCenter({ reviews }: { reviews: PortfolioReview[] }) {
  const openReviews = reviews.filter((review) => (review.status ?? "DRAFT") !== "COMPLETED")
  return <section className="overflow-hidden rounded-lg border border-border/60 bg-card" aria-labelledby="review-center-heading">
    <div className="flex items-start justify-between gap-3 border-b border-border/60 px-4 py-3.5"><div><div className="flex items-center gap-2"><ClipboardCheck className="size-4 text-muted-foreground" /><h2 id="review-center-heading" className="text-base font-semibold">复盘中心</h2></div><p className="mt-1 text-xs text-muted-foreground">把交易计划、执行和经验沉淀下来。</p></div><Link to="/personal-portfolio/reviews/new"><Button size="sm" variant="outline">新建复盘</Button></Link></div>
    {reviews.length ? <div className="divide-y divide-border/60">{reviews.slice(0, 5).map((review) => <Link key={review.id} to={`/personal-portfolio/reviews/${review.id}`} className="flex items-center justify-between gap-3 px-4 py-3 hover:bg-muted/35"><div className="min-w-0"><p className="truncate text-sm"><span className="font-mono font-medium">{review.symbol}</span><span className="ml-2 text-xs text-muted-foreground">{review.periodStart} 至 {review.periodEnd}</span></p><p className="mt-1 line-clamp-1 text-xs text-muted-foreground">{review.thesis || "尚未填写交易逻辑。"}</p></div><span className={`shrink-0 rounded-full px-2 py-0.5 text-xs ${review.status === "COMPLETED" ? "bg-emerald-50 text-emerald-700" : "bg-amber-50 text-amber-700"}`}>{review.status === "COMPLETED" ? "已完成" : "草稿"}</span></Link>)}</div> : <div className="flex min-h-[170px] items-center justify-center px-4 py-8 text-center"><div><p className="text-sm font-medium">还没有复盘记录</p><p className="mt-1 max-w-sm text-xs leading-5 text-muted-foreground">从持仓详情或这里新建一条复盘，把当时的交易理由和执行结果记录下来。</p></div></div>}
    {openReviews.length > 5 ? <div className="border-t border-border/60 px-4 py-2 text-right text-xs text-muted-foreground">还有 {openReviews.length - 5} 条待完成</div> : null}
  </section>
}
