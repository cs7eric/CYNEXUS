import { CalendarDays, CircleHelp } from "lucide-react"
import { Skeleton } from "@/components/ui/skeleton"
import type { PortfolioPerformanceState } from "./portfolio-performance-state"
import { PortfolioPerformanceBreakdown } from "./portfolio-performance-breakdown"

export function PortfolioDailyIncomeView({ state }: { state: PortfolioPerformanceState }) {
  const points = state.performance?.points ?? []
  const todayPoint = points.at(-1)
  const todayPnl = state.performance?.todayPnl ?? todayPoint?.periodPnl ?? 0

  return <div className="grid divide-y divide-border/60 xl:grid-cols-[minmax(0,1fr)_minmax(0,1.25fr)_minmax(220px,0.55fr)] xl:divide-x xl:divide-y-0">
    <DailyIncomeCalendar points={points} loading={state.loading} />
    <section className="min-w-0" aria-labelledby="daily-income-heatmap-heading">
      {state.loading && !state.performance ? <HeatmapSkeleton /> : <PortfolioPerformanceBreakdown performance={state.performance} range="daily" loading={state.loading} embedded />}
    </section>
    <DailyIncomeSummary point={todayPoint} value={todayPnl} cumulativePnl={state.performance?.periodPnl} loading={state.loading} />
  </div>
}

function DailyIncomeCalendar({ points, loading }: { points: NonNullable<PortfolioPerformanceState["performance"]>["points"]; loading: boolean }) {
  const sorted = [...points].sort((left, right) => left.tradeDate.localeCompare(right.tradeDate))
  const firstDate = sorted[0] ? parseDate(sorted[0].tradeDate) : null
  const lastDate = sorted.at(-1) ? parseDate(sorted.at(-1)!.tradeDate) : null
  const days = firstDate && lastDate ? calendarDays(firstDate, lastDate) : []
  const pointsByDate = new Map(sorted.map((point) => [point.tradeDate, point]))
  const maxAbs = Math.max(...sorted.map((point) => Math.abs(Number(point.periodPnl ?? 0))), 1)
  return <section className="min-w-0" aria-labelledby="daily-income-list-heading">
    <div className="flex items-center justify-between gap-3 border-b border-border/60 px-4 py-3.5"><div className="flex items-center gap-2"><CalendarDays className="size-4 text-muted-foreground" /><div><h3 id="daily-income-list-heading" className="text-sm font-semibold">近一个月日收益</h3><p className="mt-1 text-[11px] text-muted-foreground">日历视图 · 金额为当日盈亏</p></div></div><span className="rounded-full bg-muted px-2 py-0.5 text-[11px] text-muted-foreground">{points.length} 日</span></div>
    {sorted.length ? <div className="max-h-[360px] overflow-auto p-3"><div className="mb-2 grid grid-cols-7 gap-1 text-center text-[10px] text-muted-foreground">{["日", "一", "二", "三", "四", "五", "六"].map((label) => <span key={label}>{label}</span>)}</div><div className="grid grid-cols-7 gap-1">{days.map((date) => { const key = formatDate(date); const point = pointsByDate.get(key); const value = Number(point?.periodPnl ?? 0); const inRange = date >= (firstDate ?? date) && date <= (lastDate ?? date); return <div key={key} title={point ? `${key} ${signedMoney(value)}` : key} className={`flex min-h-12 flex-col items-center justify-center rounded-sm border border-border/40 px-0.5 py-1 text-center ${point ? calendarTone(value, maxAbs) : "bg-muted/35 text-muted-foreground"} ${inRange ? "" : "opacity-35"}`}><span className="text-[10px]">{date.getMonth() + 1}/{date.getDate()}</span>{point ? <span className="mt-0.5 font-mono text-[10px] font-semibold tabular-nums">{signedMoney(value)}</span> : <span className="mt-0.5 text-[10px]">-</span>}</div>})}</div></div> : loading ? <div className="grid grid-cols-4 gap-2 p-4">{Array.from({ length: 8 }, (_, index) => <Skeleton key={index} className="h-12 rounded-sm" />)}</div> : <EmptyDailyIncome />}
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

function calendarDays(first: Date, last: Date) {
  const start = new Date(first)
  start.setDate(start.getDate() - start.getDay())
  const end = new Date(last)
  end.setDate(end.getDate() + (6 - end.getDay()))
  const days: Date[] = []
  for (const cursor = new Date(start); cursor <= end; cursor.setDate(cursor.getDate() + 1)) days.push(new Date(cursor))
  return days
}

function parseDate(value: string) {
  const [year, month, day] = value.split("-").map(Number)
  return new Date(year, (month || 1) - 1, day || 1)
}

function formatDate(value: Date) {
  return `${value.getFullYear()}-${String(value.getMonth() + 1).padStart(2, "0")}-${String(value.getDate()).padStart(2, "0")}`
}

function calendarTone(value: number, maxAbs: number) {
  const ratio = Math.min(Math.abs(value) / maxAbs, 1)
  if (value > 0) return ratio > 0.66 ? "bg-rose-500 text-white" : ratio > 0.33 ? "bg-rose-200 text-rose-900" : "bg-rose-100 text-rose-800"
  if (value < 0) return ratio > 0.66 ? "bg-emerald-500 text-white" : ratio > 0.33 ? "bg-emerald-200 text-emerald-900" : "bg-emerald-100 text-emerald-800"
  return "bg-muted text-muted-foreground"
}

function signedMoney(value: number | null | undefined) {
  if (value == null) return "--"
  return `${value > 0 ? "+" : ""}${Number(value).toFixed(2)}`
}

function tone(value: number | null | undefined) {
  if (value == null || value === 0) return "text-muted-foreground"
  return value > 0 ? "text-rose-600" : "text-emerald-600"
}
