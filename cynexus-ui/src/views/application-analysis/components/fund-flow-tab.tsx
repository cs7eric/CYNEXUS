import { useCallback, useEffect, useMemo, useRef, useState, type ReactNode } from "react"
import { Activity, ArrowDownUp, RefreshCw, Scale, TrendingUp, Wallet } from "lucide-react"
import { Area, Bar, CartesianGrid, Cell, ComposedChart, Line, ReferenceLine, XAxis, YAxis } from "recharts"

import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import {
  ChartContainer,
  ChartTooltip,
  ChartTooltipContent,
  type ChartConfig,
} from "@/components/ui/chart"
import { cn } from "@/lib/utils"
import {
  backfillStockFundFlowHistory,
  fetchStockFundFlow,
  type StockFundFlowPoint,
  type StockFundFlowResponse,
} from "@/services/market/stock-chart"
import type { StockTargetType } from "../../stock-chart/lib/types"
import { backfillMarginStockHistory, fetchMarginStockHistory, type MarginStockDetail } from "@/services/market/margin"

export interface FundFlowTabProps {
  targetType: StockTargetType
  symbol: string
  name: string
}

function isFiniteNumber(value: number | null | undefined): value is number {
  return typeof value === "number" && Number.isFinite(value)
}

function formatWan(value: number | null | undefined) {
  if (!isFiniteNumber(value)) return "—"
  const abs = Math.abs(value)
  if (abs >= 10000) return `${(value / 10000).toFixed(2)}亿`
  return `${value.toFixed(0)}万`
}

function formatPct(value: number | null | undefined) {
  if (!isFiniteNumber(value)) return "—"
  return `${value > 0 ? "+" : ""}${value.toFixed(2)}%`
}

function formatRatio(value: number | null | undefined) {
  if (!isFiniteNumber(value)) return "—"
  const normalized = Math.abs(value) <= 1 ? value * 100 : value
  return `${normalized.toFixed(2)}%`
}

function formatPrice(value: number | null | undefined) {
  return isFiniteNumber(value) ? value.toFixed(2) : "—"
}

function formatYi(value: number | null | undefined) {
  return isFiniteNumber(value) ? `${(value / 100_000_000).toFixed(2)}亿` : "—"
}

function focusedDomain(values: Array<number | null>) : [number, number] {
  const finite = values.filter(isFiniteNumber)
  if (!finite.length) return [0, 1]
  const minimum = Math.min(...finite)
  const maximum = Math.max(...finite)
  const spread = maximum - minimum
  const padding = spread > 0 ? spread * 0.12 : Math.max(Math.abs(maximum) * 0.04, 0.01)
  return [Math.max(0, minimum - padding), maximum + padding]
}

function signedTone(value: number | null | undefined) {
  if (!isFiniteNumber(value)) return "text-slate-700"
  if (value > 0) return "text-red-600"
  if (value < 0) return "text-emerald-600"
  return "text-slate-700"
}

function flowBias(value: number | null | undefined) {
  if (!isFiniteNumber(value)) return { label: "No data", cls: "border-slate-200 bg-slate-50 text-slate-600" }
  if (value >= 5000) return { label: "Strong inflow", cls: "border-red-200 bg-red-50 text-red-700" }
  if (value > 0) return { label: "Inflow", cls: "border-red-200 bg-red-50 text-red-700" }
  if (value <= -5000) return { label: "Strong outflow", cls: "border-emerald-200 bg-emerald-50 text-emerald-700" }
  if (value < 0) return { label: "Outflow", cls: "border-emerald-200 bg-emerald-50 text-emerald-700" }
  return { label: "Balanced", cls: "border-slate-200 bg-slate-50 text-slate-600" }
}

function primaryNet(item: Pick<StockFundFlowPoint, "mainNetAmountWan" | "activeNetAmountWan">) {
  return item.mainNetAmountWan ?? item.activeNetAmountWan
}

