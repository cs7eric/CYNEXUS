import { useCallback, useEffect, useMemo, useState } from "react"
import { Area, Bar, CartesianGrid, Cell, ComposedChart, ReferenceLine, XAxis, YAxis } from "recharts"
import { ChartNoAxesCombined, CircleAlert, Loader2 } from "lucide-react"
import { Button } from "@/components/ui/button"
import { ChartContainer, ChartTooltip, ChartTooltipContent, type ChartConfig } from "@/components/ui/chart"
import { Skeleton } from "@/components/ui/skeleton"
import { fetchPortfolioPerformance } from "@/services/market/portfolio/portfolio"
import type { PortfolioPerformance, PortfolioPerformancePoint, PortfolioPerformanceRange } from "@/services/market/portfolio/types"

const chartConfig = {
  cumulativePnl: { label: "累计收益", color: "var(--chart-1)" },
  periodPnl: { label: "周期收益", color: "var(--chart-2)" },
} satisfies ChartConfig

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
  const [performance, setPerformance] = useState<PortfolioPerformance | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const loadPerformance = useCallback(async (cancelledRef: { current: boolean }) => {
    setLoading(true)
    setError(null)
    try {
      const result = await fetchPortfolioPerformance(range)
      if (!cancelledRef.current) setPerformance(result)
    } catch (cause) {
      if (!cancelledRef.current) setError(cause instanceof Error ? cause.message : String(cause))
    } finally {
      if (!cancelledRef.current) setLoading(false)
    }
  }, [range])

  useEffect(() => {
    const cancelledRef = { current: false }
    const timer = window.setTimeout(() => { void loadPerformance(cancelledRef) }, 0)
    return () => { cancelledRef.current = true; window.clearTimeout(timer) }
  }, [loadPerformance, range])

  const chartData = useMemo(() => {
    return (performance?.points ?? []).reduce<Array<PortfolioPerformancePoint & { cumulativePnl: number; label: string }>>((items, point) => {
      const cumulative = (items.at(-1)?.cumulativePnl ?? 0) + Number(point.periodPnl ?? 0)
      return [
        ...items,
        {
          ...point,
          cumulativePnl: cumulative,
          label: range === "yearly" ? point.tradeDate.slice(0, 4) : range === "monthly" ? point.tradeDate.slice(0, 7) : range === "weekly" ? `${point.tradeDate.slice(5)} 周` : point.tradeDate.slice(5),
        },
      ]
    }, [])
  }, [performance, range])
  const currentPeriod = performance?.points.at(-1)
  const currentPeriodLabel = range === "daily" ? "今日" : range === "weekly" ? "本周" : range === "monthly" ? "本月" : "本年"
  const viewCumulativePnl = chartData.at(-1)?.cumulativePnl ?? 0

  return (
    <section className="overflow-hidden rounded-lg border border-border/60 bg-card" aria-labelledby="performance-heading">
      <div className="flex flex-wrap items-start justify-between gap-3 border-b border-border/60 px-4 py-3.5">
        <div>
          <div className="flex items-center gap-2"><ChartNoAxesCombined className="size-4 text-muted-foreground" aria-hidden="true" /><h2 id="performance-heading" className="text-base font-semibold">收益变化</h2></div>
          <p className="mt-1 text-xs text-muted-foreground">按日、周、月、年观察收益变化；面积为当前视图累计收益，柱体为对应周期收益，红涨绿跌。</p>
        </div>
        <div className="flex flex-wrap items-center gap-1 rounded-md bg-muted/50 p-1" role="group" aria-label="收益视图">
          {(["daily", "weekly", "monthly", "yearly"] as PortfolioPerformanceRange[]).map((item) => <Button key={item} size="sm" variant={range === item ? "default" : "ghost"} className="h-7 px-2.5 text-xs" onClick={() => setRange(item)}>{item === "daily" ? "日" : item === "weekly" ? "周" : item === "monthly" ? "月" : "年"}</Button>)}
        </div>
      </div>

      {loading ? <div className="space-y-4 p-4"><div className="grid gap-3 sm:grid-cols-4"><Skeleton className="h-16" /><Skeleton className="h-16" /><Skeleton className="h-16" /><Skeleton className="h-16" /></div><Skeleton className="h-[280px] w-full" /></div> : error ? <div className="flex items-start gap-2 px-4 py-12 text-sm text-rose-700"><CircleAlert className="mt-0.5 size-4 shrink-0" /><div><p className="font-medium">收益历史加载失败</p><p className="mt-1 break-words text-xs">{error}</p></div></div> : performance && performance.points.length ? <div className="p-4">
        <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-5">
          <Summary label="今日收益" value={money(performance.todayPnl ?? currentPeriod?.periodPnl)} toneClass={tone(performance.todayPnl ?? currentPeriod?.periodPnl)} detail="对比上一交易日收盘" />
          <Summary label={`${currentPeriodLabel}收益`} value={money(currentPeriod?.periodPnl)} toneClass={tone(currentPeriod?.periodPnl)} detail={currentPeriod?.tradeDate ?? "等待行情"} />
          <Summary label="视图收益率" value={percent(currentPeriod?.returnRate)} toneClass={tone(currentPeriod?.returnRate)} detail="按当前视图周期计算" />
          <Summary label="期末总资产" value={amount(performance.endAssets)} toneClass="text-foreground" detail={`期初 ${amount(performance.startAssets)}`} />
          <Summary label="视图累计收益" value={money(viewCumulativePnl)} toneClass={tone(viewCumulativePnl)} detail={`${performance.tradingDays} 个交易周期`} />
        </div>
        <div className="mt-5 flex items-center justify-between gap-3"><div><p className="text-xs font-medium text-muted-foreground">{currentPeriodLabel}收益走势</p><p className="mt-0.5 text-[11px] text-muted-foreground">面积为当前视图累计收益，柱体为周期收益变化</p></div><span className="text-[11px] text-muted-foreground">数据点 {performance.points.length}</span></div>
        <ChartContainer config={chartConfig} className="mt-2 h-[300px] w-full aspect-auto">
          <ComposedChart data={chartData} margin={{ top: 8, right: 12, left: 0, bottom: 0 }}>
            <CartesianGrid vertical={false} strokeDasharray="3 3" />
            <XAxis dataKey="label" tickLine={false} axisLine={false} minTickGap={24} fontSize={10} />
            <YAxis tickLine={false} axisLine={false} width={52} fontSize={10} tickFormatter={(value) => Number(value).toFixed(0)} />
            <ReferenceLine y={0} stroke="var(--border)" strokeDasharray="3 3" />
            <ChartTooltip cursor={{ fill: "var(--muted)" }} content={<ChartTooltipContent labelFormatter={(value) => String(value)} formatter={(value, name) => [money(Number(value)), name === "cumulativePnl" ? "累计收益" : "周期收益"]} />} />
            <Area type="monotone" dataKey="cumulativePnl" stroke="var(--color-cumulativePnl)" fill="var(--color-cumulativePnl)" fillOpacity={0.12} strokeWidth={2} dot={false} isAnimationActive={false} />
            <Bar dataKey="periodPnl" radius={[3, 3, 0, 0]} barSize={14} isAnimationActive={false}>
              {chartData.map((point) => <Cell key={point.tradeDate} className={Number(point.periodPnl) >= 0 ? "fill-rose-500" : "fill-emerald-500"} />)}
            </Bar>
          </ComposedChart>
        </ChartContainer>
      </div> : <div className="flex min-h-[220px] items-center justify-center px-4 py-10 text-center"><div><Loader2 className="mx-auto size-4 text-muted-foreground" /><p className="mt-2 text-sm font-medium">暂无可计算的历史收益</p><p className="mt-1 max-w-md text-xs leading-5 text-muted-foreground">需要已确认的交易和对应标的的日线行情；导入并确认交易后会自动出现。</p></div></div>}
    </section>
  )
}

function Summary({ label, value, toneClass, detail }: { label: string; value: string; toneClass: string; detail: string }) {
  return <div className="rounded-md border border-border/60 bg-muted/20 px-3 py-2.5"><p className="text-xs text-muted-foreground">{label}</p><p className={`mt-1 truncate font-mono text-lg font-semibold tabular-nums ${toneClass}`}>{value}</p><p className="mt-0.5 truncate text-[11px] text-muted-foreground">{detail}</p></div>
}
