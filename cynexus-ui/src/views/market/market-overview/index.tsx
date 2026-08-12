import { useCallback, useEffect, useMemo, useState } from "react"
import type { ReactNode } from "react"
import {
  BarChart3,
  CheckCircle2,
  CircleAlert,
  Clock3,
  ExternalLink,
  Gauge,
  RefreshCw,
  ShieldCheck,
  Target,
  TrendingUp,
  Waves,
} from "lucide-react"
import { Link } from "react-router-dom"

import { Button } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import { WorkspaceShell } from "@/layout/workspace-shell"
import {
  fetchMarketOverviewAkshare,
  fetchMarketOverviewEltdx,
  fetchMarketPulseHistory,
  fetchMarketPulseIndexReturns,
  fetchStyleSectors,
  type IndexReturnItem,
  type MarketHistoryPoint,
  type MarketOverview,
  type MarketOverviewEltdx,
  type StyleSectorItem,
} from "@/services/market/pulse"
import {
  fetchMarketIndicators,
  type MarketIndicatorResponse,
} from "@/services/market/indicator"
import {
  fetchMarketSentimentIndex,
  fetchMarketSentimentIndexHistory,
  fetchMarketOverviewHistory,
  type MarketSentimentIndexHistoryItem,
  type MarketSentimentIndexResponse,
  type MarketOverviewHistoryItem,
} from "@/services/market/sentiment"
import {
  fetchMarginLatest,
  fetchMarginSummaryHistory,
  type MarginLatestResponse,
  type MarginMarketSummary,
} from "@/services/market/margin"
import { fetchMarketEtf, type MarketEtfResponse } from "@/services/market/etf"
import {
  fetchFuturesPositionLatest,
  type FuturesPositionLatestResponse,
} from "@/services/market/futures-position"
import type { LucideIcon } from "lucide-react"

type HistoryRow = {
  date: string
  totalAmount: number | null
  risingCount: number | null
  fallingCount: number | null
  limitUpCount: number | null
  limitDownCount: number | null
  mainNetInflow: number | null
}

type OverviewState = {
  pulse: MarketOverviewEltdx | null
  flow: MarketOverview | null
  indices: IndexReturnItem[]
  sectors: StyleSectorItem[]
  sentiment: MarketSentimentIndexResponse | null
  sentimentHistory: MarketSentimentIndexHistoryItem[]
  margin: MarginLatestResponse | null
  marginHistory: MarginMarketSummary[]
  etf: MarketEtfResponse | null
  futures: FuturesPositionLatestResponse | null
  indicator: MarketIndicatorResponse | null
  history: HistoryRow[]
  errors: string[]
}

const EMPTY_STATE: OverviewState = {
  pulse: null,
  flow: null,
  indices: [],
  sectors: [],
  sentiment: null,
  sentimentHistory: [],
  margin: null,
  marginHistory: [],
  etf: null,
  futures: null,
  indicator: null,
  history: [],
  errors: [],
}

function localDate(offset = 0) {
  const date = new Date()
  date.setDate(date.getDate() + offset)
  return date.toISOString().slice(0, 10)
}

function formatNumber(value: number | null | undefined, digits = 1) {
  if (value == null || Number.isNaN(value)) return "—"
  return value.toLocaleString("zh-CN", { maximumFractionDigits: digits })
}

function formatSigned(value: number | null | undefined, digits = 1) {
  if (value == null || Number.isNaN(value)) return "—"
  return `${value > 0 ? "+" : ""}${value.toFixed(digits)}`
}

function formatAmount(value: number | null | undefined) {
  if (value == null || Number.isNaN(value)) return "—"
  const absolute = Math.abs(value)
  if (absolute >= 10000) return `${formatNumber(value / 10000)} 万亿`
  return `${formatNumber(value)} 亿`
}

function formatTime(value?: string | null) {
  if (!value) return "—"
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? value : date.toLocaleTimeString("zh-CN", { hour: "2-digit", minute: "2-digit" })
}

function tone(value: number | null | undefined) {
  if (value == null || value === 0) return "text-muted-foreground"
  return value > 0 ? "text-rose-600" : "text-emerald-600"
}

function bgTone(value: number | null | undefined) {
  if (value == null || value === 0) return "bg-muted-foreground/30"
  return value > 0 ? "bg-rose-500" : "bg-emerald-500"
}