function useLatestScroll(itemCount: number) {
  const ref = useRef<HTMLDivElement>(null)
  useEffect(() => {
    const element = ref.current
    if (element) element.scrollLeft = element.scrollWidth
  }, [itemCount])
  return ref
}

function MetricChip({
  icon,
  label,
  value,
  tone,
}: {
  icon?: ReactNode
  label: string
  value: string
  tone?: string
}) {
  return (
    <span className="inline-flex items-center gap-1.5 rounded-full bg-slate-50 px-2 py-1 text-[11px] text-slate-500">
      {icon ? <span className="text-slate-400">{icon}</span> : null}
      <span>{label}</span>
      <span className={cn("font-mono text-sm font-semibold leading-none", tone ?? "text-slate-700")}>{value}</span>
    </span>
  )
}

function SummaryBlock({
  label,
  value,
  caption,
  tone,
}: {
  label: string
  value: string
  caption?: string
  tone?: string
}) {
  return (
    <div className="min-w-0 rounded-xl bg-slate-50/70 p-3">
      <div className="text-[11px] text-slate-500">{label}</div>
      <div className={cn("mt-1 truncate font-mono text-lg font-semibold", tone ?? "text-slate-900")}>{value}</div>
      {caption ? <div className="mt-1 truncate text-[11px] text-slate-500">{caption}</div> : null}
    </div>
  )
}

function FiveDayFundTrend({ items }: { items: StockFundFlowPoint[] }) {
  const hasMainFlow = items.some((item) => isFiniteNumber(item.mainNetAmountWan))
  const recent = useMemo(
    () => [...items]
      .sort((left, right) => right.trade_date.localeCompare(left.trade_date))
      .filter((item) => hasMainFlow ? isFiniteNumber(item.mainNetAmountWan) : isFiniteNumber(item.activeNetAmountWan))
      .slice(0, 5),
    [hasMainFlow, items],
  )
  const netValue = (item: StockFundFlowPoint) => hasMainFlow ? item.mainNetAmountWan : item.activeNetAmountWan
  const totalNet = recent.length ? recent.reduce((sum, item) => sum + (netValue(item) ?? 0), 0) : null
  const inflowDays = recent.filter((item) => (netValue(item) ?? 0) > 0).length
  const outflowDays = recent.filter((item) => (netValue(item) ?? 0) < 0).length
  const totalAmount = recent.reduce((sum, item) => sum + (item.amountWan ?? 0), 0)
  const buyRatioItems = recent.filter((item) => isFiniteNumber(item.activeBuyRatio))
  const avgBuyRatio = buyRatioItems.length
    ? buyRatioItems.reduce((sum, item) => sum + (item.activeBuyRatio ?? 0), 0) / buyRatioItems.length
    : null
  const bias = flowBias(totalNet)

  if (!recent.length) return null

  return (
    <div className="rounded-xl bg-white/70 px-3 py-2.5">
      <div className="flex flex-col gap-2 xl:flex-row xl:items-center xl:justify-between">
        <div className="flex min-w-0 flex-wrap items-center gap-x-2 gap-y-1">
          <span className="text-sm font-semibold text-slate-900">{hasMainFlow ? "近5日主力趋势" : "近5日主动资金趋势"}</span>
          <Badge className={cn("rounded-full border px-2 py-0 text-[10px] font-medium", bias.cls)} variant="outline">
            {bias.label}
          </Badge>
          <span className="text-[11px] text-slate-300">·</span>
          <span className={cn("font-mono text-sm font-semibold", signedTone(totalNet))}>{formatWan(totalNet)}</span>
        </div>
        <div className="flex min-w-0 flex-wrap items-center gap-1.5">
          <MetricChip label="In/Out" value={`${inflowDays}/${outflowDays}`} />
          <MetricChip label="Amount" value={formatWan(totalAmount)} />
          <MetricChip label="Buy" value={formatRatio(avgBuyRatio)} />
        </div>
      </div>

      <div className="mt-2 flex min-w-0 flex-wrap items-center gap-1.5">
        {recent.map((item) => (
          <span
            key={item.trade_date}
            className={cn(
              "inline-flex items-center gap-1 rounded-full px-2 py-1 text-[11px]",
              !isFiniteNumber(netValue(item))
                ? "bg-slate-50 text-slate-500"
                : (netValue(item) ?? 0) >= 0 ? "bg-red-50 text-red-600" : "bg-emerald-50 text-emerald-600",
            )}
          >
            <span className="font-mono text-slate-500">{item.trade_date.slice(5)}</span>
            <span className="font-mono font-semibold">{formatWan(netValue(item))}</span>
          </span>
        ))}
      </div>
    </div>
  )
}

