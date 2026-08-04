import { CircleHelp, TrendingDown, TrendingUp } from "lucide-react"
import { Skeleton } from "@/components/ui/skeleton"
import type { PortfolioOverview } from "@/services/market/portfolio/types"
import type { PortfolioPerformanceState } from "./portfolio-performance-state"
import { PortfolioReturnBreakdown } from "./portfolio-return-breakdown"

export function PortfolioDailyIncomeView({ state, overview }: { state: PortfolioPerformanceState; overview: PortfolioOverview | null }) {
  const points = state.performance?.points ?? []
  const todayPoint = points.at(-1)
  const todayPnl = overview?.todayPnl ?? state.performance?.todayPnl ?? todayPoint?.periodPnl ?? 0

  return <div className="grid divide-y divide-border/60 xl:grid-cols-[minmax(0,1fr)_minmax(0,1.25fr)_minmax(220px,0.55fr)] xl:divide-x xl:divide-y-0">
    <DailyIncomeList points={points} loading={state.loading} />
    <section className="min-w-0" aria-labelledby="daily-income-heatmap-heading">
      {state.loading && !overview ? <HeatmapSkeleton /> : <PortfolioReturnBreakdown overview={overview} embedded mode="today" />}
    </section>
    <DailyIncomeSummary point={todayPoint} value={todayPnl} cumulativePnl={state.performance?.periodPnl} loading={state.loading} />
  </div>
}

function DailyIncomeList({ points, loading }: { points: NonNullable<PortfolioPerformanceState["performance"]>["points"]; loading: boolean }) {
  const sorted = [...points].reverse()
  return <section className="min-w-0" aria-labelledby="daily-income-list-heading">
    <div className="flex items-center justify-between gap-3 border-b border-border/60 px-4 py-3.5"><div><h3 id="daily-income-list-heading" className="text-sm font-semibold">近一个月日收益</h3><p className="mt-1 text-[11px] text-muted-foreground">按交易日倒序展示</p></div><span className="rounded-full bg-muted px-2 py-0.5 text-[11px] text-muted-foreground">{points.length} 日</span></div>
    {sorted.length ? <div className="max-h-[360px] overflow-auto"><div className="divide-y divide-border/60">{sorted.map((point) => <div key={point.tradeDate} className="flex items-center justify-between gap-3 px-4 py-2.5 hover:bg-muted/30"><div className="min-w-0"><p className="text-xs font-medium">{formatDateLabel(point.tradeDate)}</p><p className="mt-0.5 font-mono text-[11px] text-muted-foreground tabular-nums">收益率 {(Number(point.returnRate ?? 0) * 100).toFixed(2)}%</p></div><div className={`flex items-center gap-1 font-mono text-sm font-semibold tabular-nums ${tone(point.periodPnl)}`}>{Number(point.periodPnl ?? 0) > 0 ? <TrendingUp className="size-3.5" /> : Number(point.periodPnl ?? 0) < 0 ? <TrendingDown className="size-3.5" /> : null}{signedMoney(point.periodPnl)}</div></div>)}</div></div> : loading ? <div className="space-y-2 p-4">{Array.from({ length: 6 }, (_, index) => <Skeleton key={index} className="h-10 w-full" />)}</div> : <EmptyDailyIncome />}
  </section>
}

function DailyIncomeSummary({ point, value, cumulativePnl, loading }: { point: NonNullable<PortfolioPerformanceState["performance"]>["points"][number] | undefined; value: number; cumulativePnl?: number; loading: boolean }) {
  return <section className="min-w-0" aria-labelledby="daily-income-summary-heading">
    <div className="border-b border-border/60 px-4 py-3.5"><h3 id="daily-income-summary-heading" className="text-sm font-semibold">当日收益</h3><p className="mt-1 text-[11px] text-muted-foreground">对比上一交易日收盘</p></div>
    {loading ? <div className="space-y-3 p-4"><Skeleton className="h-9 w-3/4" /><Skeleton className="h-4 w-1/2" /><Skeleton className="mt-6 h-12 w-full" /></div> : <div className="px-4 py-6"><p className={`font-mono text-3xl font-semibold tabular-nums ${tone(value)}`}>{signedMoney(value)}</p><p className="mt-2 text-xs text-muted-foreground">{point?.tradeDate ?? "等待行情"}</p><div className="mt-6 space-y-2 border-t border-border/60 pt-4 text-xs"><div className="flex items-center justify-between gap-3"><span className="text-muted-foreground">近一个月累计</span><span className={`font-mono font-semibold tabular-nums ${tone(cumulativePnl)}`}>{signedMoney(cumulativePnl)}</span></div><div className="flex items-center justify-between gap-3"><span className="text-muted-foreground">数据点</span><span className="font-mono tabular-nums">{point ? "近一个月" : "--"}</span></div></div></div>}
  </section>
}

function EmptyDailyIncome() {
  return <div className="flex min-h-[180px] items-center justify-center px-4 py-8 text-center"><div><CircleHelp className="mx-auto size-4 text-muted-foreground" /><p className="mt-2 text-sm font-medium">暂无日收益数据</p><p className="mt-1 text-xs text-muted-foreground">确认交易并获取行情后，这里会显示收益变化。</p></div></div>
}

function HeatmapSkeleton() {
  return <div className="grid grid-cols-7 gap-1.5 p-4">{Array.from({ length: 35 }, (_, index) => <Skeleton key={index} className="h-12 rounded-sm" />)}</div>
}

function formatDateLabel(value: string) {
  const [year, month, day] = value.split("-").map(Number)
  const date = new Date(year, (month || 1) - 1, day || 1)
  return `${date.getMonth() + 1}月${date.getDate()}日 周${["日", "一", "二", "三", "四", "五", "六"][date.getDay()]}`
}

function signedMoney(value: number | null | undefined) {
  if (value == null) return "--"
  return `${value > 0 ? "+" : ""}${Number(value).toFixed(2)}`
}

function tone(value: number | null | undefined) {
  if (value == null || value === 0) return "text-muted-foreground"
  return value > 0 ? "text-rose-600" : "text-emerald-600"
}