function mean(values: Array<number | null | undefined>) {
  const valid = values.filter((value): value is number => value != null && Number.isFinite(value))
  return valid.length ? valid.reduce((sum, value) => sum + value, 0) / valid.length : null
}

function delta(current: number | null, previous: number | null) {
  return current == null || previous == null ? null : current - previous
}

function toHistoryRow(item: MarketOverviewHistoryItem | MarketHistoryPoint): HistoryRow {
  return {
    date: "tradeDate" in item ? item.tradeDate : item.date,
    totalAmount: item.totalAmount,
    risingCount: item.risingCount,
    fallingCount: item.fallingCount,
    limitUpCount: item.limitUpCount,
    limitDownCount: item.limitDownCount,
    mainNetInflow: item.mainNetInflow,
  }
}

function statusLabel(value: number | null, fallback = "待确认") {
  if (value == null) return fallback
  if (value > 1) return "扩散"
  if (value > 0.2) return "改善"
  if (value < -1) return "收缩"
  if (value < -0.2) return "走弱"
  return "中性"
}

function StatusPill({ label, value }: { label: string; value: number | null }) {
  return <span className={`inline-flex items-center rounded-full border border-border/70 px-2 py-0.5 text-[11px] font-medium ${tone(value)}`}>{label}</span>
}

function SectionTitle({ icon: Icon, title, detail, action }: { icon: LucideIcon; title: string; detail?: string; action?: ReactNode }) {
  return (
    <div className="flex flex-wrap items-start justify-between gap-3 border-b border-border/60 px-4 py-3">
      <div className="flex min-w-0 items-start gap-2.5">
        <Icon className="mt-0.5 size-4 shrink-0 text-information" />
        <div className="min-w-0">
          <h2 className="text-sm font-semibold text-foreground">{title}</h2>
          {detail ? <p className="mt-1 text-xs leading-5 text-muted-foreground">{detail}</p> : null}
        </div>
      </div>
      {action}
    </div>
  )
}

function Metric({ label, value, detail, valueClass }: { label: string; value: ReactNode; detail?: string; valueClass?: string }) {
  return (
    <div className="min-w-0 border-l border-border/60 pl-3 first:border-l-0 first:pl-0">
      <div className="text-[11px] text-muted-foreground">{label}</div>
      <div className={`mt-1 truncate font-mono text-lg font-semibold tabular-nums ${valueClass ?? "text-foreground"}`}>{value}</div>
      {detail ? <div className="mt-1 truncate text-[11px] text-muted-foreground">{detail}</div> : null}
    </div>
  )
}

function Verdict({ state, stats, loading }: { state: OverviewState; stats: Stats; loading: boolean }) {
  const score = state.sentiment?.compositeScore ?? null
  const breadth = stats.latestBreadth
  const flow = stats.latest?.mainNetInflow ?? state.flow?.mainNetInflow ?? null
  const verdict = score == null || breadth == null ? "等待更多确认" : score >= 60 && breadth >= 0.52 ? "风险偏好正在扩散" : score <= 40 && breadth < 0.45 ? "防守优先，等待止跌" : "结构性行情，重在分化"
  const verdictDetail = score == null ? "情绪、宽度与资金历史尚未同时就绪" : `情绪 ${formatNumber(score, 0)} 分 · 近 5 日宽度 ${formatSigned(stats.breadthDeltaPct, 1)} 个百分点 · 主力 ${formatAmount(flow)}`

  return (
    <section className="overflow-hidden rounded-lg border border-border/70 bg-card">
      <SectionTitle icon={ShieldCheck} title="今日市场结论" detail="把分散的行情数据压缩成一个可执行判断。" action={<span className="text-[11px] text-muted-foreground">{state.flow?.tradingDate ?? state.pulse?.tradingDate ?? "—"}</span>} />
      <div className="grid gap-6 p-4 lg:grid-cols-[minmax(280px,1fr)_minmax(0,1.6fr)] lg:items-center">
        <div>
          <div className="flex items-center gap-2 text-xs text-muted-foreground"><span className={`size-2 rounded-full ${score != null ? "bg-information" : "bg-muted-foreground/40"}`} />综合状态</div>
          <h3 className="mt-2 text-2xl font-semibold tracking-tight text-foreground">{loading && score == null ? "加载中" : verdict}</h3>
          <p className="mt-2 max-w-lg text-xs leading-5 text-muted-foreground">{verdictDetail}。继续关注右侧的跨模块信号，确认是趋势扩散还是局部脉冲。</p>
          <div className="mt-4 flex flex-wrap gap-2"><StatusPill label={statusLabel(stats.breadthDeltaPct, "宽度待确认")} value={stats.breadthDeltaPct} /><StatusPill label={statusLabel(stats.flowDelta, "资金待确认")} value={stats.flowDelta} /><StatusPill label={state.futures?.aggregate?.longShortRatio != null ? "衍生品有定位" : "衍生品待确认"} value={state.futures?.aggregate?.netPosition ?? null} /></div>
        </div>
        <div className="grid grid-cols-2 gap-x-5 gap-y-5 sm:grid-cols-4">
          <Metric label="情绪指数" value={score == null ? "—" : `${formatNumber(score, 0)} / 100`} detail={state.sentiment?.level ?? "等待评分"} valueClass={score == null ? undefined : tone(score - 50)} />
          <Metric label="上涨 / 下跌" value={<><span className="text-rose-600">{stats.latest?.risingCount ?? state.pulse?.risingCount ?? "—"}</span><span className="mx-1 text-muted-foreground">/</span><span className="text-emerald-600">{stats.latest?.fallingCount ?? state.pulse?.fallingCount ?? "—"}</span></>} detail={`近 5 日宽度 ${formatSigned(stats.breadthDeltaPct)}%`} />
          <Metric label="成交额" value={formatAmount(stats.latest?.totalAmount ?? state.pulse?.totalAmount ?? state.flow?.totalAmount)} detail={`相对 5D ${formatSigned(stats.amountVs5dPct)}%`} valueClass={tone(stats.amountVs5dPct)} />
          <Metric label="主力净流入" value={formatAmount(flow)} detail={`5D 累计 ${formatAmount(stats.flow5d)}`} valueClass={tone(flow)} />
        </div>
      </div>
    </section>
  )
}