const fundTrendChartConfig = {
  net: { label: "Main Net", color: "#64748b" },
  average: { label: "5D Average", color: "#2563eb" },
} satisfies ChartConfig

function FundFlowTrendChart({
  items,
  backfilling,
  onBackfill,
}: {
  items: StockFundFlowPoint[]
  backfilling: boolean
  onBackfill: () => void
}) {
  const data = useMemo(() => {
    const ordered = [...items]
      .sort((left, right) => left.trade_date.localeCompare(right.trade_date))
      .filter((item) => isFiniteNumber(primaryNet(item)))

    return ordered.map((item, index) => {
      const window = ordered.slice(Math.max(0, index - 4), index + 1)
      const average = window.reduce((sum, point) => sum + (primaryNet(point) ?? 0), 0) / window.length
      return {
        date: item.trade_date,
        net: primaryNet(item) ?? 0,
        average,
      }
    })
  }, [items])

  const scrollRef = useLatestScroll(data.length)
  const chartWidth = Math.max(760, Math.ceil(data.length / 30) * 760)
  if (!data.length) return null

  const latest = data[data.length - 1]
  const peak = data.reduce((best, item) => Math.abs(item.net) > Math.abs(best.net) ? item : best, data[0])
  return (
    <div className="min-w-0 rounded-xl bg-white/70 px-3 py-2.5">
      <div className="mb-2 flex items-center justify-between gap-4">
        <div className="flex min-w-0 items-baseline gap-2">
          <span className="text-sm font-semibold text-slate-900">Flow Trend</span>
          <span className="text-[11px] text-slate-500">30D · daily net · 5D average</span>
        </div>
        <div className="flex shrink-0 items-center gap-3 text-[11px]">
          <span className="text-slate-500">Latest <b className={cn("font-mono font-semibold", signedTone(latest.net))}>{formatWan(latest.net)}</b></span>
          <span className="text-slate-500">Peak <b className={cn("font-mono font-semibold", signedTone(peak.net))}>{formatWan(peak.net)}</b></span>
          <Button type="button" size="sm" variant="secondary" className="h-6 gap-1 px-2 text-[10px]" disabled={backfilling} onClick={onBackfill}>
            <RefreshCw className={cn("size-3", backfilling && "animate-spin")} />
            Backfill
          </Button>
        </div>
      </div>

      <div ref={scrollRef} className="min-w-0 overflow-x-auto pb-1">
        <ChartContainer config={fundTrendChartConfig} className="h-[190px] shrink-0" style={{ width: chartWidth }}>
          <ComposedChart
            data={data}
            margin={{ top: 8, right: 4, bottom: 0, left: 0 }}
            syncId="application-analysis-fund-margin"
            syncMethod="value"
          >
          <defs>
            <linearGradient id="fundFlowAverageFill" x1="0" x2="0" y1="0" y2="1">
              <stop offset="5%" stopColor="var(--color-average)" stopOpacity={0.2} />
              <stop offset="95%" stopColor="var(--color-average)" stopOpacity={0.01} />
            </linearGradient>
            <filter id="fundFlowLineShadow" x="-20%" y="-30%" width="140%" height="160%">
              <feDropShadow dx="0" dy="3" stdDeviation="3" floodColor="#2563eb" floodOpacity="0.24" />
            </filter>
          </defs>
          <CartesianGrid vertical={false} strokeDasharray="3 4" strokeOpacity={0.36} />
          <XAxis
            dataKey="date"
            axisLine={false}
            tickLine={false}
            tickMargin={8}
            minTickGap={28}
            tickFormatter={(value) => String(value).slice(5)}
          />
          <YAxis
            axisLine={false}
            tickLine={false}
            width={56}
            tickFormatter={(value) => formatWan(Number(value))}
          />
          <ReferenceLine y={0} stroke="#94a3b8" strokeOpacity={0.45} />
          <ChartTooltip
            cursor={{ fill: "rgba(148, 163, 184, 0.08)" }}
            content={
              <ChartTooltipContent
                labelFormatter={(value) => String(value)}
                formatter={(value, name) => (
                  <div className="flex min-w-36 items-center justify-between gap-4">
                    <span className="text-muted-foreground">{fundTrendChartConfig[String(name) as keyof typeof fundTrendChartConfig]?.label ?? name}</span>
                    <span className={cn("font-mono font-semibold tabular-nums", signedTone(Number(value)))}>{formatWan(Number(value))}</span>
                  </div>
                )}
              />
            }
          />
          <Bar dataKey="net" name="net" maxBarSize={18} radius={[2, 2, 2, 2]} isAnimationActive={false}>
            {data.map((item) => (
              <Cell
                key={item.date}
                fill={item.net >= 0 ? "#ef4444" : "#10b981"}
                fillOpacity={0.7}
              />
            ))}
          </Bar>
          <Area
            dataKey="average"
            name="average"
            type="natural"
            stroke="none"
            fill="url(#fundFlowAverageFill)"
            dot={false}
            activeDot={false}
            isAnimationActive={false}
          />
          <Line
            dataKey="average"
            name="average"
            type="monotone"
            stroke="var(--color-average)"
            strokeWidth={2.2}
            style={{ filter: "url(#fundFlowLineShadow)" }}
            dot={false}
            activeDot={{ r: 3 }}
            isAnimationActive={false}
          />
          </ComposedChart>
        </ChartContainer>
      </div>
    </div>
  )
}

