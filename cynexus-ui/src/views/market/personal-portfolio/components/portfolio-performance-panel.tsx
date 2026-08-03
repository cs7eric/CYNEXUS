import { useCallback, useEffect, useMemo, useState } from "react"
import { Area, AreaChart, CartesianGrid, XAxis, YAxis } from "recharts"
import { CalendarRange, ChartNoAxesCombined, CircleAlert, Loader2 } from "lucide-react"
import { Button } from "@/components/ui/button"
import { ChartContainer, ChartTooltip, ChartTooltipContent, type ChartConfig } from "@/components/ui/chart"
import { Input } from "@/components/ui/input"
import { Skeleton } from "@/components/ui/skeleton"
import { fetchPortfolioPerformance } from "@/services/market/portfolio/portfolio"
import type { PortfolioPerformance, PortfolioPerformanceRange } from "@/services/market/portfolio/types"

const chartConfig = {
  totalPnl: { label: "累计收益", color: "var(--chart-1)" },
  periodPnl: { label: "区间收益", color: "var(--chart-2)" },
} satisfies ChartConfig

const today = () => new Intl.DateTimeFormat("en-CA", { timeZone: "Asia/Shanghai" }).format(new Date())
const daysAgo = (days: number) => {
  const date = new Date()
  date.setDate(date.getDate() - days)
  return date.toISOString().slice(0, 10)
}

function money(value: number | null | undefined) {
  if (value == null) return "--"
  return `${value > 0 ? "+" : ""}${Number(value).toFixed(2)}`
}

function amount(value: number | null | undefined) {
  if (value == null) return "--"
  return Number(value).toFixed(2)
}

function percent(value: number | null | undefined) {
  if (value == null) return "--"
  return `${(Number(value) * 100).toFixed(2)}%`
}

function tone(value: number | null | undefined) {
  if (value == null || value === 0) return "text-muted-foreground"
  return value > 0 ? "text-rose-600" : "text-emerald-600"
}