type Stats = {
  latest: HistoryRow | null
  previous: HistoryRow | null
  five: HistoryRow[]
  twenty: HistoryRow[]
  latestBreadth: number | null
  breadthDeltaPct: number | null
  amountVs5dPct: number | null
  flow5d: number | null
  flowDelta: number | null
}

function calculateStats(history: HistoryRow[]): Stats {
  const sorted = [...history].sort((a, b) => a.date.localeCompare(b.date))
  const latest = sorted.at(-1) ?? null
  const previous = sorted.at(-2) ?? null
  const five = sorted.slice(-5)
  const twenty = sorted.slice(-20)
  const breadth = (row: HistoryRow | null) => row && row.risingCount != null && row.fallingCount != null ? row.risingCount / Math.max(1, row.risingCount + row.fallingCount) : null
  const latestBreadth = breadth(latest)
  const fiveBreadth = mean(five.map(breadth))
  const amountMean = mean(five.slice(0, -1).map((item) => item.totalAmount))
  return {
    latest,
    previous,
    five,
    twenty,
    latestBreadth,
    breadthDeltaPct: latestBreadth == null || fiveBreadth == null ? null : (latestBreadth - fiveBreadth) * 100,
    amountVs5dPct: latest?.totalAmount == null || amountMean == null || amountMean === 0 ? null : ((latest.totalAmount - amountMean) / amountMean) * 100,
    flow5d: five.length ? five.reduce((sum, item) => sum + (item.mainNetInflow ?? 0), 0) : null,
    flowDelta: latest?.mainNetInflow == null || previous?.mainNetInflow == null ? null : latest.mainNetInflow - previous.mainNetInflow,
  }
}

type Signal = { name: string; value: string; baseline: string; note: string; status: string; score: number | null; href: string }