function MarginTrend({
  items,
  backfilling,
  onBackfill,
}: {
  items: MarginStockDetail[]
  backfilling: boolean
  onBackfill: () => void
}) {
  const data = useMemo(() => [...items].reverse().map((item) => ({
    ...item,
    date: item.tradeDate,
    financingBalance: isFiniteNumber(item.financingBalance) ? item.financingBalance / 100_000_000 : null,
    financingBuyAmount: isFiniteNumber(item.financingBuyAmount) ? item.financingBuyAmount / 100_000_000 : null,
    financingRepayAmount: isFiniteNumber(item.financingRepayAmount) ? item.financingRepayAmount / 100_000_000 : null,
  })), [items])
  const scrollRef = useLatestScroll(data.length)
  const chartWidth = Math.max(760, Math.ceil(data.length / 30) * 760)
  if (!data.length) return null
  const latest = items[0]
  const balanceDomain = focusedDomain(data.map((item) => item.financingBalance))
  const activityDomain = focusedDomain(data.flatMap((item) => [item.financingBuyAmount, item.financingRepayAmount]))
  const netFinancing = isFiniteNumber(latest.financingBuyAmount) && isFiniteNumber(latest.financingRepayAmount)
    ? latest.financingBuyAmount - latest.financingRepayAmount
    : null

  return (
    <div className="min-w-0 rounded-xl bg-white/70 px-3 py-2.5">
      <div className="mb-2 flex items-center justify-between gap-4">
        <div className="flex min-w-0 items-baseline gap-2">
          <span className="text-sm font-semibold text-slate-900">Margin Trading</span>
          <span className="text-[11px] text-slate-500">30D · focused scales · buy / repay</span>
        </div>
        <div className="flex shrink-0 items-center gap-3 text-[11px] text-slate-500">
          <span>Balance <b className="font-mono font-semibold text-slate-800">{formatYi(latest.financingBalance)}</b></span>
          <span>Net <b className={cn("font-mono font-semibold", signedTone(netFinancing))}>{formatYi(netFinancing)}</b></span>
          <Button
            type="button"
            size="sm"
            variant="secondary"
            className="h-6 gap-1 px-2 text-[10px]"
            disabled={backfilling}
            onClick={onBackfill}
          >
            <RefreshCw className={cn("size-3", backfilling && "animate-spin")} />
            Backfill
          </Button>
        </div>
      </div>
      <div ref={scrollRef} className="min-w-0 overflow-x-auto pb-1">
        <ChartContainer
          config={{
            financingBalance: { label: "Financing Balance", color: "#2563eb" },
            financingBuyAmount: { label: "Buy", color: "#ef4444" },
            financingRepayAmount: { label: "Repay", color: "#10b981" },
          }}
          className="h-[190px] shrink-0"
          style={{ width: chartWidth }}
        >
          <ComposedChart
            data={data}
            margin={{ top: 8, right: 4, bottom: 0, left: 0 }}
            syncId="application-analysis-fund-margin"
            syncMethod="value"
          >
          <defs>
            <linearGradient id="marginBalanceFill" x1="0" x2="0" y1="0" y2="1">
              <stop offset="5%" stopColor="var(--color-financingBalance)" stopOpacity={0.22} />
              <stop offset="95%" stopColor="var(--color-financingBalance)" stopOpacity={0.01} />
            </linearGradient>
          </defs>
          <CartesianGrid vertical={false} strokeDasharray="3 4" strokeOpacity={0.36} />
          <XAxis dataKey="date" axisLine={false} tickLine={false} tickMargin={8} tickFormatter={(value) => String(value).slice(5)} />
          <YAxis
            yAxisId="balance"
            axisLine={false}
            tickLine={false}
            width={54}
            domain={balanceDomain}
            allowDataOverflow
            tickFormatter={(value) => `${Number(value).toFixed(1)}亿`}
          />
          <YAxis
            yAxisId="activity"
            orientation="right"
            axisLine={false}
            tickLine={false}
            width={48}
            domain={activityDomain}
            allowDataOverflow
            tickFormatter={(value) => `${Number(value).toFixed(1)}亿`}
          />
          <ChartTooltip
            cursor={{ fill: "rgba(148, 163, 184, 0.08)" }}
            content={<ChartTooltipContent labelFormatter={(value) => String(value)} indicator="dot" />}
          />
          <Area yAxisId="balance" dataKey="financingBalance" name="financingBalance" type="natural" stroke="var(--color-financingBalance)" strokeWidth={2.2} fill="url(#marginBalanceFill)" dot={false} activeDot={{ r: 3.5, fill: "white", strokeWidth: 2 }} isAnimationActive={false} />
          <Line yAxisId="activity" dataKey="financingBuyAmount" name="financingBuyAmount" type="monotone" stroke="var(--color-financingBuyAmount)" strokeWidth={1.5} dot={false} isAnimationActive={false} />
          <Line yAxisId="activity" dataKey="financingRepayAmount" name="financingRepayAmount" type="monotone" stroke="var(--color-financingRepayAmount)" strokeWidth={1.5} dot={false} isAnimationActive={false} />
          </ComposedChart>
        </ChartContainer>
      </div>
      <div className="mt-1 flex items-center gap-3 text-[10px] text-slate-500">
        <span>{latest.tradeDate}</span>
        <span>{latest.exchange}</span>
        <span className="truncate">{latest.source}</span>
      </div>
    </div>
  )
}

