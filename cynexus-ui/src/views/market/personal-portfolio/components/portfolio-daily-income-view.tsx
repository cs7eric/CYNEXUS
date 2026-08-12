import { useEffect, useState } from "react"
import { CalendarDays, CircleAlert, CircleHelp } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import { fetchPortfolioPerformance } from "@/services/market/portfolio/portfolio"
import type { PortfolioPerformance } from "@/services/market/portfolio/types"
import type { PortfolioPerformanceState } from "./portfolio-performance-state"
import { PortfolioPerformanceBreakdown } from "./portfolio-performance-breakdown"

export function PortfolioDailyIncomeView({ state }: { state: PortfolioPerformanceState }) {
  const points = state.performance?.points ?? []
  const latestDate = points.at(-1)?.tradeDate ?? null
  const [selectedDate, setSelectedDate] = useState<string | null>(null)
  const [selectedPerformance, setSelectedPerformance] = useState<PortfolioPerformance | null>(null)
  const [selectedPerformanceDate, setSelectedPerformanceDate] = useState<string | null>(null)
  const [selectedLoading, setSelectedLoading] = useState(false)
  const [selectedError, setSelectedError] = useState<string | null>(null)
  const [selectedErrorDate, setSelectedErrorDate] = useState<string | null>(null)
  const [selectedRequestToken, setSelectedRequestToken] = useState(0)
  const activeDate = selectedDate && points.some((point) => point.tradeDate === selectedDate) ? selectedDate : latestDate
  const activePerformance = activeDate === latestDate ? state.performance : selectedPerformanceDate === activeDate ? selectedPerformance : null
  const activePoint = activePerformance?.points.at(-1)
  const activePnl = activePerformance?.todayPnl ?? activePoint?.periodPnl ?? 0
  const activeLoading = state.loading || (activeDate !== latestDate && (selectedLoading || selectedPerformanceDate !== activeDate))
  const activeError = selectedErrorDate === activeDate ? selectedError : null

  const handleSelectDate = (date: string) => {
    const sameDate = date === selectedDate
    setSelectedDate(date)
    if (date === latestDate) {
      setSelectedLoading(false)
      setSelectedError(null)
      setSelectedErrorDate(null)
      return
    }
    setSelectedLoading(true)
    setSelectedError(null)
    setSelectedErrorDate(null)
    if (sameDate) setSelectedRequestToken((value) => value + 1)
  }

  useEffect(() => {
    if (!activeDate || activeDate === latestDate) return
    let cancelled = false
    void fetchPortfolioPerformance("daily", activeDate, activeDate)
      .then((result) => {
        if (!cancelled) {
          setSelectedPerformance(result)
          setSelectedPerformanceDate(activeDate)
        }
      })
      .catch((cause) => {
        if (!cancelled) {
          setSelectedError(cause instanceof Error ? cause.message : String(cause))
          setSelectedErrorDate(activeDate)
        }
      })
      .finally(() => {
        if (!cancelled) setSelectedLoading(false)
      })
    return () => { cancelled = true }
  }, [activeDate, latestDate, selectedRequestToken])

  return <div className="grid divide-y divide-border/60 xl:grid-cols-[minmax(0,1fr)_minmax(0,1.25fr)_minmax(220px,0.55fr)] xl:divide-x xl:divide-y-0">
    <DailyIncomeCalendar points={points} selectedDate={activeDate} onSelectDate={handleSelectDate} loading={state.loading} />
    <section className="min-w-0" aria-labelledby="daily-income-heatmap-heading">
      {activeError && !selectedLoading ? <SelectedDateError date={activeDate} error={activeError} onRetry={() => { setSelectedLoading(true); setSelectedError(null); setSelectedErrorDate(null); setSelectedRequestToken((value) => value + 1) }} /> : state.loading && !state.performance ? <HeatmapSkeleton /> : <PortfolioPerformanceBreakdown performance={activePerformance} range="daily" loading={activeLoading} embedded />}
    </section>
    <DailyIncomeSummary point={activePoint} value={activePnl} cumulativePnl={state.performance?.periodPnl} selectedDate={activeDate !== latestDate ? activeDate : null} loading={state.loading || selectedLoading} />
  </div>
}