function SignalMatrix({ state, stats }: { state: OverviewState; stats: Stats }) {
  const margin = state.margin?.summaries?.reduce((sum, item) => sum + (item.marginBalance ?? 0), 0) || null
  const marginHistory = state.marginHistory.map((item) => item.marginBalance ?? 0)
  const marginDelta = margin != null && marginHistory.length > 1 ? margin - (marginHistory.at(-2) ?? margin) : null
  const futures = state.futures?.aggregate
  const etfShare = state.etf?.summary?.broadShareDelta ?? null
  const topSector = [...state.sectors].sort((a, b) => (b.change_pct ?? -999) - (a.change_pct ?? -999))[0]
  const breadthScore = stats.breadthDeltaPct
  const signals: Signal[] = [
    { name: "价格", value: state.indices[0]?.returnPct == null ? "—" : `${formatSigned(state.indices[0].returnPct)}%`, baseline: "宽基 5D 收益", note: state.indices[0]?.name ?? "指数数据", status: statusLabel(state.indices[0]?.returnPct ?? null), score: state.indices[0]?.returnPct ?? null, href: "/market/pulse" },
    { name: "宽度", value: stats.latestBreadth == null ? "—" : `${formatNumber(stats.latestBreadth * 100, 1)}%`, baseline: `5D ${formatSigned(stats.breadthDeltaPct)}pp`, note: `${stats.latest?.risingCount ?? "—"} 家上涨 / ${stats.latest?.fallingCount ?? "—"} 家下跌`, status: statusLabel(breadthScore), score: breadthScore, href: "/data-collector/market-indicator" },
    { name: "成交", value: formatAmount(stats.latest?.totalAmount), baseline: `相对 5D ${formatSigned(stats.amountVs5dPct)}%`, note: "成交额是否放大", status: statusLabel(stats.amountVs5dPct), score: stats.amountVs5dPct, href: "/market/pulse" },
    { name: "资金", value: formatAmount(stats.latest?.mainNetInflow ?? state.flow?.mainNetInflow), baseline: `5D ${formatAmount(stats.flow5d)}`, note: "主力净流入", status: statusLabel(stats.flowDelta), score: stats.flowDelta, href: "/market/pulse" },
    { name: "杠杆", value: formatAmount(margin), baseline: `日变动 ${formatAmount(marginDelta)}`, note: "融资融券余额", status: statusLabel(marginDelta), score: marginDelta, href: "/market/margin-trading" },
    { name: "衍生品", value: futures?.longShortRatio == null ? "—" : futures.longShortRatio.toFixed(2), baseline: `净持仓 ${formatNumber(futures?.netPosition, 0)}`, note: futures?.longShortRatio == null ? "没有可用定位" : "多空比", status: futures?.longShortRatio == null ? "待确认" : futures.longShortRatio >= 1 ? "偏多" : "偏空", score: futures?.netPosition ?? null, href: "/market/futures-position" },
  ]
  return (
    <section className="overflow-hidden rounded-lg border border-border/70 bg-card">
      <SectionTitle icon={Gauge} title="六维信号矩阵" detail="每个维度只保留当前读数、近期基线和趋势状态，避免把详情页原样搬过来。" />
      <div className="divide-y divide-border/60">
        {signals.map((signal) => (
          <Link key={signal.name} to={signal.href} className="grid gap-3 px-4 py-3.5 transition-colors hover:bg-muted/30 sm:grid-cols-[90px_minmax(120px,0.8fr)_minmax(120px,0.9fr)_minmax(160px,1.2fr)_80px] sm:items-center">
            <div className="flex items-center gap-2 text-sm font-medium text-foreground"><span className={`size-2 rounded-full ${bgTone(signal.score)}`} />{signal.name}</div>
            <div className={`font-mono text-base font-semibold tabular-nums ${tone(signal.score)}`}>{signal.value}</div>
            <div className="font-mono text-xs tabular-nums text-muted-foreground">{signal.baseline}</div>
            <div className="min-w-0 truncate text-xs text-muted-foreground">{signal.note}{signal.name === "价格" && topSector ? ` · 领涨 ${topSector.name}` : ""}{signal.name === "资金" && etfShare != null ? ` · ETF 宽基份额 ${formatSigned(etfShare)}%` : ""}</div>
            <div className="text-left sm:text-right"><StatusPill label={signal.status} value={signal.score} /></div>
          </Link>
        ))}
      </div>
    </section>
  )
}