export function PortfolioPerformancePanel() {
  const [range, setRange] = useState<PortfolioPerformanceRange>("daily")
  const [from, setFrom] = useState(daysAgo(29))
  const [to, setTo] = useState(today())
  const [performance, setPerformance] = useState<PortfolioPerformance | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const loadPerformance = useCallback(async (cancelledRef: { current: boolean }) => {
    setLoading(true)
    setError(null)
    try {
      const result = await fetchPortfolioPerformance(range, range === "custom" ? from : undefined, range === "custom" ? to : undefined)
      if (!cancelledRef.current) setPerformance(result)
    } catch (cause) {
      if (!cancelledRef.current) setError(cause instanceof Error ? cause.message : String(cause))
    } finally {
      if (!cancelledRef.current) setLoading(false)
    }
  }, [from, range, to])

  useEffect(() => {
    if (range === "custom" && (!from || !to)) return
    const cancelledRef = { current: false }
    const timer = window.setTimeout(() => { void loadPerformance(cancelledRef) }, 0)
    return () => { cancelledRef.current = true; window.clearTimeout(timer) }
  }, [from, loadPerformance, range, to])

  const chartData = useMemo(() => (performance?.points ?? []).map((point) => ({
    ...point,
    label: range === "monthly" ? point.tradeDate.slice(0, 7) : point.tradeDate.slice(5),
  })), [performance, range])
  const todayDate = today()
  const todayPoint = performance?.points.find((point) => point.tradeDate === todayDate)
  const todayPnl = performance?.todayPnl ?? todayPoint?.periodPnl
  const chartDataWithTodayPnl = useMemo(() => chartData.map((point) => point.tradeDate === todayDate && todayPnl != null
    ? { ...point, periodPnl: todayPnl }
    : point), [chartData, todayDate, todayPnl])

  return (
    <section className="overflow-hidden rounded-lg border border-border/60 bg-card" aria-labelledby="performance-heading">
      <div className="flex flex-wrap items-start justify-between gap-3 border-b border-border/60 px-4 py-3.5">
        <div>
          <div className="flex items-center gap-2"><ChartNoAxesCombined className="size-4 text-muted-foreground" aria-hidden="true" /><h2 id="performance-heading" className="text-base font-semibold">区间收益</h2></div>
          <p className="mt-1 text-xs text-muted-foreground">根据成交价格和每日收盘价重建组合收益，不把可用资金变化误算成交易收益。</p>
        </div>
        <div className="flex flex-wrap items-center gap-1 rounded-md bg-muted/50 p-1" role="group" aria-label="收益区间">
          {(["daily", "monthly", "custom"] as PortfolioPerformanceRange[]).map((item) => <Button key={item} size="sm" variant={range === item ? "default" : "ghost"} className="h-7 px-2.5 text-xs" onClick={() => setRange(item)}>{item === "daily" ? "日" : item === "monthly" ? "月" : "自定义"}</Button>)}
        </div>
      </div>

      {range === "custom" ? <div className="flex flex-wrap items-end gap-3 border-b border-border/60 bg-muted/20 px-4 py-3">
        <label className="grid gap-1 text-xs text-muted-foreground"><span className="inline-flex items-center gap-1"><CalendarRange className="size-3.5" aria-hidden="true" />开始日期</span><Input type="date" value={from} max={to || today()} onChange={(event) => setFrom(event.target.value)} className="h-8 w-[150px] bg-background text-xs" /></label>
        <label className="grid gap-1 text-xs text-muted-foreground"><span>结束日期</span><Input type="date" value={to} min={from} max={today()} onChange={(event) => setTo(event.target.value)} className="h-8 w-[150px] bg-background text-xs" /></label>
      </div> : null}

      {loading ? <div className="space-y-4 p-4"><div className="grid gap-3 sm:grid-cols-4"><Skeleton className="h-16" /><Skeleton className="h-16" /><Skeleton className="h-16" /><Skeleton className="h-16" /></div><Skeleton className="h-[280px] w-full" /></div> : error ? <div className="flex items-start gap-2 px-4 py-12 text-sm text-rose-700"><CircleAlert className="mt-0.5 size-4 shrink-0" /><div><p className="font-medium">收益历史加载失败</p><p className="mt-1 break-words text-xs">{error}</p></div></div> : performance && performance.points.length ? <div className="p-4">
        <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-5">
          <Summary label="今日盈亏" value={money(todayPnl)} toneClass={tone(todayPnl)} detail={todayPnl != null ? "对比上一交易日收盘，已剔除非今日资金变化" : "等待今日实时行情"} />
          <Summary label="区间收益" value={money(performance.periodPnl)} toneClass={tone(performance.periodPnl)} detail={`${performance.from} 至 ${performance.to}`} />
          <Summary label="区间收益率" value={percent(performance.periodReturnRate)} toneClass={tone(performance.periodReturnRate)} detail="以区间起始总资产为基准" />
          <Summary label="期末总资产" value={amount(performance.endAssets)} toneClass="text-foreground" detail={`期初 ${amount(performance.startAssets)}`} />
          <Summary label="最好 / 最差" value={`${money(performance.bestPeriodPnl)} / ${money(performance.worstPeriodPnl)}`} toneClass="text-foreground" detail={`${performance.tradingDays} 个交易日`} />
        </div>
        <div className="mt-5 flex items-center justify-between gap-3"><div><p className="text-xs font-medium text-muted-foreground">收益变化</p><p className="mt-0.5 text-[11px] text-muted-foreground">面积为累计收益，虚线为当日 / 当月收益变化</p></div><span className="text-[11px] text-muted-foreground">数据点 {performance.points.length}</span></div>
        <ChartContainer config={chartConfig} className="mt-2 h-[300px] w-full aspect-auto">
          <AreaChart data={chartDataWithTodayPnl} margin={{ top: 8, right: 12, left: 0, bottom: 0 }}>
            <CartesianGrid vertical={false} strokeDasharray="3 3" />
            <XAxis dataKey="label" tickLine={false} axisLine={false} minTickGap={24} fontSize={10} />
            <YAxis tickLine={false} axisLine={false} width={52} fontSize={10} tickFormatter={(value) => Number(value).toFixed(0)} />
            <ChartTooltip cursor={{ fill: "var(--muted)" }} content={<ChartTooltipContent labelFormatter={(value) => String(value)} formatter={(value, name) => [money(Number(value)), name === "totalPnl" ? "累计收益" : "区间收益"]} />} />
            <Area type="monotone" dataKey="totalPnl" stroke="var(--color-totalPnl)" fill="var(--color-totalPnl)" fillOpacity={0.12} strokeWidth={2} dot={false} isAnimationActive={false} />
            <Area type="monotone" dataKey="periodPnl" stroke="var(--color-periodPnl)" fill="none" strokeDasharray="4 4" strokeWidth={1.5} dot={false} isAnimationActive={false} />
          </AreaChart>
        </ChartContainer>
      </div> : <div className="flex min-h-[220px] items-center justify-center px-4 py-10 text-center"><div><Loader2 className="mx-auto size-4 text-muted-foreground" /><p className="mt-2 text-sm font-medium">暂无可计算的历史收益</p><p className="mt-1 max-w-md text-xs leading-5 text-muted-foreground">需要已确认的交易和对应标的的日线行情；导入并确认交易后会自动出现。</p></div></div>}
    </section>
  )
}

function Summary({ label, value, toneClass, detail }: { label: string; value: string; toneClass: string; detail: string }) {
  return <div className="rounded-md border border-border/60 bg-muted/20 px-3 py-2.5"><p className="text-xs text-muted-foreground">{label}</p><p className={`mt-1 truncate font-mono text-lg font-semibold tabular-nums ${toneClass}`}>{value}</p><p className="mt-0.5 truncate text-[11px] text-muted-foreground">{detail}</p></div>
}