function FundFlowDetail({
  item,
  snapshot,
}: {
  item: StockFundFlowPoint
  snapshot: StockFundFlowResponse | null
}) {
  const isLatest = snapshot?.trade_date === item.trade_date
  const hasOrderSplit = [
    item.mainNetAmountWan,
    item.superNetAmountWan,
    item.largeNetAmountWan,
    item.mediumNetAmountWan,
    item.smallNetAmountWan,
  ].some(isFiniteNumber)

  return (
    <div className="min-w-0 space-y-2 pt-1">
      {hasOrderSplit ? (
        <div className="grid gap-2 lg:grid-cols-5">
          <SummaryBlock label="主力净额" value={formatWan(item.mainNetAmountWan)} tone={signedTone(item.mainNetAmountWan)} />
          <SummaryBlock label="超大单净额" value={formatWan(item.superNetAmountWan)} tone={signedTone(item.superNetAmountWan)} />
          <SummaryBlock label="大单净额" value={formatWan(item.largeNetAmountWan)} tone={signedTone(item.largeNetAmountWan)} />
          <SummaryBlock label="中单净额" value={formatWan(item.mediumNetAmountWan)} tone={signedTone(item.mediumNetAmountWan)} />
          <SummaryBlock label="小单净额" value={formatWan(item.smallNetAmountWan)} tone={signedTone(item.smallNetAmountWan)} />
        </div>
      ) : (
        <div className="grid gap-2 lg:grid-cols-4">
          <SummaryBlock label="主动净额" value={formatWan(item.activeNetAmountWan)} tone={signedTone(item.activeNetAmountWan)} />
          <SummaryBlock label="成交额" value={formatWan(item.amountWan)} />
          <SummaryBlock label="主动买入占比" value={formatRatio(item.activeBuyRatio)} caption={`主动卖出 ${formatRatio(item.activeSellRatio)}`} />
          <SummaryBlock label="涨跌幅" value={formatPct(item.changePct)} tone={signedTone(item.changePct)} />
        </div>
      )}

      {isLatest ? (
        <>
          <div className="grid gap-2 lg:grid-cols-4">
            <SummaryBlock label="最新价" value={formatPrice(snapshot?.lastPrice)} caption={snapshot?.trade_date ? `交易日 ${snapshot.trade_date}` : undefined} />
            <SummaryBlock label="换手率" value={formatPct(snapshot?.turnoverRate)} />
            <SummaryBlock label="大单买入占比" value={formatRatio(snapshot?.buyBigRatio)} />
            <SummaryBlock label="大单卖出占比" value={formatRatio(snapshot?.sellBigRatio)} />
          </div>
          <div className="flex flex-wrap items-center gap-1.5 rounded-xl bg-slate-50/70 px-3 py-2 text-[11px] text-slate-500">
            <span>Source</span>
            <Badge variant="outline" className="rounded-full bg-white px-2 py-0 text-[10px] text-slate-600">{snapshot?.source || "—"}</Badge>
            {snapshot?.captured_at ? (
              <>
                <span className="text-slate-300">·</span>
                <span>Captured {new Date(snapshot.captured_at).toLocaleString("zh-CN", { hour12: false })}</span>
              </>
            ) : null}
          </div>
        </>
      ) : null}
    </div>
  )
}

