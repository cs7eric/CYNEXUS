import { useMemo } from "react"
import { Area, Bar, CartesianGrid, Cell, ComposedChart, ReferenceLine, XAxis, YAxis } from "recharts"
import { ChartNoAxesCombined, ChevronLeft, ChevronRight, CircleAlert, Loader2 } from "lucide-react"
import { Button } from "@/components/ui/button"
import { ChartContainer, ChartTooltip, ChartTooltipContent, type ChartConfig } from "@/components/ui/chart"
import { Skeleton } from "@/components/ui/skeleton"
import type { PortfolioPerformance, PortfolioPerformancePoint, PortfolioPerformanceRange } from "@/services/market/portfolio/types"
import { PortfolioIncomeCalendar } from "./portfolio-income-calendar"
import type { PortfolioPerformanceState } from "./portfolio-performance-state"

const chartConfig = {
  cumulativePnl: { label: "累计收益", color: "var(--chart-1)" },
  periodPnl: { label: "周期收益", color: "var(--chart-2)" },
} satisfies ChartConfig

export type PortfolioChartView = "calendar" | "bars"

interface PortfolioPerformanceToolbarProps {
  range: PortfolioPerformanceRange
  onRangeChange: (range: PortfolioPerformanceRange) => void
  chartView: PortfolioChartView
  onChartViewChange: (view: PortfolioChartView) => void
  showChartView?: boolean
  periodLabel?: string | null
  canGoPrevious?: boolean
  canGoNext?: boolean
  onPreviousPeriod?: () => void
  onNextPeriod?: () => void
}

export function PortfolioPerformanceToolbar({ range, onRangeChange, chartView, onChartViewChange, showChartView = true, periodLabel, canGoPrevious = true, canGoNext = false, onPreviousPeriod, onNextPeriod }: PortfolioPerformanceToolbarProps) {
  return <div className="flex flex-wrap items-center gap-2" aria-label="收益筛选与视图">
    <div className="flex flex-wrap items-center gap-1 rounded-md bg-muted/50 p-1" role="group" aria-label="收益视图">
      {(["daily", "weekly", "monthly", "yearly"] as PortfolioPerformanceRange[]).map((item) => <Button key={item} type="button" size="sm" variant={range === item ? "default" : "ghost"} className="h-7 px-2.5 text-xs" onClick={() => onRangeChange(item)}>{item === "daily" ? "日" : item === "weekly" ? "周" : item === "monthly" ? "月" : "年"}</Button>)}
    </div>
    {showChartView ? <div className="flex items-center gap-1 rounded-md border border-border/60 bg-background p-1" role="group" aria-label="图表类型">
      <Button type="button" size="sm" variant={chartView === "calendar" ? "secondary" : "ghost"} className="h-7 px-2.5 text-xs" onClick={() => onChartViewChange("calendar")}>日历图</Button>
      <Button type="button" size="sm" variant={chartView === "bars" ? "secondary" : "ghost"} className="h-7 px-2.5 text-xs" onClick={() => onChartViewChange("bars")}>柱状图</Button>
    </div> : null}
    {range !== "daily" && range !== "custom" && onPreviousPeriod && onNextPeriod ? <div className="flex items-center gap-1 rounded-md border border-border/60 bg-background p-1" aria-label="收益周期切换">
      <Button type="button" size="sm" variant="ghost" className="h-7 gap-1 px-2 text-xs" onClick={onPreviousPeriod} disabled={!canGoPrevious} aria-label="上一个周期" title={range === "monthly" ? "上个月" : "上一个周期"}><ChevronLeft className="size-3.5" />{range === "monthly" ? "上个月" : "上一个周期"}</Button>
      <span className="min-w-[92px] text-center text-xs font-medium tabular-nums">{periodLabel ?? "当前周期"}</span>
      <Button type="button" size="sm" variant="ghost" className="h-7 gap-1 px-2 text-xs" onClick={onNextPeriod} disabled={!canGoNext} aria-label="下一个周期" title={range === "monthly" ? "下个月" : "下一个周期"}>{range === "monthly" ? "下个月" : "下一个周期"}<ChevronRight className="size-3.5" /></Button>
    </div> : null}
  </div>
}

interface PortfolioPerformancePanelProps {
  state: PortfolioPerformanceState
  chartView: PortfolioChartView
  embedded?: boolean
  onChartViewChange: (view: PortfolioChartView) => void
}