function PulseHistory({ stats }: { stats: Stats }) {
  const values = stats.five.map((item) => item.risingCount != null && item.fallingCount != null ? item.risingCount - item.fallingCount : null)
  const max = Math.max(...values.map((value) => Math.abs(value ?? 0)), 1)
  return (
    <section className="overflow-hidden rounded-lg border border-border/70 bg-card">
      <SectionTitle icon={TrendingUp} title="近 5 日市场脉冲" detail="看节奏是否延续：宽度、成交与资金是否在同一方向上共振。" />
      <div className="grid gap-4 p-4 lg:grid-cols-[minmax(0,1.2fr)_minmax(260px,0.8fr)]">
        <div className="grid grid-cols-5 gap-2 sm:gap-4">
          {stats.five.length ? stats.five.map((item, index) => {
            const breadth = values[index]
            const height = Math.max(10, Math.round((Math.abs(breadth ?? 0) / max) * 100))
            return <div key={item.date} className="min-w-0"><div className="flex h-32 items-end justify-center border-b border-border/60"><div className={`w-full max-w-12 rounded-t-sm ${bgTone(breadth)}`} style={{ height: `${height}%` }} /></div><div className="mt-2 truncate text-center text-[11px] text-muted-foreground">{item.date.slice(5)}</div><div className={`mt-1 text-center font-mono text-xs tabular-nums ${tone(breadth)}`}>{formatSigned(breadth, 0)}</div></div>
          }) : <div className="col-span-5 flex h-36 items-center justify-center text-xs text-muted-foreground">暂无近 5 日历史</div>}
        </div>
        <div className="grid grid-cols-2 gap-3 border-t border-border/60 pt-4 lg:border-l lg:border-t-0 lg:pl-4 lg:pt-0">
          <Metric label="5D 平均成交" value={formatAmount(mean(stats.five.map((item) => item.totalAmount)))} detail="亿元" />
          <Metric label="5D 累计资金" value={formatAmount(stats.flow5d)} detail="主力净流入" valueClass={tone(stats.flow5d)} />
          <Metric label="涨停变化" value={formatSigned(delta(stats.latest?.limitUpCount ?? null, stats.previous?.limitUpCount ?? null), 0)} detail="相比上一交易日" valueClass={tone(delta(stats.latest?.limitUpCount ?? null, stats.previous?.limitUpCount ?? null))} />
          <Metric label="跌停变化" value={formatSigned(delta(stats.latest?.limitDownCount ?? null, stats.previous?.limitDownCount ?? null), 0)} detail="相比上一交易日" valueClass={tone(-1 * (delta(stats.latest?.limitDownCount ?? null, stats.previous?.limitDownCount ?? null) ?? 0))} />
        </div>
      </div>
    </section>
  )
}

function WindowCompare({ stats, state }: { stats: Stats; state: OverviewState }) {
  const rows = [
    { label: "宽度（上涨占比）", today: stats.latestBreadth == null ? null : stats.latestBreadth * 100, five: mean(stats.five.map((item) => item.risingCount != null && item.fallingCount != null ? (item.risingCount / Math.max(1, item.risingCount + item.fallingCount)) * 100 : null)), twenty: mean(stats.twenty.map((item) => item.risingCount != null && item.fallingCount != null ? (item.risingCount / Math.max(1, item.risingCount + item.fallingCount)) * 100 : null)), suffix: "%" },
    { label: "成交额", today: stats.latest?.totalAmount ?? null, five: mean(stats.five.map((item) => item.totalAmount)), twenty: mean(stats.twenty.map((item) => item.totalAmount)), suffix: "亿" },
    { label: "主力净流入", today: stats.latest?.mainNetInflow ?? null, five: mean(stats.five.map((item) => item.mainNetInflow)), twenty: mean(stats.twenty.map((item) => item.mainNetInflow)), suffix: "亿" },
    { label: "情绪指数", today: state.sentiment?.compositeScore ?? null, five: mean(state.sentimentHistory.slice(-5).map((item) => item.compositeScore)), twenty: mean(state.sentimentHistory.map((item) => item.compositeScore)), suffix: "分" },
  ]
  return (
    <section className="overflow-hidden rounded-lg border border-border/70 bg-card">
      <SectionTitle icon={BarChart3} title="今日 / 5D 基线 / 20D 背景" detail="相比昨天只能看出变化，三段窗口才能判断变化是否异常、是否持续。" />
      <div className="overflow-x-auto"><table className="w-full min-w-[620px] text-left text-xs"><thead className="bg-muted/25 text-[11px] text-muted-foreground"><tr><th className="px-4 py-3 font-medium">指标</th><th className="px-4 py-3 text-right font-medium">今日</th><th className="px-4 py-3 text-right font-medium">5D 均值</th><th className="px-4 py-3 text-right font-medium">20D 均值</th><th className="px-4 py-3 text-right font-medium">偏离 20D</th></tr></thead><tbody className="divide-y divide-border/60">{rows.map((row) => { const deviation = row.today == null || row.twenty == null ? null : row.today - row.twenty; return <tr key={row.label}><td className="px-4 py-3 font-medium text-foreground">{row.label}</td><td className={`px-4 py-3 text-right font-mono tabular-nums ${tone(deviation)}`}>{formatNumber(row.today)}{row.today == null ? "" : row.suffix}</td><td className="px-4 py-3 text-right font-mono tabular-nums text-muted-foreground">{formatNumber(row.five)}{row.five == null ? "" : row.suffix}</td><td className="px-4 py-3 text-right font-mono tabular-nums text-muted-foreground">{formatNumber(row.twenty)}{row.twenty == null ? "" : row.suffix}</td><td className={`px-4 py-3 text-right font-mono tabular-nums ${tone(deviation)}`}>{formatSigned(deviation)}{deviation == null ? "" : row.suffix}</td></tr> })}</tbody></table></div>
    </section>
  )
}

