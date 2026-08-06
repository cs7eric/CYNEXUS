import { ArrowDownRight, ArrowUpRight, ChartNoAxesCombined, CircleDollarSign, Minus, WalletCards } from "lucide-react"
import { Line, LineChart, YAxis } from "recharts"

import { ChartContainer, ChartTooltip, ChartTooltipContent, type ChartConfig } from "@/components/ui/chart"
import type { MarginMarketSummary } from "@/services/market/margin"
import { cn } from "@/lib/utils"
import { formatDate, formatYi } from "../lib/format"
import { changeFromDaysAgo, summaryByExchange, type MarginTrendPoint, type MarginTrendValueKey } from "../lib/derive"

interface SummaryCardsProps {
  summaries: MarginMarketSummary[]
  financingBuyChange: number | null
  trend: MarginTrendPoint[]
}

function MetricCard({
  label,
  value,
  sub,
  tone,
  icon: Icon,
  trend,
  trendKey,
  trendLabel,
  changes,
}: {
  label: string
  value: string
  sub: string
  tone: "blue" | "red" | "green" | "violet" | "slate"
  icon: typeof WalletCards
  trend: MarginTrendPoint[]
  trendKey: MarginTrendValueKey
  trendLabel: string
  changes?: Array<{ period: number; change: ReturnType<typeof changeFromDaysAgo> }>
}) {
  return (
    <div
      className={cn(
        "min-h-[126px] min-w-0 overflow-hidden rounded-lg bg-muted/45 p-4",
        tone === "blue" && "bg-sky-50/80 dark:bg-sky-950/20",
        tone === "red" && "bg-red-50/80 dark:bg-red-950/20",
        tone === "green" && "bg-emerald-50/80 dark:bg-emerald-950/20",
        tone === "violet" && "bg-violet-50/80 dark:bg-violet-950/20",
      )}
    >
      <div className="grid min-w-0 grid-cols-[minmax(0,1fr)_minmax(112px,1.2fr)] items-start gap-3">
        <div className="min-w-0">
          <div className="flex items-center justify-between gap-2">
            <span className="truncate text-xs font-medium text-muted-foreground">{label}</span>
            <Icon className="size-4 shrink-0 text-muted-foreground" />
          </div>
          <div className="mt-5 truncate text-2xl font-semibold tabular-nums tracking-tight">{value}</div>
          <div className="mt-2 truncate text-xs text-muted-foreground">{sub}</div>
        </div>
        <BalanceTrendChart trend={trend} trendKey={trendKey} label={trendLabel} />
      </div>
      {changes ? <BalanceChangeGrid changes={changes} /> : null}
    </div>
  )
}

function BalanceTrendChart({
  trend,
  trendKey,
  label,
}: {
  trend: MarginTrendPoint[]
  trendKey: MarginTrendValueKey
  label: string
}) {
  const rawData = trend
    .map((point) => ({ date: point.date, value: point[trendKey] }))
    .filter((point): point is { date: string; value: number } => point.value != null && Number.isFinite(point.value))
    .slice(-20)
  const baseline = rawData[0]?.value ?? 0
  const data = rawData.map((point) => ({
    ...point,
    plotValue: baseline === 0 ? point.value : ((point.value - baseline) / Math.abs(baseline)) * 100,
  }))
  const values = data.map((point) => point.plotValue)
  const minimum = values.length ? Math.min(...values) : 0
  const maximum = values.length ? Math.max(...values) : 0
  const range = Math.max(maximum - minimum, 0.01)
  const padding = Math.max(range * 0.14, 0.08)
  const config = { value: { label, color: "#0f8bb8" } } satisfies ChartConfig

  if (rawData.length < 2) {
    return <div className="flex h-10 items-center justify-center text-[10px] text-muted-foreground">暂无趋势</div>
  }

  return (
    <div className="min-w-0" role="img" aria-label={`${label}近20个交易日相对变化趋势`}>
      <ChartContainer config={config} className="h-10 w-full min-w-0">
        <LineChart data={data} margin={{ left: 2, right: 2, top: 3, bottom: 1 }}>
          <YAxis hide domain={[minimum - padding, maximum + padding]} />
          <ChartTooltip
            cursor={false}
            content={
              <ChartTooltipContent
                indicator="line"
                labelFormatter={(value) => formatDate(String(value))}
                formatter={(_, __, item) => (
                  <div className="grid gap-0.5">
                    <span className="font-mono font-medium tabular-nums">{formatYi(Number(item.payload?.value), 1)}</span>
                    <span className="font-mono text-[10px] text-muted-foreground tabular-nums">
                      相对起点 {Number(item.payload?.plotValue) >= 0 ? "+" : ""}{Number(item.payload?.plotValue).toFixed(2)}%
                    </span>
                  </div>
                )}
              />
            }
          />
          <Line
            type="monotone"
            dataKey="plotValue"
            name="value"
            stroke="var(--color-value)"
            strokeWidth={2}
            dot={false}
            isAnimationActive={false}
          />
        </LineChart>
      </ChartContainer>
    </div>
  )
}