function DateItem({
  item,
  latest,
  snapshot,
}: {
  item: StockFundFlowPoint
  latest: boolean
  snapshot: StockFundFlowResponse | null
}) {
  const [expanded, setExpanded] = useState(latest)
  const net = primaryNet(item)
  const netLabel = isFiniteNumber(item.mainNetAmountWan) ? "Main Net" : "Active Net"
  const bias = flowBias(net)

  return (
    <div className="min-w-0 rounded-xl bg-white/70">
      <button
        type="button"
        onClick={() => setExpanded((value) => !value)}
        className="sticky top-0 z-10 flex w-full items-center rounded-xl bg-white/95 px-3 py-2.5 text-left transition hover:bg-slate-50/90"
      >
        <div className="flex min-w-0 flex-1 flex-wrap items-center gap-x-3 gap-y-1.5">
          <div className="flex min-w-0 flex-wrap items-center gap-x-2 gap-y-1">
            <span className="font-mono text-sm font-semibold text-slate-900">{item.trade_date}</span>
            {latest ? <Badge className="rounded-full border-slate-200 bg-slate-50 px-1.5 py-0 text-[10px] text-slate-600" variant="outline">Latest</Badge> : null}
            <Badge className={cn("rounded-full border px-1.5 py-0 text-[10px]", bias.cls)} variant="outline">{bias.label}</Badge>
          </div>
          <div className="flex min-w-0 flex-wrap items-center gap-1.5">
            <MetricChip icon={<Wallet className="size-3" />} label={netLabel} value={formatWan(net)} tone={signedTone(net)} />
            <MetricChip icon={<Activity className="size-3" />} label="Amount" value={formatWan(item.amountWan)} />
            <MetricChip icon={<TrendingUp className="size-3" />} label="Change" value={formatPct(item.changePct)} tone={signedTone(item.changePct)} />
            <MetricChip icon={<Scale className="size-3" />} label="Buy" value={formatRatio(item.activeBuyRatio)} />
          </div>
        </div>
      </button>
      {expanded ? (
        <div className="px-3 pb-3">
          <FundFlowDetail item={item} snapshot={snapshot} />
        </div>
      ) : null}
    </div>
  )
}