function Consensus({ state, stats }: { state: OverviewState; stats: Stats }) {
  const sectors = [...state.sectors].filter((item) => item.change_pct != null).sort((a, b) => (b.change_pct ?? 0) - (a.change_pct ?? 0))
  const events = state.indicator?.events?.slice(0, 4) ?? []
  const consensus = [
    stats.breadthDeltaPct != null && stats.breadthDeltaPct > 0 ? "上涨家数占比高于 5D 基线" : "上涨家数尚未形成扩散",
    stats.flow5d != null && stats.flow5d > 0 ? "主力资金近 5 日累计为正" : "主力资金仍偏谨慎",
    sectors[0] ? `${sectors[0].name} 领涨 ${formatSigned(sectors[0].change_pct)}%` : "板块强弱待确认",
  ]
  const disagreement = [
    stats.amountVs5dPct != null && stats.amountVs5dPct < 0 ? "成交额低于 5D 基线，价格上涨缺少量能确认" : "量能未出现明显背离",
    state.futures?.aggregate?.longShortRatio != null && state.sentiment?.compositeScore != null && ((state.futures.aggregate.longShortRatio > 1 && state.sentiment.compositeScore < 45) || (state.futures.aggregate.longShortRatio < 1 && state.sentiment.compositeScore > 60)) ? "衍生品定位与情绪方向不一致" : "衍生品与情绪暂未出现明显冲突",
    events.length ? `${events.length} 条异动事件需要观察是否扩散` : "异动事件较少，趋势确认不足",
  ]
  return <section className="grid gap-4 lg:grid-cols-2"><div className="overflow-hidden rounded-lg border border-border/70 bg-card"><SectionTitle icon={CheckCircle2} title="共识" detail="至少两个独立维度同向，才算有可跟踪的市场信号。" /><div className="space-y-3 p-4">{consensus.map((item) => <div key={item} className="flex gap-2 text-xs leading-5 text-foreground"><CheckCircle2 className="mt-0.5 size-3.5 shrink-0 text-emerald-600" />{item}</div>)}</div></div><div className="overflow-hidden rounded-lg border border-border/70 bg-card"><SectionTitle icon={CircleAlert} title="分歧" detail="分歧不是噪音，是下一交易日最值得验证的地方。" /><div className="space-y-3 p-4">{disagreement.map((item) => <div key={item} className="flex gap-2 text-xs leading-5 text-foreground"><CircleAlert className="mt-0.5 size-3.5 shrink-0 text-amber-600" />{item}</div>)}</div></div></section>
}

function Reliability({ state }: { state: OverviewState }) {
  const checks = [
    { label: "行情快照", ready: Boolean(state.pulse || state.flow), detail: state.flow?.fetchedAt ?? state.pulse?.fetchedAt },
    { label: "历史窗口", ready: state.history.length >= 5, detail: `${state.history.length} 个交易日` },
    { label: "情绪因子", ready: Boolean(state.sentiment && state.sentiment.componentCount >= 6), detail: state.sentiment ? `${state.sentiment.componentCount}/9 因子` : "—" },
    { label: "跨市场数据", ready: Boolean(state.margin || state.etf || state.futures), detail: [state.margin && "杠杆", state.etf && "ETF", state.futures && "期指"].filter(Boolean).join(" / ") || "—" },
  ]
  return <section className="overflow-hidden rounded-lg border border-border/70 bg-muted/15"><SectionTitle icon={ShieldCheck} title="数据可靠性" detail="Overview 的结论只在基础数据足够完整时成立。" /><div className="grid divide-y divide-border/60 sm:grid-cols-4 sm:divide-x sm:divide-y-0">{checks.map((check) => <div key={check.label} className="flex items-center gap-3 px-4 py-3"><span className={`flex size-7 shrink-0 items-center justify-center rounded-full ${check.ready ? "bg-emerald-500/10 text-emerald-600" : "bg-amber-500/10 text-amber-600"}`}>{check.ready ? <CheckCircle2 className="size-4" /> : <CircleAlert className="size-4" />}</span><div className="min-w-0"><div className="text-xs font-medium text-foreground">{check.label}</div><div className="mt-1 truncate text-[11px] text-muted-foreground">{check.detail || "待更新"}</div></div></div>)}</div></section>
}

