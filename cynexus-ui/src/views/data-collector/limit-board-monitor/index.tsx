import { useCallback, useEffect, useMemo, useState } from "react"
import { Activity, CircleAlert, RefreshCw } from "lucide-react"

import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { WorkspaceShell } from "@/layout/workspace-shell"
import { fetchLimitBoardSessionSummaries, type LimitBoardSessionSummary, type LimitBoardSessionSummaryResponse } from "@/services/market/indicator"

/**
 * Maintenance Notice:
 * Before modifying this page, review the related documentation under:
 * - design/front/infra/index.md
 * - design/front/infra/limit-board-monitor.detail.md
 * - design/front/reuse/reuse.md
 *
 * If this page's route, data source, API usage, component structure,
 * or reusable logic changes, update the corresponding design documents.
 */

type Session = "morning" | "afternoon"
type Direction = "all" | "up" | "down"

const sessionLabels: Record<Session, string> = { morning: "上午盘", afternoon: "下午盘" }

function amount(value?: number | null) {
  if (value == null) return "-"
  if (Math.abs(value) >= 100000000) return `${(value / 100000000).toFixed(2)} 亿`
  if (Math.abs(value) >= 10000) return `${(value / 10000).toFixed(1)} 万`
  return value.toFixed(0)
}

function duration(value?: number | null) {
  if (!value) return "-"
  return `${Math.floor(value / 60)}分${value % 60}秒`
}

function percent(value?: number | null) {
  return value == null ? "-" : `${(value * 100).toFixed(1)}%`
}

function time(value?: string | null) {
  return value ? new Date(value).toLocaleTimeString("zh-CN", { hour: "2-digit", minute: "2-digit" }) : "-"
}

function trendLabel(row: LimitBoardSessionSummary) {
  if (row.trend_direction === "increasing") return "封单增强"
  if (row.trend_direction === "decreasing") return "封单走弱"
  if (row.trend_direction === "flat") return "封单平稳"
  return "样本不足"
}

function SummaryTable({ rows }: { rows: LimitBoardSessionSummary[] }) {
  return <div className="overflow-x-auto"><table className="w-full min-w-[1050px] text-sm"><thead><tr className="border-b border-border/60 bg-muted/20 text-left text-xs text-muted-foreground"><th className="px-4 py-2.5 font-medium">股票</th><th className="px-4 py-2.5 font-medium">方向 / 状态</th><th className="px-4 py-2.5 text-right font-medium">最大封单额</th><th className="px-4 py-2.5 text-right font-medium">均值 / 中位数</th><th className="px-4 py-2.5 text-right font-medium">封板时长</th><th className="px-4 py-2.5 text-right font-medium">开板 / 回封</th><th className="px-4 py-2.5 font-medium">封单趋势</th><th className="px-4 py-2.5 text-right font-medium">覆盖</th></tr></thead><tbody>{rows.length === 0 ? <tr><td colSpan={8} className="px-4 py-14 text-center text-xs text-muted-foreground">当前交易日暂无已规整的 session 汇总</td></tr> : rows.map((row) => <tr key={`${row.trade_date}-${row.trade_session}-${row.code}`} className="border-b border-border/40 last:border-0"><td className="px-4 py-3"><div className="font-medium">{row.name || row.code}</div><div className="mt-0.5 font-mono text-xs text-muted-foreground">{row.code} {row.market || ""}</div></td><td className="px-4 py-3"><div className={row.direction === "down" ? "text-emerald-600" : "text-rose-600"}>{row.direction === "down" ? "跌停" : "涨停"}</div><div className="mt-0.5 text-xs text-muted-foreground">{row.is_sealed_at_close ? "收盘封板" : row.closing_status === "opened" ? "曾封后开板" : "触板"}</div></td><td className="px-4 py-3 text-right font-mono tabular-nums">{amount(row.max_sealed_amount)}<div className="mt-0.5 text-[11px] text-muted-foreground">峰值 {time(row.peak_sealed_amount_at)}</div></td><td className="px-4 py-3 text-right font-mono tabular-nums">{amount(row.avg_sealed_amount)}<div className="mt-0.5 text-[11px] text-muted-foreground">{amount(row.median_sealed_amount)}</div></td><td className="px-4 py-3 text-right font-mono tabular-nums">{duration(row.sealed_duration_seconds)}<div className="mt-0.5 text-[11px] text-muted-foreground">首封 {time(row.first_sealed_at)}</div></td><td className="px-4 py-3 text-right font-mono tabular-nums">{row.open_count ?? 0} / {row.reseal_count ?? 0}</td><td className="px-4 py-3"><div className="font-medium">{trendLabel(row)}</div><div className="mt-0.5 font-mono text-[11px] text-muted-foreground">变化 {amount(row.sealed_amount_change)}</div></td><td className="px-4 py-3 text-right font-mono tabular-nums">{percent(row.coverage_ratio)}<div className="mt-0.5 text-[11px] text-muted-foreground">缺口 {row.gap_count ?? 0}</div></td></tr>)}</tbody></table></div>
}

