import { useEffect, useMemo, useRef, useState } from "react"
import { Activity, Clock3, Layers3 } from "lucide-react"

import { Skeleton } from "@/components/ui/skeleton"
import { fetchTradeReviewMarketState, type TradeReviewMarketStateResponse } from "@/services/market/indicator"
import { fetchEasyTdxTimeshare } from "@/services/market/easy-tdx"
import { cn } from "@/lib/utils"

type Props = {
  tradeDate: string
  currentTimestamp: number
  industryName?: string | null
  industryCode?: string | null
  mode?: "intraday" | "daily" | "aggregate"
  realEventCount?: number
}
type Row = Record<string, unknown>
type MiniPoint = { price: number; timestamp: number }

const number = (row: Row | null | undefined, camel: string, snake: string) => {
  const parsed = Number(row?.[camel] ?? row?.[snake])
  return Number.isFinite(parsed) ? parsed : 0
}
const records = (value: unknown): Row[] => {
  if (Array.isArray(value)) return value.filter((item): item is Row => !!item && typeof item === "object")
  if (typeof value !== "string") return []
  try {
    const parsed: unknown = JSON.parse(value)
    return Array.isArray(parsed) ? parsed.filter((item): item is Row => !!item && typeof item === "object") : []
  } catch { return [] }
}
const timeAt = (timestamp: number, fallback: string) => timestamp ? new Date(timestamp).toISOString() : `${fallback}T15:00:00+08:00`
const signed = (value: number) => `${value > 0 ? "+" : ""}${value.toFixed(2)}%`
const minuteText = (value: unknown) => String(value ?? "").match(/(\d{2}:\d{2})/)?.[1] ?? ""
const pointTimestamp = (row: Row, tradeDate: string) => {
  const raw = row.trade_time ?? row.datetime ?? row.timestamp
  if (typeof raw === "number" && Number.isFinite(raw)) return raw < 10_000_000_000 ? raw * 1000 : raw
  const text = String(raw ?? "")
  if (/^\d{4}-\d{2}-\d{2}[T\s]/.test(text)) {
    const parsed = Date.parse(/[zZ]|[+-]\d{2}:?\d{2}$/.test(text) ? text : `${text.replace(" ", "T")}+08:00`)
    if (Number.isFinite(parsed)) return parsed
  }
  const minute = minuteText(row.time_label ?? row.time ?? raw)
  return minute ? Date.parse(`${tradeDate}T${minute}:00+08:00`) : 0
}
const miniPoints = (rows: Row[], tradeDate: string): MiniPoint[] => rows.map((row) => ({
  price: Number(row.price ?? row.last_price ?? row.close), timestamp: pointTimestamp(row, tradeDate),
})).filter((row) => Number.isFinite(row.price) && row.price > 0 && Number.isFinite(row.timestamp) && row.timestamp > 0)

function Metric({ label, value, tone }: { label: string; value: string; tone?: "up" | "down" | "warn" }) {
  return <div className="min-w-0 bg-background/70 px-2 py-1"><div className="truncate text-[8px] text-muted-foreground">{label}</div><div className={cn("truncate font-mono text-[11px] font-semibold leading-4 tabular-nums", tone === "up" && "text-rose-600", tone === "down" && "text-emerald-600", tone === "warn" && "text-amber-600")}>{value}</div></div>
}

function MiniTimeshare({ points, loading, change, currentTimestamp }: { points?: MiniPoint[]; loading?: boolean; change: number; currentTimestamp: number }) {
  if (loading) return <Skeleton className="mt-1.5 h-9 w-full" />
  const visible = points?.filter((item) => !currentTimestamp || item.timestamp <= currentTimestamp) ?? []
  if (!visible.length) return <div className="mt-1.5 h-9 text-[9px] leading-9 text-muted-foreground">截至游标暂无分时</div>
  const min = Math.min(...visible.map((item) => item.price))
  const max = Math.max(...visible.map((item) => item.price))
  const span = Math.max(0.0001, max - min)
  const total = Math.max(1, points?.length ?? 0)
  const positions = new Map(points?.map((point, index) => [point.timestamp, index]))
  const line = visible.map((item) => {
    const index = positions.get(item.timestamp) ?? 0
    return `${index ? "L" : "M"}${(index / Math.max(1, total - 1) * 100).toFixed(1)},${(18 - (item.price - min) / span * 16).toFixed(1)}`
  }).join(" ")
  const end = positions.get(visible.at(-1)?.timestamp ?? 0) ?? 0
  const endX = end / Math.max(1, total - 1) * 100
  return <svg viewBox="0 0 100 20" preserveAspectRatio="none" className="mt-1.5 h-9 w-full"><path d={`${line} L${endX.toFixed(1)},20 L0,20 Z`} fill={change >= 0 ? "#dc2626" : "#16a34a"} fillOpacity=".12" /><path d={line} fill="none" stroke={change >= 0 ? "#dc2626" : "#16a34a"} strokeWidth="1.35" vectorEffect="non-scaling-stroke" /></svg>
}