function NextSession({ stats }: { stats: Stats }) {
  const points = [
    { title: "宽度是否继续扩散", detail: `上涨占比需要站稳 ${formatNumber((stats.latestBreadth ?? 0) * 100, 1)}% 上方`, href: "/data-collector/market-indicator" },
    { title: "量能是否补上", detail: `成交额回到 5D 基线（${formatAmount(mean(stats.five.map((item) => item.totalAmount)))}）才算确认`, href: "/market/pulse" },
    { title: "资金与杠杆是否同向", detail: "主力净流入和融资余额至少一项继续改善", href: "/market/margin-trading" },
  ]
  return <section className="overflow-hidden rounded-lg border border-border/70 bg-card"><SectionTitle icon={Target} title="下一交易日验证点" detail="把今天的判断转成明天可以复核的条件。" /><div className="grid divide-y divide-border/60 sm:grid-cols-3 sm:divide-x sm:divide-y-0">{points.map((point, index) => <Link key={point.title} to={point.href} className="group p-4 transition-colors hover:bg-muted/30"><div className="flex items-center justify-between"><span className="font-mono text-[11px] text-muted-foreground">0{index + 1}</span><ExternalLink className="size-3.5 text-muted-foreground transition-transform group-hover:translate-x-0.5" /></div><div className="mt-4 text-sm font-medium text-foreground">{point.title}</div><p className="mt-2 text-xs leading-5 text-muted-foreground">{point.detail}</p></Link>)}</div></section>
}

function IndexStrip({ items }: { items: IndexReturnItem[] }) {
  return <div className="flex min-w-0 flex-wrap items-center gap-x-5 gap-y-2 border-b border-border/60 pb-3 text-xs"><span className="inline-flex items-center gap-1.5 font-medium text-foreground"><BarChart3 className="size-3.5 text-information" />宽基 5D</span>{items.slice(0, 5).map((item) => <Link key={item.code} to="/market/pulse" className="inline-flex items-center gap-1.5 text-muted-foreground hover:text-foreground"><span className="max-w-24 truncate">{item.name}</span><span className={`font-mono font-semibold tabular-nums ${tone(item.returnPct)}`}>{formatSigned(item.returnPct)}%</span></Link>)}</div>
}

