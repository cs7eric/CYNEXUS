import { ArrowDownRight, ArrowUpRight, Minus, ShieldAlert, TrendingUp } from "lucide-react"

import { cn } from "@/lib/utils"
import { formatYuanAsYi } from "../lib/format"

export interface MarginInsightPoint {
  tradeDate: string
  constituentCount?: number | null
  matchedStockCount?: number | null
  financingBalance: number | null
  marginBalance?: number | null
  financingBuyAmount: number | null
  financingRepayAmount: number | null
  securitiesLendingBalanceAmount: number | null
  turnoverAmount?: number | null
}

interface ChangeValue {
  value: number
  percent: number | null
}

function finite(value: number | null | undefined): value is number {
  return value != null && Number.isFinite(value)
}

function formatBalance(value: number | null | undefined) {
  if (!finite(value)) return "-"
  if (Math.abs(value) >= 1_000_000_000_000) {
    return `${(value / 1_000_000_000_000).toFixed(2)} 万亿`
  }
  return formatYuanAsYi(value, 2)
}

function signedBalance(value: number | null | undefined) {
  if (!finite(value)) return "-"
  return `${value >= 0 ? "+" : ""}${formatBalance(value)}`
}

function signedPercent(value: number | null | undefined) {
  if (!finite(value)) return "-"
  return `${value >= 0 ? "+" : ""}${value.toFixed(2)}%`
}

function changeAt(points: MarginInsightPoint[], daysAgo: number): ChangeValue | null {
  const values = points.filter((item) => finite(item.financingBalance))
  if (values.length <= daysAgo) return null
  const latest = values.at(-1)?.financingBalance
  const previous = values.at(-(daysAgo + 1))?.financingBalance
  if (!finite(latest) || !finite(previous)) return null
  return {
    value: latest - previous,
    percent: previous === 0 ? null : ((latest - previous) / Math.abs(previous)) * 100,
  }
}

function percentile(points: MarginInsightPoint[]) {
  const values = points.map((item) => item.financingBalance).filter(finite)
  const latest = values.at(-1)
  if (!finite(latest) || values.length < 2) return null
  return (values.filter((value) => value <= latest).length / values.length) * 100
}

function InsightMetric({ label, value, detail, tone = "neutral" }: {
  label: string
  value: React.ReactNode
  detail?: string
  tone?: "neutral" | "up" | "down"
}) {
  return (
    <div className="min-w-0 bg-background px-3 py-2.5">
      <div className="text-[11px] text-muted-foreground">{label}</div>
      <div className={cn(
        "mt-1 truncate text-sm font-semibold tabular-nums",
        tone === "up" && "text-red-600 dark:text-red-400",
        tone === "down" && "text-emerald-600 dark:text-emerald-400",
      )}>{value}</div>
      {detail ? <div className="mt-1 truncate text-[10px] text-muted-foreground">{detail}</div> : null}
    </div>
  )
}

function changeTone(change: ChangeValue | null) {
  return change == null ? "neutral" : change.value > 0 ? "up" : change.value < 0 ? "down" : "neutral"
}

function changeText(change: ChangeValue | null, includePercent = true) {
  if (!change) return "数据不足"
  return `${signedBalance(change.value)}${includePercent && change.percent != null ? ` (${signedPercent(change.percent)})` : ""}`
}

