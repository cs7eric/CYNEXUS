import { Activity, BarChart3, Gauge, LineChart as LineChartIcon, WalletCards } from "lucide-react"
import {
  Bar,
  CartesianGrid,
  ComposedChart,
  Line,
  ReferenceLine,
  XAxis,
  YAxis,
} from "recharts"

import {
  ChartContainer,
  ChartTooltip,
  ChartTooltipContent,
  type ChartConfig,
} from "@/components/ui/chart"
import { cn } from "@/lib/utils"
import { formatDate, formatYuanAsYi } from "../lib/format"
import {
  FinancingBalanceInsight,
  type MarginInsightPoint,
} from "./margin-insights"
import { isUsableMarginHistoryPoint } from "../lib/margin-history"

interface MarginOverviewChartsProps {
  points: MarginInsightPoint[]
  fallback?: MarginInsightPoint
  title?: string
  bare?: boolean
}

const overviewChartConfig = {
  financingBalance: { label: "融资余额", color: "#0f766e" },
  netFinancingBuy: { label: "融资净买入", color: "#2563eb" },
} satisfies ChartConfig

function finite(value: number | null | undefined): value is number {
  return value != null && Number.isFinite(value)
}

function orderedPoints(points: MarginInsightPoint[], fallback?: MarginInsightPoint) {
  const source = points.filter(isUsableMarginHistoryPoint)
  if (fallback && isUsableMarginHistoryPoint(fallback) && !source.some((item) => item.tradeDate === fallback.tradeDate)) {
    source.push(fallback)
  }
  return source.sort((left, right) => left.tradeDate.localeCompare(right.tradeDate))
}

function signedYuan(value: number | null) {
  if (!finite(value)) return "-"
  return `${value >= 0 ? "+" : ""}${formatYuanAsYi(value, 2)}`
}

function signedPercent(value: number | null) {
  if (!finite(value)) return "-"
  return `${value >= 0 ? "+" : ""}${value.toFixed(2)}%`
}

function latestChange(points: MarginInsightPoint[]) {
  const values = points.filter((item) => finite(item.financingBalance))
  if (values.length < 2) return null
  const latest = values.at(-1)?.financingBalance
  const previous = values.at(-2)?.financingBalance
  if (!finite(latest) || !finite(previous)) return null
  return {
    value: latest - previous,
    percent: previous === 0 ? null : ((latest - previous) / Math.abs(previous)) * 100,
  }
}

function OverviewMetric({
  label,
  value,
  detail,
  icon: Icon,
  tone = "neutral",
  compact = false,
}: {
  label: string
  value: React.ReactNode
  detail?: string
  icon: typeof WalletCards
  tone?: "neutral" | "up" | "down"
  compact?: boolean
}) {
  return (
    <div className={cn(
      "min-w-0",
      compact ? "border-t border-border/60 py-3 first:border-t-0" : "border-border/60 px-3.5 py-3 first:border-l-0 sm:border-l",
    )}>
      <div className="flex items-center gap-1.5 text-xs text-muted-foreground">
        {!compact ? <Icon className="size-3.5 shrink-0" /> : null}
        <span className="truncate">{label}</span>
      </div>
      <div className={cn(
        "mt-1.5 truncate font-semibold tabular-nums tracking-tight",
        compact ? "text-base" : "text-lg",
        tone === "up" && "text-red-600 dark:text-red-400",
        tone === "down" && "text-emerald-600 dark:text-emerald-400",
      )}>{value}</div>
      {detail ? <div className="mt-1 truncate text-[11px] text-muted-foreground">{detail}</div> : null}
    </div>
  )
}

function ChartShell({
  title,
  icon: Icon,
  children,
  bare = false,
}: {
  title: string
  icon: typeof Activity
  children: React.ReactNode
  bare?: boolean
}) {
  return (
    <section className={cn("min-w-0", !bare && "rounded-lg border border-border/70 bg-background p-3.5")}>
      <div className="flex items-center justify-between gap-2">
        <div className="flex items-center gap-2">
          <Icon className="size-4 text-muted-foreground" />
          <h3 className="text-sm font-semibold">{title}</h3>
        </div>
        <span className="text-[11px] text-muted-foreground">近 60 个交易日</span>
      </div>
      {children}
    </section>
  )
}