function ChangeValue({ change }: { change: ReturnType<typeof changeFromDaysAgo> }) {
  if (!change) return <span className="text-muted-foreground">-</span>
  const positive = change.value > 0
  const tone = positive ? "text-red-600 dark:text-red-400" : change.value < 0 ? "text-emerald-600 dark:text-emerald-400" : "text-muted-foreground"
  return (
    <span className="min-w-0">
      <span className={cn("block truncate font-mono text-xs tabular-nums", tone)}>
        {change.value > 0 ? "+" : ""}{formatYi(change.value, 1)}
      </span>
      <span className={cn("block truncate font-mono text-[10px] tabular-nums", tone)}>
        {change.percent == null ? "环比 -" : `${change.percent > 0 ? "+" : ""}${change.percent.toFixed(1)}%`}
      </span>
    </span>
  )
}

function BalanceChangeGrid({ changes }: { changes: Array<{ period: number; change: ReturnType<typeof changeFromDaysAgo> }> }) {
  return (
    <div className="mt-2 grid grid-cols-3 gap-1.5 border-t border-border/50 pt-1.5">
      {changes.map(({ period, change }) => (
        <div key={period} className="min-w-0">
          <div className="mb-0.5 text-[10px] text-muted-foreground">近{period}日</div>
          <ChangeValue change={change} />
        </div>
      ))}
    </div>
  )
}

export function SummaryCards({ summaries, financingBuyChange, trend }: SummaryCardsProps) {
  const byExchange = summaryByExchange(summaries)
  const cn = byExchange.get("CN")
  const isUp = (financingBuyChange ?? 0) >= 0

  return (
    <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-4">
      <MetricCard
        label="全市场两融余额"
        value={formatYi(cn?.marginBalance, 1)}
        sub={`融资余额 ${formatYi(cn?.financingBalance, 1)}`}
        tone="blue"
        icon={WalletCards}
        trend={trend}
        trendKey="cn"
        trendLabel="全市场两融余额"
      />
      <MetricCard
        label="单日融资买入"
        value={formatYi(cn?.financingBuyAmount, 1)}
        sub={`较上一交易日 ${financingBuyChange == null ? "-" : `${isUp ? "+" : ""}${formatYi(financingBuyChange, 1)}`}`}
        tone={financingBuyChange == null ? "slate" : isUp ? "red" : "green"}
        icon={financingBuyChange == null ? Minus : isUp ? ArrowUpRight : ArrowDownRight}
        trend={trend}
        trendKey="buyCn"
        trendLabel="单日融资买入"
        changes={[5, 10, 20].map((period) => ({
          period,
          change: changeFromDaysAgo(trend, "buyCn", period),
        }))}
      />
      <MetricCard
        label="融资余额"
        value={formatYi(cn?.financingBalance, 1)}
        sub="市场未偿还的融资借款"
        tone="violet"
        icon={ChartNoAxesCombined}
        trend={trend}
        trendKey="financingBalanceCn"
        trendLabel="融资余额"
        changes={[5, 10, 20].map((period) => ({
          period,
          change: changeFromDaysAgo(trend, "financingBalanceCn", period),
        }))}
      />
      <MetricCard
        label="融券余额"
        value={formatYi(cn?.securitiesLendingBalanceAmount, 1)}
        sub="借入股票对应的市值"
        tone="slate"
        icon={CircleDollarSign}
        trend={trend}
        trendKey="securitiesLendingBalanceAmountCn"
        trendLabel="融券余额"
        changes={[5, 10, 20].map((period) => ({
          period,
          change: changeFromDaysAgo(trend, "securitiesLendingBalanceAmountCn", period),
        }))}
      />
    </div>
  )
}