function StockCard({ stock, points, loading, currentTimestamp, cacheKey, showSealDetails = false }: { stock: Row; points?: MiniPoint[]; loading?: boolean; currentTimestamp: number; cacheKey: string; showSealDetails?: boolean }) {
  const code = String(stock.code ?? "")
  const change = number(stock, "changePct", "change_pct")
  const firstSealAt = String(stock.firstSealAt ?? stock.first_seal_at ?? "")
  const firstSeal = minuteText(firstSealAt)
  const opened = number(stock, "openCount", "open_count")
  return <div className="min-w-0 bg-background/65 px-2 py-1.5"><div className="flex items-center justify-between gap-1.5"><div className="truncate text-[11px] font-medium">{String(stock.name ?? code ?? "-")}</div><div className={cn("shrink-0 font-mono text-[10px]", change >= 0 ? "text-rose-600" : "text-emerald-600")}>{signed(change)}</div></div><MiniTimeshare points={points} loading={loading} change={change} currentTimestamp={currentTimestamp} /><div className="truncate font-mono text-[9px] text-muted-foreground">{number(stock, "height", "height")}板 · {cacheKey.split(":").at(-1)}</div>{showSealDetails && firstSeal ? <div className="truncate font-mono text-[9px] text-muted-foreground">首封 {firstSeal}{opened > 0 ? ` · 炸 ${opened}次` : ""}</div> : null}</div>
}

function PoolStrip({ title, stocks, timeshares, loading, currentTimestamp, cacheKeyFor, accent, showSealDetails = false }: { title: string; stocks: Row[]; timeshares: Record<string, MiniPoint[]>; loading: Record<string, boolean>; currentTimestamp: number; cacheKeyFor: (code: string) => string; accent?: boolean; showSealDetails?: boolean }) {
  return <section className={cn("min-w-0 bg-muted/30 px-2.5 py-2", accent && "bg-rose-500/[0.035]")}><div className="flex items-center justify-between gap-2"><span className="truncate text-[10px] text-muted-foreground">{title}</span><span className="font-mono text-[9px] text-muted-foreground">{stocks.length}只</span></div>{stocks.length ? <div className="mt-1.5 grid grid-cols-[repeat(auto-fit,minmax(180px,1fr))] gap-1.5">{stocks.map((stock) => { const code = String(stock.code ?? ""); const key = cacheKeyFor(code); return <StockCard key={code} stock={stock} points={timeshares[key]} loading={loading[key]} currentTimestamp={currentTimestamp} cacheKey={key} showSealDetails={showSealDetails} /> })}</div> : <div className="mt-1 text-[9px] text-muted-foreground">暂无可用标的</div>}</section>
}

function LadderPool({ stocks, timeshares, loading, currentTimestamp, cacheKeyFor }: { stocks: Row[]; timeshares: Record<string, MiniPoint[]>; loading: Record<string, boolean>; currentTimestamp: number; cacheKeyFor: (code: string) => string }) {
  const groups = useMemo(() => Array.from(stocks.reduce((result, stock) => {
    const height = number(stock, "height", "height")
    result.set(height, [...(result.get(height) ?? []), stock])
    return result
  }, new Map<number, Row[]>()).entries()).sort(([left], [right]) => right - left), [stocks])
  return <section className="min-w-0 bg-rose-500/[0.035] px-2.5 py-2"><div className="flex items-center justify-between gap-2"><span className="truncate text-[10px] text-muted-foreground">行业连板梯度</span><span className="font-mono text-[9px] text-muted-foreground">{stocks.length}只</span></div>{groups.length ? <div className="mt-1.5 space-y-1.5">{groups.map(([height, members]) => <div key={height} className="grid grid-cols-[34px_minmax(0,1fr)] gap-1.5"><div className="pt-1.5 font-mono text-[10px] font-semibold text-rose-600">{height}板</div><div className="grid grid-cols-[repeat(auto-fit,minmax(180px,1fr))] gap-1.5">{members.map((stock) => { const code = String(stock.code ?? ""); const key = cacheKeyFor(code); return <StockCard key={code} stock={stock} points={timeshares[key]} loading={loading[key]} currentTimestamp={currentTimestamp} cacheKey={key} showSealDetails /> })}</div></div>)}</div> : <div className="mt-1 text-[9px] text-muted-foreground">当前游标暂无行业连板标的</div>}</section>
}