export default function MarketOverviewPage() {
  const [state, setState] = useState<OverviewState>(EMPTY_STATE)
  const [loading, setLoading] = useState(true)
  const [refreshing, setRefreshing] = useState(false)
  const stats = useMemo(() => calculateStats(state.history), [state.history])

  const load = useCallback(async (manual = false) => {
    if (manual) setRefreshing(true)
    else setLoading(true)
    const end = localDate()
    const start = localDate(-35)
    const results = await Promise.allSettled([
      fetchMarketOverviewEltdx(),
      fetchMarketOverviewAkshare(),
      fetchMarketPulseIndexReturns(5),
      fetchStyleSectors(),
      fetchMarketSentimentIndex(),
      fetchMarketSentimentIndexHistory(start, end),
      fetchMarginLatest({ top: 5 }),
      fetchMarginSummaryHistory({ start, end }),
      fetchMarketEtf({ historyDays: 5, rowLimit: 30 }),
      fetchFuturesPositionLatest({ scopeType: "VARIETY", top: 5, sort: "netPosition", sortDir: "desc" }),
      fetchMarketIndicators({ status: "all", limit: 250 }),
      fetchMarketOverviewHistory({ start, end }),
      fetchMarketPulseHistory("20d"),
    ])
    const errors: string[] = []
    const value = <T,>(index: number): T | null => {
      const result = results[index]
      if (result.status === "fulfilled") return result.value as T
      errors.push(result.reason instanceof Error ? result.reason.message : String(result.reason))
      return null
    }
    const overviewHistory = value<{ items?: MarketOverviewHistoryItem[] }>(11)
    const pulseHistory = value<{ items?: MarketHistoryPoint[] }>(12)
    const history = (overviewHistory?.items?.length ? overviewHistory.items.map(toHistoryRow) : pulseHistory?.items?.map(toHistoryRow) ?? []).filter((item) => Boolean(item.date)).sort((a, b) => a.date.localeCompare(b.date))
    setState({
      pulse: value<MarketOverviewEltdx>(0),
      flow: value<MarketOverview>(1),
      indices: value<{ items?: IndexReturnItem[] }>(2)?.items ?? [],
      sectors: value<{ items?: StyleSectorItem[] }>(3)?.items ?? [],
      sentiment: value<MarketSentimentIndexResponse>(4),
      sentimentHistory: value<{ items?: MarketSentimentIndexHistoryItem[] }>(5)?.items ?? [],
      margin: value<MarginLatestResponse>(6),
      marginHistory: value<{ items?: MarginMarketSummary[] }>(7)?.items ?? [],
      etf: value<MarketEtfResponse>(8),
      futures: value<FuturesPositionLatestResponse>(9),
      indicator: value<MarketIndicatorResponse>(10),
      history,
      errors,
    })
    setLoading(false)
    setRefreshing(false)
  }, [])

  useEffect(() => {
    const firstLoad = window.setTimeout(() => void load(), 0)
    const timer = window.setInterval(() => void load(true), 60_000)
    return () => { window.clearTimeout(firstLoad); window.clearInterval(timer) }
  }, [load])

  const capturedAt = state.flow?.fetchedAt ?? state.pulse?.fetchedAt ?? state.etf?.fetchedAt ?? state.indicator?.capturedAt
  const hasAnyData = Boolean(state.pulse || state.flow || state.history.length || state.sentiment)

  return <WorkspaceShell sectionLabel="market" pageTitle="market overview"><div className="space-y-5"><header className="flex flex-col justify-between gap-4 border-b border-border/60 pb-4 lg:flex-row lg:items-end"><div className="min-w-0"><div className="flex items-center gap-2 text-xs font-medium text-muted-foreground"><Waves className="size-3.5 text-information" /><span>Market</span><span className="text-border">/</span><span>Overview</span></div><h1 className="mt-2 text-2xl font-semibold tracking-tight text-foreground">Market overview</h1><p className="mt-1 max-w-3xl text-sm leading-6 text-muted-foreground">用今天的行情、近 5 日节奏和 20 日背景，快速判断市场是在扩散、分化，还是等待确认。</p></div><Button type="button" variant="outline" size="sm" onClick={() => void load(true)} disabled={refreshing} title="刷新市场总览"><RefreshCw className={refreshing ? "animate-spin" : undefined} />刷新</Button></header><div className="flex flex-wrap items-center justify-between gap-3 rounded-lg border border-border/60 bg-muted/20 px-3 py-2.5 text-xs"><div className="flex flex-wrap items-center gap-x-4 gap-y-2 text-muted-foreground"><span className="inline-flex items-center gap-2 font-medium text-foreground"><span className={`size-1.5 rounded-full ${hasAnyData ? "bg-emerald-500" : "bg-amber-500"}`} />{hasAnyData ? "行情连接正常" : "等待行情快照"}</span><span className="inline-flex items-center gap-1.5"><Clock3 className="size-3.5" />最近采集 <strong className="font-mono font-medium text-foreground">{formatTime(capturedAt)}</strong></span><span>历史窗口 <strong className="font-mono font-medium text-foreground">{state.history.length || "—"}D</strong></span></div><span className="text-muted-foreground">自动刷新 60 秒{state.errors.length ? ` · ${state.errors.length} 项数据不可用` : ""}</span></div><IndexStrip items={state.indices} />{loading && !hasAnyData ? <div className="space-y-5"><Skeleton className="h-52 w-full" /><Skeleton className="h-72 w-full" /><Skeleton className="h-64 w-full" /></div> : <><Verdict state={state} stats={stats} loading={loading} /><SignalMatrix state={state} stats={stats} /><PulseHistory stats={stats} /><WindowCompare stats={stats} state={state} /><Consensus state={state} stats={stats} /><NextSession stats={stats} /><Reliability state={state} /></>}</div></WorkspaceShell>
}