export function FinancingBalanceInsight({
  points,
  framed = true,
}: {
  points: MarginInsightPoint[]
  framed?: boolean
}) {
  const current = points.at(-1)?.financingBalance
  const day = changeAt(points, 1)
  const week = changeAt(points, 5)
  const month = changeAt(points, 20)
  const position = percentile(points)
  const direction = day?.value == null
    ? "历史样本不足，暂不判断杠杆资金方向。"
    : day.value > 0
      ? "融资余额上升，越来越多杠杆资金参与市场，风险偏好可能提升。"
      : day.value < 0
        ? "融资余额下降，杠杆资金正在撤退，需关注风险偏好快速收缩。"
        : "融资余额基本持平，杠杆资金暂未出现明显方向。"

  return (
    <section className={cn(framed ? "rounded-lg border border-border/70 bg-background p-3.5" : "p-4")}>
      <div className={cn(
        "grid grid-cols-2 gap-px overflow-hidden rounded-md border border-border/60 bg-border/60",
        "sm:grid-cols-5",
      )}>
        <InsightMetric label="当前融资余额" value={formatBalance(current)} />
        <InsightMetric label="日变化" value={changeText(day)} tone={changeTone(day)} />
        <InsightMetric label="周变化" value={changeText(week, false)} tone={changeTone(week)} detail="近 5 个交易日" />
        <InsightMetric label="月变化" value={changeText(month, false)} tone={changeTone(month)} detail="近 20 个交易日" />
        <InsightMetric label="历史位置" value={position == null ? "-" : `${position.toFixed(0)}%`} detail="融资余额历史分位" />
      </div>
      <div className="mt-3 flex items-start gap-2 rounded-md bg-muted/35 px-3 py-2 text-xs leading-5 text-muted-foreground">
        {day?.value == null ? <Minus className="mt-0.5 size-3.5 shrink-0" /> : day.value > 0 ? <ArrowUpRight className="mt-0.5 size-3.5 shrink-0 text-red-600" /> : <ArrowDownRight className="mt-0.5 size-3.5 shrink-0 text-emerald-600" />}
        <div>
          <div>{direction}</div>
          <div className="mt-1">占比越高，说明当日买盘中杠杆资金参与越深，上涨弹性和波动风险通常也更高；占比较低时，行情可能更多由机构或长线资金推动。</div>
        </div>
      </div>
    </section>
  )
}

export function FinancingBuyRatioInsight({ points, framed = true }: { points: MarginInsightPoint[]; framed?: boolean }) {
  const current = points.at(-1)
  const ratio = finite(current?.financingBuyAmount) && finite(current?.turnoverAmount) && current.turnoverAmount > 0
    ? (current.financingBuyAmount / current.turnoverAmount) * 100
    : null
  const tone = ratio != null && ratio >= 15 ? "down" : "neutral"

  return (
    <section className={cn(framed ? "rounded-lg border border-border/70 bg-background p-3.5" : "p-4")}>
      <div className="flex items-center gap-2">
        <h3 className="text-sm font-semibold">融资买入占成交额比例</h3>
        <TrendingUp className="size-4 text-muted-foreground" />
      </div>
      <div className="mt-3 grid grid-cols-2 gap-px overflow-hidden rounded-md border border-border/60 bg-border/60">
        <InsightMetric label="融资买入占成交额比例" value={ratio == null ? "暂无成交额数据" : `${ratio.toFixed(2)}%`} tone={tone} />
        <InsightMetric label="计算口径" value={ratio == null ? "数据不足" : "融资买入 / 股票成交额"} detail={ratio == null ? "需要对应交易日成交额" : undefined} />
      </div>
    </section>
  )
}