export function FundFlowTab({ symbol, name }: FundFlowTabProps) {
  const [data, setData] = useState<StockFundFlowResponse | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState("")
  const [marginItems, setMarginItems] = useState<MarginStockDetail[]>([])
  const [marginBackfilling, setMarginBackfilling] = useState(false)
  const [fundFlowBackfilling, setFundFlowBackfilling] = useState(false)

  const refresh = useCallback(async () => {
    try {
      setLoading(true)
      setError("")
      const [fundFlow, margin] = await Promise.allSettled([
        fetchStockFundFlow(symbol),
        fetchMarginStockHistory(symbol, 120),
      ])
      setMarginItems(margin.status === "fulfilled" ? margin.value.items : [])
      if (fundFlow.status === "rejected") throw fundFlow.reason
      setData(fundFlow.value)
    } catch (err) {
      setData(null)
      setError(err instanceof Error ? err.message : "加载资金流失败")
    } finally {
      setLoading(false)
    }
  }, [symbol])

  const backfillMargin = useCallback(async () => {
    try {
      setMarginBackfilling(true)
      setError("")
      await backfillMarginStockHistory(symbol)
      const margin = await fetchMarginStockHistory(symbol, 120)
      setMarginItems(margin.items)
    } catch (err) {
      setError(err instanceof Error ? err.message : "Margin history backfill failed")
    } finally {
      setMarginBackfilling(false)
    }
  }, [symbol])

  const backfillFundFlow = useCallback(async () => {
    try {
      setFundFlowBackfilling(true)
      setError("")
      await backfillStockFundFlowHistory(symbol)
      setData(await fetchStockFundFlow(symbol))
    } catch (err) {
      setError(err instanceof Error ? err.message : "Fund-flow history backfill failed")
    } finally {
      setFundFlowBackfilling(false)
    }
  }, [symbol])

  useEffect(() => {
    void refresh()
  }, [refresh])

  const items = useMemo(() => {
    const history = data?.history ?? []
    if (history.length) {
      return [...history].sort((left, right) => right.trade_date.localeCompare(left.trade_date))
    }
    if (!data?.trade_date) return []
    return [{
      trade_date: data.trade_date,
      activeNetAmountWan: data.activeNetAmountWan ?? null,
      mainNetAmountWan: data.mainNetAmountWan ?? null,
      superNetAmountWan: data.superNetAmountWan ?? null,
      largeNetAmountWan: data.largeNetAmountWan ?? null,
      mediumNetAmountWan: data.mediumNetAmountWan ?? null,
      smallNetAmountWan: data.smallNetAmountWan ?? null,
      activeBuyRatio: data.activeBuyRatio ?? null,
      activeSellRatio: data.activeSellRatio ?? null,
      amountWan: data.amountWan ?? null,
      changePct: data.changePct ?? null,
    }]
  }, [data])
  const latestDate = items[0]?.trade_date
  const latestNet = data?.mainNetAmountWan ?? data?.activeNetAmountWan
  const latestNetLabel = isFiniteNumber(data?.mainNetAmountWan) ? "Main Net" : "Active Net"
  const latestBias = flowBias(latestNet)
  const dataTip = error || data?.error || ""

  return (
    <section className="min-w-0 space-y-2 rounded-2xl bg-muted/50 p-3">
      <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <div className="text-sm font-semibold text-slate-900">Fund Flow</div>
          <div className="text-[11px] text-slate-500">Stock fund-flow snapshots · newest first · {items.length} sessions</div>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          {latestDate ? <Badge variant="outline" className="w-fit rounded-full bg-white px-2 py-0 text-[10px]">Latest {latestDate}</Badge> : null}
          <Button size="sm" variant="secondary" onClick={() => void refresh()} disabled={loading} className="h-7 gap-1.5 rounded-lg px-2.5 text-[11px]">
            <RefreshCw className={cn("size-3.5", loading && "animate-spin")} />
            Refresh
          </Button>
        </div>
      </div>

      <div className="rounded-xl bg-white/70 px-3 py-2.5">
        <div className="flex flex-col gap-2 xl:flex-row xl:items-center xl:justify-between">
          <div className="flex min-w-0 flex-wrap items-center gap-x-2 gap-y-1">
            <span className="text-sm font-semibold text-slate-900">{data?.name || name}</span>
            <span className="font-mono text-[11px] text-slate-500">{data?.symbol || symbol}</span>
            <Badge className={cn("rounded-full border px-2 py-0 text-[10px]", latestBias.cls)} variant="outline">{latestBias.label}</Badge>
          </div>
          <div className="flex min-w-0 flex-wrap items-center gap-1.5">
            <MetricChip icon={<Wallet className="size-3" />} label={latestNetLabel} value={formatWan(latestNet)} tone={signedTone(latestNet)} />
            <MetricChip icon={<Activity className="size-3" />} label="Amount" value={formatWan(data?.amountWan)} />
            <MetricChip icon={<ArrowDownUp className="size-3" />} label="Buy/Sell" value={`${formatRatio(data?.activeBuyRatio)} / ${formatRatio(data?.activeSellRatio)}`} />
            <MetricChip icon={<TrendingUp className="size-3" />} label="Change" value={formatPct(data?.changePct)} tone={signedTone(data?.changePct)} />
          </div>
        </div>
      </div>

      <FiveDayFundTrend items={items} />
      <div className="grid min-w-0 gap-2 xl:grid-cols-2">
        <FundFlowTrendChart items={items} backfilling={fundFlowBackfilling} onBackfill={() => void backfillFundFlow()} />
        <MarginTrend items={marginItems} backfilling={marginBackfilling} onBackfill={() => void backfillMargin()} />
      </div>

      {dataTip ? (
        <div className="rounded-xl bg-amber-50/70 px-3 py-2 text-[11px] leading-5 text-amber-700">
          数据提示：{dataTip}
        </div>
      ) : null}

      {items.length === 0 ? (
        <div className="rounded-xl border border-dashed border-slate-200 bg-white px-4 py-6 text-center text-xs text-slate-500">
          {loading ? "Loading fund-flow snapshots..." : "No stock fund-flow snapshots yet"}
        </div>
      ) : (
        <div className="space-y-1.5">
          {items.map((item) => (
            <DateItem key={item.trade_date} item={item} latest={item.trade_date === latestDate} snapshot={data} />
          ))}
        </div>
      )}
    </section>
  )
}