function DailyIncomeCalendar({ points, selectedDate, onSelectDate, loading }: { points: NonNullable<PortfolioPerformanceState["performance"]>["points"]; selectedDate: string | null; onSelectDate: (date: string) => void; loading: boolean }) {
  const sorted = [...points].sort((left, right) => left.tradeDate.localeCompare(right.tradeDate))
  const firstDate = sorted[0] ? parseDate(sorted[0].tradeDate) : null
  const lastDate = sorted.at(-1) ? parseDate(sorted.at(-1)!.tradeDate) : null
  const days = firstDate && lastDate ? calendarDays(firstDate, lastDate) : []
  const pointsByDate = new Map(sorted.map((point) => [point.tradeDate, point]))
  const maxAbs = Math.max(...sorted.map((point) => Math.abs(Number(point.periodPnl ?? 0))), 1)
  return <section className="min-w-0" aria-labelledby="daily-income-list-heading">
    <div className="flex items-center justify-between gap-3 border-b border-border/60 px-4 py-3.5"><div className="flex items-center gap-2"><CalendarDays className="size-4 text-muted-foreground" /><div><h3 id="daily-income-list-heading" className="text-sm font-semibold">近一个月日收益</h3><p className="mt-1 text-[11px] text-muted-foreground">日历视图 · 点击交易日查看当日持仓贡献</p></div></div><span className="rounded-full bg-muted px-2 py-0.5 text-[11px] text-muted-foreground">{points.length} 日</span></div>
    {sorted.length ? <div className="max-h-[360px] overflow-auto p-3"><div className="mb-2 grid grid-cols-7 gap-1 text-center text-[10px] text-muted-foreground">{["日", "一", "二", "三", "四", "五", "六"].map((label) => <span key={label}>{label}</span>)}</div><div className="grid grid-cols-7 gap-1">{days.map((date) => { const key = formatDate(date); const point = pointsByDate.get(key); const value = Number(point?.periodPnl ?? 0); const inRange = date >= (firstDate ?? date) && date <= (lastDate ?? date); const selected = key === selectedDate; return <button key={key} type="button" disabled={!point} aria-pressed={selected} aria-label={point ? `${key} 当日收益 ${signedMoney(value)}，点击查看` : `${key} 无交易数据`} title={point ? `${key} ${signedMoney(value)} · 点击查看` : key} onClick={() => { if (point) onSelectDate(key) }} className={`flex min-h-12 flex-col items-center justify-center rounded-sm border border-border/40 px-0.5 py-1 text-center outline-none transition-[box-shadow,opacity] duration-150 focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-1 disabled:cursor-not-allowed ${point ? calendarTone(value, maxAbs) : "bg-muted/35 text-muted-foreground"} ${inRange ? "" : "opacity-35"} ${selected ? selectedCalendarTone(value) : ""}`}><span className="text-[10px]">{date.getMonth() + 1}/{date.getDate()}</span>{point ? <span className="mt-0.5 font-mono text-[10px] font-semibold tabular-nums">{signedMoney(value)}</span> : <span className="mt-0.5 text-[10px]">-</span>}</button>})}</div></div> : loading ? <div className="grid grid-cols-4 gap-2 p-4">{Array.from({ length: 8 }, (_, index) => <Skeleton key={index} className="h-12 rounded-sm" />)}</div> : <EmptyDailyIncome />}
  </section>
}

function DailyIncomeSummary({ point, value, cumulativePnl, selectedDate, loading }: { point: NonNullable<PortfolioPerformanceState["performance"]>["points"][number] | undefined; value: number; cumulativePnl?: number; selectedDate: string | null; loading: boolean }) {
  return <section className="min-w-0" aria-labelledby="daily-income-summary-heading">
    <div className="border-b border-border/60 px-4 py-3.5"><h3 id="daily-income-summary-heading" className="text-sm font-semibold">当日收益</h3><p className="mt-1 text-[11px] text-muted-foreground">对比上一交易日收盘</p></div>
    {loading ? <div className="space-y-3 p-4"><Skeleton className="h-9 w-3/4" /><Skeleton className="h-4 w-1/2" /><Skeleton className="mt-6 h-12 w-full" /></div> : <div className="px-4 py-6"><p className={`font-mono text-3xl font-semibold tabular-nums ${tone(value)}`}>{signedMoney(value)}</p><p className="mt-2 text-xs text-muted-foreground">{point?.tradeDate ?? "等待行情"}</p><div className="mt-6 space-y-2 border-t border-border/60 pt-4 text-xs"><div className="flex items-center justify-between gap-3"><span className="text-muted-foreground">近一个月累计</span><span className={`font-mono font-semibold tabular-nums ${tone(cumulativePnl)}`}>{signedMoney(cumulativePnl)}</span></div><div className="flex items-center justify-between gap-3"><span className="text-muted-foreground">数据点</span><span className="font-mono tabular-nums">{point ? selectedDate ? "选中日期" : "最新交易日" : "--"}</span></div></div></div>}
  </section>
}

function SelectedDateError({ date, error, onRetry }: { date: string | null; error: string; onRetry: () => void }) {
  return <div className="flex min-h-[260px] items-center justify-center px-4 py-10 text-center"><div><CircleAlert className="mx-auto size-5 text-destructive" /><p className="mt-2 text-sm font-medium">{date ?? "选中日期"}收益加载失败</p><p className="mt-1 max-w-xs break-words text-xs leading-5 text-muted-foreground">{error}</p><Button type="button" size="sm" variant="outline" className="mt-4" onClick={onRetry}>重试</Button></div></div>
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

function selectedCalendarTone(value: number) {
  if (value > 0) return "ring-2 ring-rose-200 ring-offset-1"
  if (value < 0) return "ring-2 ring-emerald-200 ring-offset-1"
  return "ring-2 ring-muted-foreground/20 ring-offset-1"
}

function signedMoney(value: number | null | undefined) {
  if (value == null) return "--"
  return `${value > 0 ? "+" : ""}${Number(value).toFixed(2)}`
}

function tone(value: number | null | undefined) {
  if (value == null || value === 0) return "text-muted-foreground"
  return value > 0 ? "text-rose-600" : "text-emerald-600"
}