export function MarginOverviewCharts({ points, fallback, title, bare = false }: MarginOverviewChartsProps) {
  const history = orderedPoints(points, fallback)
  const current = history.at(-1)
  const change = latestChange(history)
  const netFinancingBuy = finite(current?.financingBuyAmount) && finite(current?.financingRepayAmount)
    ? current.financingBuyAmount - current.financingRepayAmount
    : null
  const leverage = finite(current?.financingBalance) && finite(current?.marginBalance)
    ? (current.financingBalance / Math.max(current.marginBalance, 1)) * 100
    : null
  const financingBuyRatio = finite(current?.financingBuyAmount) && finite(current?.turnoverAmount) && current.turnoverAmount > 0
    ? (current.financingBuyAmount / current.turnoverAmount) * 100
    : null
  const chartData = history.map((item) => ({
    date: item.tradeDate,
    financingBalance: finite(item.financingBalance) ? item.financingBalance / 100_000_000 : null,
    netFinancingBuy: finite(item.financingBuyAmount) && finite(item.financingRepayAmount)
      ? (item.financingBuyAmount - item.financingRepayAmount) / 100_000_000
      : null,
  }))
  const trendTone = change == null ? "neutral" : change.value >= 0 ? "up" : "down"
  const buyTone = netFinancingBuy == null ? "neutral" : netFinancingBuy >= 0 ? "up" : "down"

  return (
    <section className={cn("min-w-0", !bare && "rounded-xl border border-border/70 bg-card p-4 shadow-sm")}>
      {title ? (
        <div className="flex items-start justify-between gap-3">
          <div>
            <div className="flex items-center gap-2">
              <h2 className="text-base font-semibold tracking-tight">{title}</h2>
              {!bare ? <span className="rounded-full bg-muted px-2 py-0.5 text-[11px] text-muted-foreground">快照</span> : null}
            </div>
            <p className="mt-1 text-xs text-muted-foreground">存量看融资余额，行为看净买入，结构看杠杆水平。</p>
          </div>
          <Gauge className="mt-0.5 size-4 shrink-0 text-muted-foreground" />
        </div>
      ) : null}

      {!bare ? (
        <div className="mt-4 grid divide-y border-y border-border/60 sm:grid-cols-4 sm:divide-x sm:divide-y-0">
          <OverviewMetric label="融资余额" value={formatYuanAsYi(current?.financingBalance, 2)} detail="未偿还融资借款" icon={WalletCards} />
          <OverviewMetric label="今日净买入" value={signedYuan(netFinancingBuy)} detail="融资买入 - 融资偿还" icon={Activity} tone={buyTone} />
          <OverviewMetric label="融资余额变化" value={signedPercent(change?.percent ?? null)} detail="较上一交易日" icon={LineChartIcon} tone={trendTone} />
          <OverviewMetric label="杠杆水平" value={leverage == null ? "-" : `${leverage.toFixed(1)}%`} detail="融资余额 / 两融余额" icon={BarChart3} />
        </div>
      ) : null}

      <div className={cn("mt-4 min-w-0 gap-5", !title && "mt-0", bare && "xl:grid xl:grid-cols-[minmax(0,1fr)_220px]")}>
        <div className="min-w-0">
          <ChartShell title="融资余额与净买入" icon={LineChartIcon} bare={bare}>
            {chartData.length >= 2 ? (
              <ChartContainer config={overviewChartConfig} className="mt-3 h-60 w-full aspect-auto">
                <ComposedChart data={chartData} margin={{ top: 8, right: -2, bottom: 0, left: -12 }}>
                  <CartesianGrid vertical={false} strokeDasharray="3 3" />
                  <XAxis dataKey="date" tickLine={false} axisLine={false} tickMargin={8} minTickGap={24} tickFormatter={(value) => String(value).slice(5)} />
                  <YAxis yAxisId="balance" tickLine={false} axisLine={false} width={48} tickFormatter={(value) => Number(value).toLocaleString("zh-CN", { maximumFractionDigits: 0 })} />
                  <YAxis yAxisId="netBuy" orientation="right" tickLine={false} axisLine={false} width={48} tickFormatter={(value) => Number(value).toLocaleString("zh-CN", { maximumFractionDigits: 0 })} />
                  <ReferenceLine yAxisId="netBuy" y={0} stroke="hsl(var(--border))" />
                  <ChartTooltip
                    content={
                      <ChartTooltipContent
                        labelFormatter={(value) => `交易日 ${formatDate(String(value))}`}
                        formatter={(value, name) => (
                          <div className="flex flex-1 items-center justify-between gap-4">
                            <span className="text-muted-foreground">{name === "netFinancingBuy" ? "融资净买入" : "融资余额"}</span>
                            <span className="font-mono font-medium tabular-nums">
                              {name === "netFinancingBuy"
                                ? signedYuan(Number(value) * 100_000_000)
                                : formatYuanAsYi(Number(value) * 100_000_000, 2)}
                            </span>
                          </div>
                        )}
                      />
                    }
                  />
                  <Bar yAxisId="netBuy" dataKey="netFinancingBuy" fill="var(--color-netFinancingBuy)" radius={[2, 2, 0, 0]} maxBarSize={14} isAnimationActive={false} />
                  <Line yAxisId="balance" type="monotone" dataKey="financingBalance" stroke="var(--color-financingBalance)" strokeWidth={2.5} dot={false} connectNulls={false} isAnimationActive={false} />
                </ComposedChart>
              </ChartContainer>
            ) : <EmptyChart />}
          </ChartShell>
          {bare ? (
            <FinancingBalanceInsight points={history} framed={false} />
          ) : null}
        </div>
        {bare ? (
          <div className="mt-1 min-w-0 xl:mt-0">
            <OverviewMetric label="融资余额" value={formatYuanAsYi(current?.financingBalance, 2)} detail="未偿还融资借款" icon={WalletCards} compact />
            <OverviewMetric label="今日净买入" value={signedYuan(netFinancingBuy)} detail="融资买入 - 融资偿还" icon={Activity} tone={buyTone} compact />
            <OverviewMetric label="融资余额变化" value={signedPercent(change?.percent ?? null)} detail="较上一交易日" icon={LineChartIcon} tone={trendTone} compact />
            <OverviewMetric label="杠杆水平" value={leverage == null ? "-" : `${leverage.toFixed(1)}%`} detail="融资余额 / 两融余额" icon={BarChart3} compact />
            <OverviewMetric
              label="融资买入占成交额比例"
              value={financingBuyRatio == null ? "数据不足" : `${financingBuyRatio.toFixed(2)}%`}
              detail="融资买入 / 股票成交额"
              icon={BarChart3}
              tone={financingBuyRatio != null && financingBuyRatio >= 15 ? "down" : "neutral"}
              compact
            />
          </div>
        ) : null}
      </div>
      <div className="mt-2 flex items-center justify-end text-[11px] text-muted-foreground">单位：亿元</div>
    </section>
  )
}

function EmptyChart() {
  return <div className="flex h-52 items-center justify-center text-xs text-muted-foreground">历史样本不足，暂无法绘制趋势</div>
}