export default function LimitBoardMonitorPage() {
  const [tradeDate, setTradeDate] = useState("")
  const [session, setSession] = useState<Session>("morning")
  const [direction, setDirection] = useState<Direction>("all")
  const [payload, setPayload] = useState<LimitBoardSessionSummaryResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      setPayload(await fetchLimitBoardSessionSummaries({ tradeDate: tradeDate || undefined, tradeSession: session, direction, limit: 240 }))
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "封板汇总暂时不可用")
    } finally {
      setLoading(false)
    }
  }, [direction, session, tradeDate])

  useEffect(() => {
    const timer = window.setTimeout(() => void load(), 0)
    return () => window.clearTimeout(timer)
  }, [load])

  const rows = useMemo(() => payload?.items ?? [], [payload])
  const sealedCount = rows.filter((row) => row.is_sealed_at_close).length
  const avgCoverage = rows.length ? rows.reduce((sum, row) => sum + (row.coverage_ratio ?? 0), 0) / rows.length : null

  return <WorkspaceShell sectionLabel="Data Collector" pageTitle="Limit Board Monitor"><div className="space-y-5"><header className="flex flex-col justify-between gap-4 border-b border-border/60 pb-4 lg:flex-row lg:items-end"><div><div className="flex items-center gap-2 text-xs font-medium text-muted-foreground"><Activity className="size-3.5 text-information" />封板额 session 监控</div><h1 className="mt-2 text-2xl font-semibold tracking-tight">涨跌停封板监控</h1><p className="mt-1 max-w-3xl text-sm text-muted-foreground">盘中每 5 秒采集涨跌停股票的买一/卖一封单额，交易时段结束后仅展示规整后的封板强度与趋势特征。</p></div><div className="flex flex-wrap items-center gap-2"><Input type="date" value={tradeDate} onChange={(event) => setTradeDate(event.target.value)} className="h-8 w-[145px] text-xs" aria-label="选择交易日" /><Tabs value={session} onValueChange={(value) => setSession(value as Session)}><TabsList variant="line" className="h-8"><TabsTrigger value="morning" className="px-3 text-xs">上午盘</TabsTrigger><TabsTrigger value="afternoon" className="px-3 text-xs">下午盘</TabsTrigger></TabsList></Tabs><Button type="button" variant="outline" size="sm" onClick={() => void load()} disabled={loading}><RefreshCw className={loading ? "animate-spin" : undefined} />刷新</Button></div></header><div className="flex flex-wrap items-center gap-x-5 gap-y-2 rounded-lg border border-border/60 bg-muted/25 px-3 py-2.5 text-xs text-muted-foreground"><span>交易日 <strong className="font-mono font-medium text-foreground">{payload?.tradeDate || tradeDate || "最新"}</strong></span><span>时段 <strong className="font-medium text-foreground">{sessionLabels[session]}</strong></span><span>已规整 <strong className="font-mono font-medium text-foreground">{rows.length}</strong> 只</span><span>收盘封板 <strong className="font-mono font-medium text-foreground">{sealedCount}</strong> 只</span><span>平均覆盖 <strong className="font-mono font-medium text-foreground">{percent(avgCoverage)}</strong></span><span className="ml-auto">{payload?.source || "等待汇总数据"}</span></div>{error ? <div className="flex items-start gap-2 rounded-lg border border-amber-200 bg-amber-50 px-3 py-2.5 text-sm text-amber-800 dark:border-amber-900/60 dark:bg-amber-950/20 dark:text-amber-300"><CircleAlert className="mt-0.5 size-4 shrink-0" /><span>{error}</span></div> : null}<section className="overflow-hidden rounded-lg border border-border/70 bg-card"><div className="flex flex-wrap items-center justify-between gap-3 border-b border-border/60 px-4 py-3"><div><h2 className="text-sm font-semibold">{sessionLabels[session]}封板清单</h2><p className="mt-1 text-xs text-muted-foreground">数据来自交易时段收盘规整结果，不展示 Redis 原始 5 秒快照。</p></div><Tabs value={direction} onValueChange={(value) => setDirection(value as Direction)}><TabsList variant="line" className="h-8"><TabsTrigger value="all" className="px-3 text-xs">全部</TabsTrigger><TabsTrigger value="up" className="px-3 text-xs">涨停</TabsTrigger><TabsTrigger value="down" className="px-3 text-xs">跌停</TabsTrigger></TabsList></Tabs></div>{loading ? <div className="px-4 py-14 text-center text-xs text-muted-foreground">正在加载封板汇总…</div> : <SummaryTable rows={rows} />}</section></div></WorkspaceShell>
}