export function MarketStateMaterializedSlot({ tradeDate, currentTimestamp, industryName, industryCode, mode = "intraday", realEventCount = 0 }: Props) {
  const [state, setState] = useState<TradeReviewMarketStateResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [timeshares, setTimeshares] = useState<Record<string, MiniPoint[]>>({})
  const [timeshareLoading, setTimeshareLoading] = useState<Record<string, boolean>>({})
  const replayRequest = useRef({ scope: "", cursor: 0, direction: 1, appliedCursor: 0 })
  useEffect(() => {
    if (mode === "aggregate") {
      return
    }
    if (!industryCode) return
    const scope = `${tradeDate}|${mode}|${industryCode}`
    const request = replayRequest.current
    if (request.scope !== scope) {
      request.scope = scope
      request.cursor = currentTimestamp
      request.appliedCursor = 0
      request.direction = 1
    } else {
      request.direction = currentTimestamp >= request.cursor ? 1 : -1
      request.cursor = currentTimestamp
    }
    const requestCursor = currentTimestamp
    queueMicrotask(() => { setError(null) })
    void fetchTradeReviewMarketState({ tradeDate, mode, asOf: mode === "intraday" ? timeAt(currentTimestamp, tradeDate) : undefined, membership: "union", industryCode: industryCode ?? undefined, windowDays: 60 })
      .then((next) => {
        const latest = replayRequest.current
        if (latest.scope !== scope) return
        // Do not discard every in-flight minute while replay is running.
        // Apply completed snapshots in cursor order; rapid playback now shows
        // each available ladder transition instead of only the final frame.
        if (latest.direction > 0 && requestCursor < latest.appliedCursor) return
        if (latest.direction < 0 && latest.appliedCursor && requestCursor > latest.appliedCursor) return
        latest.appliedCursor = requestCursor
        setState(next)
        setError(next.dataStatus === "unavailable" ? "市场状态尚未物化" : null)
      })
      .catch((cause) => { if (replayRequest.current.scope === scope) setError(cause instanceof Error ? cause.message : "加载市场状态失败") })
      .finally(() => { if (replayRequest.current.scope === scope) setLoading(false) })
  }, [currentTimestamp, industryCode, mode, tradeDate])

  const market = industryCode ? state?.industries?.[0] ?? null : null
  const pools = useMemo(() => new Map((state?.pools ?? []).map((pool) => [String(pool.poolType ?? pool.pool_type), records(pool.stocks)])), [state])
  const ladder = useMemo(() => pools.get("ladder") ?? [], [pools])
  const yesterdayLimitUpFeedback = useMemo(() => pools.get("yesterday_limit_up_feedback") ?? [], [pools])
  const poolGroups = useMemo(() => [
    { title: "行业封板池", stocks: pools.get("sealed") ?? pools.get("limit_up") ?? [], showSealDetails: true },
    { title: "行业炸板池", stocks: pools.get("broken") ?? [] },
    { title: "昨日涨停反馈", stocks: pools.get("yesterday_limit_up_feedback") ?? [] },
  ], [pools])
  const timeshareCodes = useMemo(() => [...new Set([ladder, ...poolGroups.map((group) => group.stocks)].flatMap((stocks) => stocks.map((stock) => String(stock.code ?? "")).filter(Boolean)))], [ladder, poolGroups])
  const timeshareKey = timeshareCodes.join(",")
  const cacheKeyFor = (code: string) => `${tradeDate}:${code}`
  useEffect(() => {
    let active = true
    timeshareCodes.forEach((code) => {
      const cacheKey = cacheKeyFor(code)
      if (timeshares[cacheKey] || timeshareLoading[cacheKey]) return
      setTimeshareLoading((current) => ({ ...current, [cacheKey]: true }))
      void fetchEasyTdxTimeshare({ code, tradeDate }).then((response) => {
        if (active) setTimeshares((current) => ({ ...current, [cacheKey]: miniPoints(response.items, tradeDate) }))
      }).catch(() => {
        if (active) setTimeshares((current) => ({ ...current, [cacheKey]: [] }))
      }).finally(() => {
        if (active) setTimeshareLoading((current) => ({ ...current, [cacheKey]: false }))
      })
    })
    return () => { active = false }
  // The joined target list intentionally prevents state writes from issuing duplicate requests.
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [timeshareKey, tradeDate])

  const latest = (state?.events ?? []).at(-1)
  if (mode === "aggregate") return <div className="flex h-full items-center justify-center bg-muted/25 px-3 text-center text-xs text-muted-foreground">周K、月K仅展示聚合摘要；行业连板、封板与昨日反馈请切换至日K逐日回放。</div>
  if (!industryCode) return <div className="flex h-full items-center justify-center bg-muted/25 px-3 text-xs text-muted-foreground">当前标的尚未识别行业，无法展示行业内复盘池</div>
  if (loading && !state) return <div className="grid h-full min-h-0 grid-cols-4 gap-2 p-2"><Skeleton className="h-full" /><Skeleton className="h-full" /><Skeleton className="h-full" /><Skeleton className="h-full" /></div>
  if (error || !state || !market) return <div className="flex h-full items-center justify-center bg-muted/25 px-3 text-xs text-muted-foreground">{error ?? "暂无市场状态"}</div>
  const up = number(market, "advancingCount", "advancing_count")
  const down = number(market, "decliningCount", "declining_count")
  const flat = number(market, "flatCount", "flat_count")
  const total = Math.max(1, up + down + flat)
  const touched = number(market, "touchedLimitUpCount", "touched_limit_up_count")
  const sealed = number(market, "sealedLimitUpCount", "sealed_limit_up_count")
  const broken = number(market, "brokenLimitUpCount", "broken_limit_up_count")
  const limitDown = number(market, "sealedLimitDownCount", "sealed_limit_down_count")
  const yesterdayLimitUpAverage = yesterdayLimitUpFeedback.length
    ? yesterdayLimitUpFeedback.reduce((sum, stock) => sum + number(stock, "changePct", "change_pct"), 0) / yesterdayLimitUpFeedback.length
    : null
  const yesterdayLimitUpValue = yesterdayLimitUpAverage == null ? "-" : signed(yesterdayLimitUpAverage)
  return <div className="flex h-full min-h-0 flex-col gap-2 overflow-auto px-3 py-2"><header className="flex min-w-0 items-center justify-between gap-2 text-[10px]"><div className="flex min-w-0 items-center gap-1.5"><Activity className="size-3 text-muted-foreground" /><span className="font-semibold">行业市场状态</span><span className="truncate text-muted-foreground">{industryName ?? "未关联行业"} · {mode === "intraday" ? "分钟游标" : "日级收盘"}</span></div><span className="shrink-0 font-mono text-muted-foreground">{state.dataStatus} · Indicator {realEventCount}</span></header><div className="grid shrink-0 grid-cols-[minmax(0,1fr)_minmax(0,5fr)] gap-2"><section className="min-w-0 bg-muted/30 px-2 py-1"><div className="text-[8px] text-muted-foreground">行业广度</div><div className="mt-0.5 flex h-1.5 overflow-hidden rounded-full bg-muted"><span className="bg-rose-500/75" style={{ width: `${up / total * 100}%` }} /><span className="bg-muted-foreground/25" style={{ width: `${flat / total * 100}%` }} /><span className="bg-emerald-500/75" style={{ width: `${down / total * 100}%` }} /></div><div className="mt-0.5 grid grid-cols-3 font-mono text-[8px]"><span className="text-rose-600">涨 {up}</span><span>平 {flat}</span><span className="text-emerald-600">跌 {down}</span></div></section><section className="grid grid-cols-5 gap-1 bg-muted/30 p-1"><Metric label="涨停 / 触板" value={`${sealed} / ${touched}`} tone="up" /><Metric label="炸板 / 跌停" value={`${broken} / ${limitDown}`} tone="warn" /><Metric label="封板率" value={`${number(market, "sealRate", "seal_rate").toFixed(1)}%`} /><Metric label="最高连板" value={`${number(market, "maxHeight", "max_height")}板`} /><Metric label={`昨日涨停收益${yesterdayLimitUpFeedback.length ? ` · ${yesterdayLimitUpFeedback.length}` : ""}`} value={yesterdayLimitUpValue} tone={yesterdayLimitUpAverage == null ? undefined : yesterdayLimitUpAverage >= 0 ? "up" : "down"} /></section></div><div className="grid min-h-0 flex-1 gap-2 lg:grid-cols-2"><LadderPool stocks={ladder} timeshares={timeshares} loading={timeshareLoading} currentTimestamp={currentTimestamp} cacheKeyFor={cacheKeyFor} />{poolGroups.map((group) => <PoolStrip key={group.title} {...group} timeshares={timeshares} loading={timeshareLoading} currentTimestamp={currentTimestamp} cacheKeyFor={cacheKeyFor} />)}</div><footer className="flex min-w-0 items-center gap-2 bg-muted/25 px-2 py-1 text-[9px]"><Clock3 className="size-3 shrink-0 text-muted-foreground" /><span className="truncate">{latest ? `${String(latest.eventAt ?? latest.event_at ?? "").slice(11, 16)} · ${String(latest.name ?? latest.code ?? "")} ${String(latest.eventType ?? latest.event_type ?? "")}` : "截至当前游标暂无新增事件"}</span><span className="ml-auto shrink-0 text-muted-foreground">{String(state.trend?.phase ?? "-")}</span><Layers3 className="size-3 shrink-0 text-muted-foreground" /></footer></div>
}