export function PortfolioPerformancePanel({ state, chartView, embedded = false, onChartViewChange }: PortfolioPerformancePanelProps) {
  const { range, setRange, performance, loading, error } = state
  const chartData = useMemo(() => buildChartData(performance, range), [performance, range])

  return (
    <section className={embedded ? "min-w-0" : "min-w-0 overflow-hidden rounded-lg border border-border/60 bg-card"} aria-labelledby={embedded ? undefined : "performance-heading"} aria-label={embedded ? "收益走势" : undefined}>
      {!embedded ? <div className="flex flex-wrap items-start justify-between gap-3 border-b border-border/60 px-4 py-3.5">
        <div className="min-w-0">
          <div className="flex items-center gap-2"><ChartNoAxesCombined className="size-4 text-muted-foreground" aria-hidden="true" /><h2 id="performance-heading" className="text-base font-semibold">今日收益走势</h2></div>
          <p className="mt-1 text-xs text-muted-foreground">面积为当前视图累计收益，柱体为周期收益变化，红涨绿跌。</p>
        </div>
        <PortfolioPerformanceToolbar range={range} onRangeChange={setRange} chartView={chartView} onChartViewChange={onChartViewChange} />
      </div> : null}

      {loading ? <div className="space-y-4 p-4"><Skeleton className="h-9 w-2/3" /><Skeleton className="h-[300px] w-full" /></div> : error ? <div className="flex items-start gap-2 px-4 py-12 text-sm text-rose-700"><CircleAlert className="mt-0.5 size-4 shrink-0" /><div><p className="font-medium">收益历史加载失败</p><p className="mt-1 break-words text-xs">{error}</p></div></div> : performance && performance.points.length ? <div className="p-4">
        {chartView === "calendar" ? <PortfolioIncomeCalendar points={performance.points} range={range} /> : <ChartContainer config={chartConfig} className="h-[348px] w-full aspect-auto">
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
        </ChartContainer>}
        <div className="mt-3 flex items-center justify-between gap-3 border-t border-border/60 pt-3 text-[11px] text-muted-foreground"><span>{rangeLabel(range)} · {performance.from} 至 {performance.to}</span><span>数据点 {performance.points.length}</span></div>
      </div> : <div className="flex min-h-[380px] items-center justify-center px-4 py-10 text-center"><div><Loader2 className="mx-auto size-4 text-muted-foreground" /><p className="mt-2 text-sm font-medium">暂无可计算的历史收益</p><p className="mt-1 max-w-md text-xs leading-5 text-muted-foreground">需要已确认的交易和对应标的的日线行情；导入并确认交易后会自动出现。</p></div></div>}
    </section>
  )
}

export function PortfolioPerformanceSummary({ state, embedded = false }: { state: PortfolioPerformanceState; embedded?: boolean }) {
  const { range, performance, loading, error } = state
  const currentPeriod = performance?.points.at(-1)
  const chartData = useMemo(() => buildChartData(performance, range), [performance, range])
  const viewCumulativePnl = chartData.at(-1)?.cumulativePnl ?? 0
  const currentPeriodLabel = range === "daily" ? "今日收益" : range === "weekly" ? "本周收益" : range === "monthly" ? "本月收益" : "本年收益"

  return <section className={embedded ? "min-w-0" : "min-w-0 overflow-hidden rounded-lg border border-border/60 bg-card"} aria-labelledby="performance-summary-heading">
    {loading ? <div className="space-y-3 p-4">{Array.from({ length: 5 }, (_, index) => <Skeleton key={index} className="h-[62px] w-full" />)}</div> : error ? <div className="px-4 py-8 text-xs leading-5 text-rose-700">收益摘要加载失败：{error}</div> : performance && performance.points.length ? <dl className="divide-y divide-border/60" aria-label="收益摘要">
      <SummaryRow label="今日收益" value={money(performance.todayPnl ?? currentPeriod?.periodPnl)} toneClass={tone(performance.todayPnl ?? currentPeriod?.periodPnl)} detail="对比上一交易日收盘" />
      <SummaryRow label={currentPeriodLabel} value={money(currentPeriod?.periodPnl)} toneClass={tone(currentPeriod?.periodPnl)} detail={currentPeriod?.tradeDate ?? "等待行情"} />
      <SummaryRow label="视图收益率" value={percent(currentPeriod?.returnRate)} toneClass={tone(currentPeriod?.returnRate)} detail="按当前视图周期计算" />
      <SummaryRow label="期末总资产" value={amount(performance.endAssets)} toneClass="text-foreground" detail={`期初 ${amount(performance.startAssets)}`} />
      <SummaryRow label="视图累计收益" value={money(viewCumulativePnl)} toneClass={tone(viewCumulativePnl)} detail={`${performance.tradingDays} 个交易周期`} />
    </dl> : <div className="px-4 py-8 text-xs text-muted-foreground">暂无收益摘要。</div>}
  </section>
}

function SummaryRow({ label, value, toneClass, detail }: { label: string; value: string; toneClass: string; detail: string }) {
  return <div className="px-4 py-3"><dt className="text-xs text-muted-foreground">{label}</dt><dd className={`mt-1 truncate font-mono text-lg font-semibold tabular-nums ${toneClass}`}>{value}</dd><p className="mt-0.5 truncate text-[11px] text-muted-foreground">{detail}</p></div>
}

function buildChartData(performance: PortfolioPerformance | null, range: PortfolioPerformanceRange) {
  return (performance?.points ?? []).reduce<Array<PortfolioPerformancePoint & { cumulativePnl: number; label: string }>>((items, point) => {
    const cumulative = (items.at(-1)?.cumulativePnl ?? 0) + Number(point.periodPnl ?? 0)
    return [...items, { ...point, cumulativePnl: cumulative, label: performanceLabel(point.tradeDate, range) }]
  }, [])
}

function performanceLabel(value: string, range: PortfolioPerformanceRange) {
  const date = parseDate(value)
  if (range === "weekly") {
    const start = new Date(date)
    const end = new Date(start)
    end.setDate(end.getDate() + 6)
    return `${compactDate(start)}-${compactDate(end)}`
  }
  if (range === "monthly") {
    const start = new Date(date.getFullYear(), date.getMonth(), 1)
    const end = new Date(date.getFullYear(), date.getMonth() + 1, 0)
    return `${compactDate(start)}-${compactDate(end)}`
  }
  if (range === "yearly") return `${date.getFullYear()}年`
  return value.slice(5)
}

function parseDate(value: string) {
  const [year, month, day] = value.split("-").map(Number)
  return new Date(year, (month || 1) - 1, day || 1)
}

function compactDate(value: Date) {
  return `${String(value.getMonth() + 1).padStart(2, "0")}${String(value.getDate()).padStart(2, "0")}`
}

function rangeLabel(range: PortfolioPerformanceRange) {
  return range === "daily" ? "日视图" : range === "weekly" ? "周视图" : range === "monthly" ? "月视图" : "年视图"
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