export function FinancingExposureInsight({ financingBalance, circulatingMarketCap, historicalRatios = [] }: {
  financingBalance: number | null | undefined
  circulatingMarketCap: number | null | undefined
  historicalRatios?: number[]
}) {
  const ratio = finite(financingBalance) && finite(circulatingMarketCap) && circulatingMarketCap > 0
    ? (financingBalance / circulatingMarketCap) * 100
    : null
  const ratios = historicalRatios.filter((value) => Number.isFinite(value))
  const historicalHigh = ratios.length ? Math.max(...ratios) : null
  const historicalLow = ratios.length ? Math.min(...ratios) : null

  return (
    <section className="rounded-lg border border-border/70 bg-background p-3.5">
      <div className="flex items-center gap-2">
        <h3 className="text-sm font-semibold">两融余额占流通市值比例</h3>
        <ShieldAlert className="size-4 text-muted-foreground" />
      </div>
      <div className="mt-3 grid grid-cols-2 gap-px overflow-hidden rounded-md border border-border/60 bg-border/60 sm:grid-cols-4">
        <InsightMetric label="当前" value={ratio == null ? "暂无流通市值" : `${ratio.toFixed(2)}%`} tone={ratio != null && ratio >= 3 ? "down" : "neutral"} />
        <InsightMetric label="历史最高" value={historicalHigh == null ? "暂无序列" : `${historicalHigh.toFixed(2)}%`} />
        <InsightMetric label="历史最低" value={historicalLow == null ? "暂无序列" : `${historicalLow.toFixed(2)}%`} />
        <InsightMetric label="风险含义" value={ratio != null && ratio >= 3 ? "杠杆偏高" : ratio == null ? "数据不足" : "杠杆温和"} />
      </div>
      <p className="mt-3 text-xs leading-5 text-muted-foreground">
        计算公式：融资余额 / 流通市值。比例较高意味着市场杠杆较高，牛市后期和快速下跌时的平仓压力更大；比例较低通常代表杠杆资金较少、市场可能偏冷。
        {ratios.length ? " 历史极值按当前可用流通市值口径回溯。" : " 当前数据暂无完整历史流通市值序列。"}
      </p>
    </section>
  )
}

function factorScore(value: number | null, scale: number, inverse = false) {
  if (value == null || !Number.isFinite(value)) return null
  const normalized = Math.max(-1, Math.min(1, value / scale))
  return Math.max(0, Math.min(100, 50 + (inverse ? -normalized : normalized) * 50))
}

export function MarginSentimentSummary({ points }: { points: MarginInsightPoint[] }) {
  const current = points.at(-1)
  const trendChange = changeAt(points, 5)
  const balanceChange = changeAt(points, 20)
  const previous = points.at(-2)
  const netBuy = finite(current?.financingBuyAmount) && finite(current?.financingRepayAmount)
    ? current.financingBuyAmount - current.financingRepayAmount
    : null
  const netBuyRatio = finite(current?.financingBalance) && current.financingBalance > 0 && finite(netBuy)
    ? (netBuy / current.financingBalance) * 100
    : null
  const lendingChange = finite(current?.securitiesLendingBalanceAmount) && finite(previous?.securitiesLendingBalanceAmount)
    ? ((current.securitiesLendingBalanceAmount - previous.securitiesLendingBalanceAmount) / Math.max(Math.abs(previous.securitiesLendingBalanceAmount), 1)) * 100
    : null
  const buyRatio = finite(current?.financingBuyAmount) && finite(current?.turnoverAmount) && current.turnoverAmount > 0
    ? (current.financingBuyAmount / current.turnoverAmount) * 100
    : null
  const factors = [
    { label: "融资余额趋势", weight: 30, score: factorScore(trendChange?.percent ?? null, 5) },
    { label: "融资净买入", weight: 30, score: factorScore(netBuyRatio, 5) },
    { label: "融资余额增速", weight: 20, score: factorScore(balanceChange?.percent ?? null, 10) },
    { label: "融券变化", weight: 10, score: factorScore(lendingChange, 10, true) },
    { label: "融资占成交比例", weight: 10, score: factorScore(buyRatio, 20) },
  ]
  const available = factors.filter((factor) => factor.score != null)
  const score = available.length
    ? available.reduce((sum, factor) => sum + (factor.score ?? 0) * factor.weight, 0) / available.reduce((sum, factor) => sum + factor.weight, 0)
    : null
  const label = score == null ? "数据不足" : score >= 65 ? "杠杆资金偏积极" : score <= 35 ? "杠杆资金偏谨慎" : "杠杆资金中性"

  return (
    <section className="shrink-0 text-right">
      <div className={cn(
        "font-mono tabular-nums",
        score != null && score >= 65 && "text-red-600 dark:text-red-400",
        score != null && score <= 35 && "text-emerald-600 dark:text-emerald-400",
      )}>
        <div className="text-2xl font-bold leading-none">{score == null ? "-" : `${Math.round(score)} / 100`}</div>
        <div className="mt-1.5 text-xs font-medium">{label}</div>
      </div>
    </section>
  )
}
