import { useEffect, useMemo, useRef, useState, type ReactNode } from "react"
import { Activity, ChevronDown, ChevronUp, Layers3, RefreshCw, TrendingDown, TrendingUp } from "lucide-react"

import { Skeleton } from "@/components/ui/skeleton"
import {
  fetchTradeReviewMarketState,
  type MarketIndicatorBreadth,
  type MarketIndicatorEvent,
  type MarketIndicatorIndustry,
  type MarketIndicatorReplayTimelineResponse,
} from "@/services/market/indicator"
import { type LimitEmotionPayload, type LimitEmotionStock } from "@/services/market/pulse"
import { fetchEasyTdxTimeshare, type EasyTdxRow } from "@/services/market/easy-tdx"
import { cn } from "@/lib/utils"

type Mode = "intraday" | "daily" | "aggregate"

type Props = {
  tradeDate: string
  currentTimestamp: number
  industryCode?: string | null
  mode: Mode
  dailyWindowDays: number
  onDailyWindowDaysChange: (days: number) => void
  onStartMinuteReplay?: (tradeDate: string) => void
  realEventCount?: number
  timeline?: MarketIndicatorReplayTimelineResponse | null
}

type LadderStock = LimitEmotionStock & { height: number; industry?: string }

type MiniPoint = { time: string; timestamp: number; price: number; volume: number; changePct: number }

type MinuteSnapshot = {
  breadth: MarketIndicatorBreadth | null
  events: MarketIndicatorEvent[]
  industries: MarketIndicatorIndustry[]
}

type MarketStateRequest = {
  cursorDate: string
  asOf: string
  mode: Mode
  windowDays: number
  timestamp: number
  sessionKey: string
}

function numberValue(value: unknown): number | null {
  if (typeof value === "number" && Number.isFinite(value)) return value
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : null
}

function recordNumber(record: Record<string, unknown> | null | undefined, camel: string, snake: string) {
  return numberValue(record?.[camel] ?? record?.[snake])
}

function jsonArray(value: unknown): Array<Record<string, unknown>> {
  if (Array.isArray(value)) return value.filter((item): item is Record<string, unknown> => !!item && typeof item === "object")
  if (typeof value !== "string") return []
  try {
    const parsed: unknown = JSON.parse(value)
    return Array.isArray(parsed) ? parsed.filter((item): item is Record<string, unknown> => !!item && typeof item === "object") : []
  } catch {
    return []
  }
}

function dateFromTimestamp(timestamp: number, fallback: string) {
  if (!timestamp) return fallback
  return new Intl.DateTimeFormat("en-CA", {
    timeZone: "Asia/Shanghai",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).format(new Date(timestamp))
}

function isoAsOf(timestamp: number, tradeDate: string) {
  if (timestamp) return new Date(timestamp).toISOString()
  return `${tradeDate}T15:00:00+08:00`
}

function timeValue(value: unknown) {
  const text = String(value ?? "")
  return text.match(/(\d{2}:\d{2})/)?.[1] ?? ""
}

function pointTimestamp(row: EasyTdxRow, tradeDate: string, time: string) {
  const raw = row.trade_time ?? row.datetime ?? row.timestamp
  if (typeof raw === "number" && Number.isFinite(raw)) return raw < 10_000_000_000 ? raw * 1000 : raw
  const text = String(raw ?? "")
  if (/^\d{4}-\d{2}-\d{2}[T\s]/.test(text)) {
    const parsed = Date.parse(/[zZ]|[+-]\d{2}:?\d{2}$/.test(text) ? text : `${text.replace(" ", "T")}+08:00`)
    if (Number.isFinite(parsed)) return parsed
  }
  return time ? Date.parse(`${tradeDate}T${time}:00+08:00`) : 0
}

function parseTimeshare(items: EasyTdxRow[], previousClose: number | null, tradeDate: string): MiniPoint[] {
  let firstPrice: number | null = null
  return items.map((row) => {
    const price = numberValue(row.price ?? row.last_price ?? row.close) ?? 0
    const volume = numberValue(row.volume ?? row.vol ?? row.volume_lots) ?? 0
    const time = timeValue(row.time_label ?? row.time ?? row.trade_time ?? row.datetime ?? row.timestamp)
    firstPrice ??= previousClose ?? price
    return {
      time,
      timestamp: pointTimestamp(row, tradeDate, time),
      price,
      volume,
      changePct: firstPrice ? (price / firstPrice - 1) * 100 : 0,
    }
  }).filter((item) => item.price > 0 && item.timestamp > 0)
}

function signed(value: number | null | undefined, digits = 2) {
  if (value == null || !Number.isFinite(value)) return "-"
  return `${value > 0 ? "+" : ""}${value.toFixed(digits)}%`
}

function pct(value: number | null | undefined, digits = 1) {
  if (value == null || !Number.isFinite(value)) return "-"
  return `${value.toFixed(digits)}%`
}

function ladderCounts(value: unknown) {
  const levels = jsonArray(value)
  const total = levels.reduce((sum, level) => sum + (recordNumber(level, "count", "count") ?? 0), 0)
  const continuing = levels.reduce((sum, level) => sum + ((recordNumber(level, "height", "height") ?? 0) >= 2 ? (recordNumber(level, "count", "count") ?? 0) : 0), 0)
  return { total, continuing }
}

function ladderSummary(value: unknown) {
  return jsonArray(value)
    .sort((left, right) => (recordNumber(right, "height", "height") ?? 0) - (recordNumber(left, "height", "height") ?? 0))
    .map((level) => `${recordNumber(level, "height", "height") ?? 0}板×${recordNumber(level, "count", "count") ?? 0}`)
    .join(" · ")
}

function Card({ children, className }: { children: ReactNode; className?: string }) {
  return <section className={cn("min-w-0 rounded-lg bg-muted/25 p-3", className)}>{children}</section>
}

function Heading({ icon: Icon, title, detail }: { icon: typeof Activity; title: string; detail?: string }) {
  return (
    <div className="flex min-w-0 items-center justify-between gap-2">
      <div className="flex min-w-0 items-center gap-1.5">
        <Icon className="size-3.5 shrink-0 text-muted-foreground" aria-hidden="true" />
        <h3 className="truncate text-xs font-semibold">{title}</h3>
      </div>
      {detail ? <span className="shrink-0 text-[10px] text-muted-foreground">{detail}</span> : null}
    </div>
  )
}

function Metric({ label, value, tone }: { label: string; value: string; tone?: "up" | "down" | "warn" }) {
  return (
    <div className="min-w-0 rounded-md bg-background/70 px-2.5 py-2">
      <div className="truncate text-[10px] text-muted-foreground">{label}</div>
      <div className={cn("mt-0.5 truncate font-mono text-sm font-semibold tabular-nums", tone === "up" && "text-rose-600", tone === "down" && "text-emerald-600", tone === "warn" && "text-amber-600")}>{value}</div>
    </div>
  )
}

function Breadth({ value }: { value: MarketIndicatorBreadth | null }) {
  const up = value?.advancingCount ?? 0
  const down = value?.decliningCount ?? 0
  const flat = value?.flatCount ?? 0
  const total = Math.max(1, value?.totalCount ?? up + down + flat)
  return (
    <div>
      <div className="flex h-2 overflow-hidden rounded-full bg-muted">
        <div className="bg-rose-500/80" style={{ width: `${up / total * 100}%` }} />
        <div className="bg-muted-foreground/25" style={{ width: `${flat / total * 100}%` }} />
        <div className="bg-emerald-500/80" style={{ width: `${down / total * 100}%` }} />
      </div>
      <div className="mt-1 flex items-center justify-between font-mono text-[10px] tabular-nums">
        <span className="text-rose-600">涨 {up}</span>
        <span className="text-muted-foreground">平 {flat}</span>
        <span className="text-emerald-600">跌 {down}</span>
      </div>
    </div>
  )
}

function MiniTimeshare({ points, name, currentTimestamp }: { points: MiniPoint[]; name: string; currentTimestamp: number }) {
  if (!points.length) return <div className="flex h-16 items-center justify-center text-[10px] text-muted-foreground">暂无分时</div>
  // Use the full session for the x scale while strictly clipping the plotted
  // series to the replay cursor. The graph then expands/retracts with replay
  // instead of always showing the closing snapshot.
  const visible = points.filter((point) => !currentTimestamp || point.timestamp <= currentTimestamp)
  if (!visible.length) return <div className="flex h-16 items-center justify-center text-[10px] text-muted-foreground">截至游标暂无分时</div>
  const width = 180
  const height = 64
  const min = Math.min(...visible.map((item) => item.changePct), 0)
  const max = Math.max(...visible.map((item) => item.changePct), 0)
  const span = Math.max(0.2, max - min)
  const x = (index: number) => index / Math.max(1, points.length - 1) * width
  const y = (value: number) => 3 + (max - value) / span * (height - 16)
  const zeroY = y(0)
  const volumeMax = Math.max(...visible.map((item) => item.volume), 1)
  const pointIndex = new Map(points.map((point, index) => [point.timestamp, index]))
  const path = visible.map((item) => {
    const index = pointIndex.get(item.timestamp) ?? 0
    return `${index ? "L" : "M"}${x(index).toFixed(1)},${y(item.changePct).toFixed(1)}`
  }).join(" ")
  return (
    <div className="min-w-0">
      <svg viewBox={`0 0 ${width} ${height}`} preserveAspectRatio="none" className="h-16 w-full" aria-label={`${name} 分时`}>
        <line x1="0" x2={width} y1={zeroY} y2={zeroY} stroke="currentColor" strokeOpacity="0.14" strokeDasharray="2 2" />
        {visible.map((item) => {
          const index = pointIndex.get(item.timestamp) ?? 0
          const previous = points[index - 1]?.changePct ?? 0
          const barHeight = Math.max(1, item.volume / volumeMax * 12)
          return <rect key={`${item.time}-${index}`} x={x(index)} y={height - barHeight} width={Math.max(0.5, width / points.length - 0.2)} height={barHeight} fill={item.changePct >= previous ? "#dc2626" : "#16a34a"} fillOpacity="0.35" />
        })}
        <path d={path} fill="none" stroke={visible.at(-1)!.changePct >= 0 ? "#dc2626" : "#16a34a"} strokeWidth="1.5" vectorEffect="non-scaling-stroke" />
      </svg>
      <div className="flex items-center justify-between gap-2 font-mono text-[9px] tabular-nums">
        <span className={visible.at(-1)!.changePct >= 0 ? "text-rose-600" : "text-emerald-600"}>{signed(visible.at(-1)!.changePct)}</span>
        <span className="truncate text-muted-foreground">{visible.at(-1)!.time}</span>
      </div>
    </div>
  )
}

export function MarketStateTab({
  tradeDate,
  currentTimestamp,
  mode,
  dailyWindowDays,
  onDailyWindowDaysChange,
  onStartMinuteReplay,
  realEventCount = 0,
  timeline,
}: Props) {
  // The timeline remains part of the public component contract while the
  // materialized state is the source used by this tab.
  void timeline
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [emotion, setEmotion] = useState<LimitEmotionPayload | null>(null)
  const [marketBreadth, setMarketBreadth] = useState<MarketIndicatorBreadth | null>(null)
  const [industries, setIndustries] = useState<MarketIndicatorIndustry[]>([])
  const [minute, setMinute] = useState<MinuteSnapshot | null>(null)
  const [timeshare, setTimeshare] = useState<Record<string, MiniPoint[]>>({})
  const [timeshareLoading, setTimeshareLoading] = useState<Record<string, boolean>>({})
  const [history, setHistory] = useState<Array<Record<string, unknown>>>([])
  const [phase, setPhase] = useState<Record<string, unknown> | null>(null)
  const [expandedFirstBoardScope, setExpandedFirstBoardScope] = useState<string | null>(null)
  const [industryLimitPulse, setIndustryLimitPulse] = useState<Record<string, "up" | "down">>({})
  const industryLimitCountsRef = useRef({ scope: "", counts: new Map<string, number>() })
  const industryPulseTimersRef = useRef(new Map<string, ReturnType<typeof setTimeout>>())
  const marketStateMountedRef = useRef(true)
  const marketStateInFlightRef = useRef(false)
  const pendingMarketStateRequestRef = useRef<MarketStateRequest | null>(null)
  const latestMarketStateRequestRef = useRef<MarketStateRequest | null>(null)

  const cursorDate = dateFromTimestamp(currentTimestamp, tradeDate)
  const asOf = isoAsOf(currentTimestamp, tradeDate)
  const firstBoardScope = `${cursorDate}:${mode}`
  const firstBoardExpanded = expandedFirstBoardScope === firstBoardScope

  useEffect(() => {
    // React Strict Mode replays effects during development. Re-arm the mount
    // guard on the second setup so a valid response is not discarded forever.
    marketStateMountedRef.current = true
    return () => {
      marketStateMountedRef.current = false
      industryPulseTimersRef.current.forEach((timer) => clearTimeout(timer))
    }
  }, [])

  useEffect(() => {
    if (mode === "aggregate") {
      return
    }
    const request: MarketStateRequest = {
      cursorDate,
      asOf,
      mode,
      windowDays: dailyWindowDays,
      timestamp: currentTimestamp,
      sessionKey: `${cursorDate}|${mode}|${dailyWindowDays}`,
    }
    latestMarketStateRequestRef.current = request
    pendingMarketStateRequestRef.current = request
    if (marketStateInFlightRef.current) return

    const drain = () => {
      const nextRequest = pendingMarketStateRequestRef.current
      if (!nextRequest) return
      pendingMarketStateRequestRef.current = null
      marketStateInFlightRef.current = true
      // Cursor movement is a refresh, not a new page load. Preserve the
      // prior snapshot so replay does not flash a skeleton every minute.
      setError(null)
      fetchTradeReviewMarketState({
      tradeDate: nextRequest.cursorDate,
      mode: nextRequest.mode as "intraday" | "daily",
      asOf: nextRequest.mode === "intraday" ? nextRequest.asOf : undefined,
      membership: "union",
      // This is the complete market workspace. The lower replay slot owns
      // the selected industry's scoped pools.
      industryCode: undefined,
      windowDays: nextRequest.windowDays,
    }).then((state) => {
      const latestRequest = latestMarketStateRequestRef.current
      if (!marketStateMountedRef.current || !latestRequest || latestRequest.sessionKey !== nextRequest.sessionKey) return
      // Never paint a snapshot from after a cursor that was dragged back.
      if (nextRequest.mode === "intraday" && nextRequest.timestamp > latestRequest.timestamp) return
      const market = state.market ?? {}
      const mappedBreadth: MarketIndicatorBreadth = {
        advancingCount: recordNumber(market, "advancingCount", "advancing_count"),
        decliningCount: recordNumber(market, "decliningCount", "declining_count"),
        flatCount: recordNumber(market, "flatCount", "flat_count"),
        limitUpCount: recordNumber(market, "sealedLimitUpCount", "sealed_limit_up_count"),
        limitUpTouchCount: recordNumber(market, "touchedLimitUpCount", "touched_limit_up_count"),
        limitDownCount: recordNumber(market, "sealedLimitDownCount", "sealed_limit_down_count"),
      }
      const ladder = jsonArray(market.ladder).map((item) => ({
        streak: recordNumber(item, "height", "height") ?? 0,
        count: recordNumber(item, "count", "count") ?? 0,
        stocks: [] as LimitEmotionStock[],
      }))
      const pools = new Map((state.pools ?? []).map((pool) => [String(pool.poolType ?? pool.pool_type), jsonArray(pool.stocks)]))
      const ladderPool = pools.get("ladder") ?? pools.get("limit_up") ?? []
      const stocksByHeight = new Map<number, LimitEmotionStock[]>()
      ladderPool.forEach((stock) => {
        const height = recordNumber(stock, "height", "height") ?? 0
        const row = {
          code: String(stock.code ?? ""), name: String(stock.name ?? stock.code ?? "-"),
          changePct: recordNumber(stock, "changePct", "change_pct"), height,
          industry: String(stock.industry ?? stock.industry_name ?? "未分类"),
          firstLimitUpAt: String(stock.firstSealAt ?? stock.first_seal_at ?? "") || undefined,
        } as LimitEmotionStock
        if (row.code) stocksByHeight.set(height, [...(stocksByHeight.get(height) ?? []), row])
      })
      const ladderWithStocks = ladder.map((row) => ({ ...row, stocks: stocksByHeight.get(row.streak) ?? [] }))
      if (!ladderWithStocks.length && ladderPool.length) {
        ladderWithStocks.push(...[...stocksByHeight.entries()].map(([streak, stocks]) => ({ streak, count: stocks.length, stocks })).sort((left, right) => right.streak - left.streak))
      }
      const limitUpStocks = (pools.get("limit_up") ?? ladderPool).map((stock) => ({
        code: String(stock.code ?? ""), name: String(stock.name ?? stock.code ?? "-"), changePct: recordNumber(stock, "changePct", "change_pct"),
      } as LimitEmotionStock))
      const limitDownStocks = (pools.get("limit_down") ?? []).map((stock) => ({
        code: String(stock.code ?? ""), name: String(stock.name ?? stock.code ?? "-"), changePct: recordNumber(stock, "changePct", "change_pct"),
      } as LimitEmotionStock))
      const brokenStocks = (pools.get("broken") ?? []).map((stock) => ({
        code: String(stock.code ?? ""), name: String(stock.name ?? stock.code ?? "-"), changePct: recordNumber(stock, "changePct", "change_pct"),
      } as LimitEmotionStock))
      const nextEmotion = {
        limitUp: { count: mappedBreadth.limitUpCount ?? 0, stocks: limitUpStocks },
        limitDown: { count: mappedBreadth.limitDownCount ?? 0, stocks: limitDownStocks },
        breakBoard: { touchedCount: mappedBreadth.limitUpTouchCount ?? 0, brokenCount: recordNumber(market, "brokenLimitUpCount", "broken_limit_up_count") ?? 0, rate: recordNumber(market, "breakRate", "break_rate"), brokenStocks },
        streak: { maxHeight: recordNumber(market, "maxHeight", "max_height") ?? 0, distribution: ladderWithStocks, promotion: { overallRate: recordNumber(market, "promotionRate", "promotion_rate"), levels: [] }, broken: { count: brokenStocks.length, highStreakBrokenCount: 0, stocks: brokenStocks }, sentiment: { level: state.trend?.phase ?? "normal", text: String(state.trend?.phase ?? "-" ) } },
      } as unknown as LimitEmotionPayload
      setEmotion(nextEmotion)
      setMarketBreadth(mappedBreadth)
      const mappedIndustries = (state.industries ?? []).map((item) => ({
        industryCode: String(item.industryCode ?? item.industry_code ?? ""), industryName: String(item.industryName ?? item.industry_name ?? "未分类"),
        stockCount: recordNumber(item, "stockCount", "stock_count"), advancingCount: recordNumber(item, "advancingCount", "advancing_count"), decliningCount: recordNumber(item, "decliningCount", "declining_count"), flatCount: recordNumber(item, "flatCount", "flat_count"),
        limitUpCount: recordNumber(item, "sealedLimitUpCount", "sealed_limit_up_count"), limitUpTouchCount: recordNumber(item, "touchedLimitUpCount", "touched_limit_up_count"),
        limitDownCount: recordNumber(item, "sealedLimitDownCount", "sealed_limit_down_count"),
        maxHeight: recordNumber(item, "maxHeight", "max_height"),
        ladder: item.ladder,
        factor: item.factor,
      }))
      const industryPulseScope = `${nextRequest.cursorDate}:${nextRequest.mode}`
      const tracker = industryLimitCountsRef.current
      const nextCounts = new Map(mappedIndustries.map((item) => [item.industryCode, item.limitUpCount ?? 0]))
      if (tracker.scope !== industryPulseScope) {
        tracker.scope = industryPulseScope
        tracker.counts = nextCounts
        setIndustryLimitPulse({})
      } else if (nextRequest.mode === "intraday") {
        const changes: Record<string, "up" | "down"> = {}
        nextCounts.forEach((count, code) => {
          const previous = tracker.counts.get(code)
          if (previous == null || previous === count) return
          changes[code] = count > previous ? "up" : "down"
        })
        tracker.counts = nextCounts
        const changedCodes = Object.keys(changes)
        if (changedCodes.length) {
          setIndustryLimitPulse((current) => ({ ...current, ...changes }))
          changedCodes.forEach((code) => {
            const existingTimer = industryPulseTimersRef.current.get(code)
            if (existingTimer) clearTimeout(existingTimer)
            industryPulseTimersRef.current.set(code, setTimeout(() => {
              setIndustryLimitPulse((current) => {
                const next = { ...current }
                delete next[code]
                return next
              })
              industryPulseTimersRef.current.delete(code)
            }, 1_450))
          })
        }
      } else {
        tracker.counts = nextCounts
      }
      setIndustries(mappedIndustries)
      setHistory((state.history ?? []).filter((item): item is Record<string, unknown> => !!item && typeof item === "object"))
      setPhase(state.trend ?? null)
      setMinute(nextRequest.mode === "intraday" ? { breadth: mappedBreadth, events: (state.events ?? []).map((event) => ({ code: String(event.code ?? ""), name: String(event.name ?? ""), eventType: String(event.eventType ?? event.event_type ?? "INDICATOR"), eventAt: String(event.eventAt ?? event.event_at ?? ""), changePct: numberValue(event.changePct ?? event.change_pct) })), industries: mappedIndustries } : null)
      if (state.dataStatus === "unavailable") setError("当前交易日市场状态尚未物化")
      setLoading(false)
    }).catch((cause) => {
      const latestRequest = latestMarketStateRequestRef.current
      if (!marketStateMountedRef.current || !latestRequest || latestRequest.sessionKey !== nextRequest.sessionKey) return
      setError(cause instanceof Error ? cause.message : "加载市场状态失败")
      setLoading(false)
    }).finally(() => {
      marketStateInFlightRef.current = false
      drain()
    })
    }
    drain()
  }, [asOf, cursorDate, currentTimestamp, dailyWindowDays, mode])

  const ladderStocks = useMemo<LadderStock[]>(() => {
    const rows = emotion?.streak?.distribution ?? []
    return rows.flatMap((row) => (row.stocks ?? []).map((stock) => ({ ...stock, height: row.streak, industry: stock.industry ?? undefined })))
      .sort((a, b) => (b.height - a.height) || ((b.changePct ?? 0) - (a.changePct ?? 0)))
  }, [emotion])

  const continuingStockCount = useMemo(
    () => (emotion?.streak?.distribution ?? []).reduce(
      (sum, row) => sum + (row.streak >= 2 ? (row.stocks?.length || row.count || 0) : 0),
      0,
    ),
    [emotion],
  )

  useEffect(() => {
    let cancelled = false
    const targets = ladderStocks
    targets.forEach((stock) => {
      if (timeshare[stock.code] || timeshareLoading[stock.code]) return
      setTimeshareLoading((current) => ({ ...current, [stock.code]: true }))
      void fetchEasyTdxTimeshare({ code: stock.code, tradeDate: cursorDate })
        .then((response) => {
          if (cancelled) return
          setTimeshare((current) => ({ ...current, [stock.code]: parseTimeshare(response.items, null, cursorDate) }))
        })
        .catch(() => {
          if (!cancelled) setTimeshare((current) => ({ ...current, [stock.code]: [] }))
        })
        .finally(() => {
          if (!cancelled) setTimeshareLoading((current) => ({ ...current, [stock.code]: false }))
        })
    })
    return () => { cancelled = true }
  }, [cursorDate, ladderStocks, timeshare, timeshareLoading])

  const visibleBreadth = mode === "intraday" ? (minute?.breadth ?? marketBreadth) : marketBreadth
  const limitUp = emotion?.limitUp?.count ?? visibleBreadth?.limitUpCount ?? null
  const limitDown = emotion?.limitDown?.count ?? visibleBreadth?.limitDownCount ?? null
  const minuteBroken = minute?.events.filter((event) => /break|broken|炸板/i.test(event.eventType)).length ?? 0
  const touched = mode === "intraday"
    ? (visibleBreadth?.limitUpTouchCount ?? emotion?.breakBoard?.touchedCount ?? null)
    : (emotion?.breakBoard?.touchedCount ?? visibleBreadth?.limitUpTouchCount ?? null)
  const broken = mode === "intraday"
    ? (minute?.events.length ? minuteBroken : emotion?.breakBoard?.brokenCount ?? null)
    : (emotion?.breakBoard?.brokenCount ?? null)
  const breakRate = mode === "intraday"
    ? (touched && broken != null ? broken / touched * 100 : null)
    : (emotion?.breakBoard?.rate ?? (touched && broken != null ? broken / touched * 100 : null))
  const promoted = mode === "intraday"
    ? (minute?.events.filter((event) => /new|first|sealed|limit_up_sealed/i.test(event.eventType)).length || null)
    : (emotion?.streak?.promotion?.levels.reduce((sum, row) => sum + row.todayPromotedCount, 0) ?? null)
  const promotionRate = mode === "intraday" ? null : (emotion?.streak?.promotion?.overallRate ?? null)
  const minuteChanges = minute?.events.map((event) => event.changePct).filter((value): value is number => value != null && Number.isFinite(value)).sort((a, b) => a - b) ?? []
  const minuteMedian = minuteChanges.length ? minuteChanges[Math.floor(minuteChanges.length / 2)] : null
  const median = mode === "intraday" ? minuteMedian : (emotion?.medianChangePct ?? null)
  const marketIndustries = mode === "intraday" && minute?.industries.length ? minute.industries : industries
  const industryPool = useMemo(() => {
    const unique = new Map<string, MarketIndicatorIndustry>()
    marketIndustries
      .filter((item) => item.firstLimitUpCode || item.firstLimitUpName || (item.limitUpCount ?? 0) > 0)
      .forEach((item) => {
        const current = unique.get(item.industryName)
        if (!current || (item.limitUpCount ?? 0) > (current.limitUpCount ?? 0)) unique.set(item.industryName, item)
      })
    return [...unique.values()]
      .sort((left, right) => {
        const leftLadder = ladderCounts((left as unknown as Record<string, unknown>).ladder).continuing
        const rightLadder = ladderCounts((right as unknown as Record<string, unknown>).ladder).continuing
        return rightLadder - leftLadder || (right.limitUpCount ?? 0) - (left.limitUpCount ?? 0)
      })
      .slice(0, 12)
  }, [marketIndustries])

  if (mode === "aggregate") {
    return <div className="flex min-h-40 items-center justify-center rounded-lg bg-muted/25 px-5 text-center text-xs text-muted-foreground">周K、月K仅展示价格与聚合摘要；市场情绪池请切换至日K逐日回放。</div>
  }

  return (
    <div className="flex h-full min-h-0 flex-col gap-3 overflow-auto pr-1">
      {loading && !emotion ? (
        <div className="grid grid-cols-2 gap-2 md:grid-cols-4"><Skeleton className="h-16" /><Skeleton className="h-16" /><Skeleton className="h-16" /><Skeleton className="h-16" /></div>
      ) : error ? (
        <div className="flex items-center justify-between rounded-lg bg-muted/40 px-3 py-3 text-xs text-muted-foreground"><span>{error}</span><RefreshCw className="size-3.5" /></div>
      ) : (
        <>
          <div className="grid grid-cols-2 gap-2 md:grid-cols-4">
            <Metric label="连板最高" value={emotion?.streak?.maxHeight != null ? `${emotion.streak.maxHeight} 板` : "-"} />
            <Metric label="炸板率" value={pct(breakRate)} tone={breakRate != null && breakRate > 30 ? "warn" : undefined} />
            <Metric label="晋级率" value={pct(promotionRate)} tone="up" />
            <Metric label="新晋板数" value={promoted == null ? "-" : String(promoted)} tone="up" />
            <Metric label="涨停 / 跌停" value={`${limitUp ?? "-"} / ${limitDown ?? "-"}`} tone="up" />
            <Metric label="触板 / 炸板" value={`${touched ?? "-"} / ${broken ?? "-"}`} tone="warn" />
            <Metric label="涨 / 跌" value={`${visibleBreadth?.advancingCount ?? "-"} / ${visibleBreadth?.decliningCount ?? "-"}`} />
            <Metric label="涨跌中位数" value={signed(median)} tone={median != null && median >= 0 ? "up" : "down"} />
          </div>

          {mode === "daily" ? (
            <div className="flex items-center gap-1 text-[10px] text-muted-foreground">
              <span>区间：</span>
              {[20, 60, 120].map((days) => <button key={days} type="button" className={cn("rounded px-2 py-1", dailyWindowDays === days ? "bg-foreground text-background" : "bg-muted hover:bg-muted/80")} onClick={() => onDailyWindowDaysChange(days)}>{days}日</button>)}
              <span className="ml-1">历史梯度与生态按复盘日截断</span>
            </div>
          ) : null}

          <Card>
            <Heading icon={Layers3} title="涨停连板梯度" detail={`涨停 ${ladderStocks.length} · 连板 ${continuingStockCount}`} />
            <p className="mt-1 text-[10px] text-muted-foreground">按连板高度展开；“连板”仅统计 2 板及以上，首板单列展示。</p>
            <div className="mt-2 space-y-2.5">
              {(emotion?.streak?.distribution ?? []).map((row) => {
                const stocks = row.stocks ?? []
                const isFirstBoard = row.streak === 1
                const hiddenCount = Math.max(0, stocks.length - 18)
                const visibleStocks = isFirstBoard && !firstBoardExpanded ? stocks.slice(0, 18) : stocks
                return (
                  <div key={row.streak} className="grid grid-cols-[52px_minmax(0,1fr)] items-stretch gap-2">
                    <div className={cn("flex flex-col justify-center border-l-2 pl-2", row.streak >= 2 ? "border-rose-500" : "border-muted-foreground/30")}>
                      <span className={cn("font-mono text-sm font-semibold tabular-nums", row.streak >= 2 ? "text-rose-600" : "text-foreground")}>{row.streak}板</span>
                      <span className="mt-0.5 font-mono text-[9px] text-muted-foreground">{row.count || stocks.length || 0}只</span>
                    </div>
                    <div className="min-w-0">
                      <div className="grid grid-cols-[repeat(auto-fit,minmax(148px,1fr))] gap-1.5">
                        {visibleStocks.map((stock) => <div key={stock.code} className="min-w-0 bg-background/80 px-2 py-1.5"><div className="flex min-w-0 items-center justify-between gap-2"><span className="truncate text-[11px] font-medium">{stock.name}</span><span className={cn("shrink-0 font-mono text-[10px] tabular-nums", (stock.changePct ?? 0) >= 0 ? "text-rose-600" : "text-emerald-600")}>{signed(stock.changePct)}</span></div><div className="mt-1 flex min-w-0 items-center justify-between gap-1 text-[9px] text-muted-foreground"><span className="truncate">{(stock as LadderStock).industry ?? "未分类"}</span><span className="shrink-0 font-mono">{stock.code}</span></div></div>)}
                        {!stocks.length ? <span className="text-[10px] text-muted-foreground">暂无标的</span> : null}
                      </div>
                      {isFirstBoard && hiddenCount > 0 ? <button type="button" className="mt-1.5 flex items-center gap-1 text-[10px] text-muted-foreground hover:text-foreground" onClick={() => setExpandedFirstBoardScope((current) => current === firstBoardScope ? null : firstBoardScope)}>{firstBoardExpanded ? <><ChevronUp className="size-3" />收起首板</> : <><ChevronDown className="size-3" />展开剩余 {hiddenCount} 只</>}</button> : null}
                    </div>
                  </div>
                )
              })}
              {!emotion?.streak?.distribution?.length ? <div className="py-4 text-center text-xs text-muted-foreground">暂无连板梯度</div> : null}
            </div>
          </Card>

          <Card>
            <Heading icon={TrendingUp} title="新晋板行业池" detail={`${industryPool.length} 个行业`} />
            <p className="mt-1 text-[10px] text-muted-foreground">优先展示含 2 板以上标的的行业；连板数不包含首板。</p>
            <div className="mt-2 grid grid-cols-[repeat(auto-fit,minmax(148px,1fr))] gap-1.5">
              {industryPool.map((item) => {
                const row = item as unknown as Record<string, unknown>
                const { total, continuing } = ladderCounts(row.ladder)
                const firstBoard = Math.max(0, total - continuing)
                const summary = ladderSummary(row.ladder)
                const pulse = item.industryCode ? industryLimitPulse[item.industryCode] : undefined
                return <article key={`${item.industryCode}-${item.industryName}`} className={cn("min-w-0 rounded-md bg-background/75 px-2 py-1.5 transition-colors hover:bg-muted/55", pulse === "up" && "trade-review-industry-limit-up", pulse === "down" && "trade-review-industry-limit-down")}>
                  <div className="flex min-w-0 items-center gap-1.5 border-l-2 border-rose-500/70 pl-1.5">
                    <span className="min-w-0 flex-1 truncate text-[11px] font-semibold">{item.industryName}</span>
                    <span className="shrink-0 rounded-sm bg-rose-500/10 px-1.5 py-0.5 font-mono text-[9px] font-semibold text-rose-600" title={summary}>{summary || "暂无梯度"}</span>
                  </div>
                  <div className="mt-1.5 grid grid-cols-3 font-mono text-[9px] tabular-nums">
                    <div className="flex items-baseline justify-between pr-1.5"><span className="text-muted-foreground">涨停</span><strong className="text-[12px] text-rose-600">{item.limitUpCount ?? total}</strong></div>
                    <div className="flex items-baseline justify-between border-x border-border/60 px-1.5"><span className="text-muted-foreground">连板</span><strong className={cn("text-[12px]", continuing > 0 ? "text-rose-600" : "text-muted-foreground")}>{continuing}</strong></div>
                    <div className="flex items-baseline justify-between pl-1.5"><span className="text-muted-foreground">首板</span><strong className="text-[12px] text-foreground">{firstBoard}</strong></div>
                  </div>
                </article>
              })}
              {!industryPool.length ? <div className="py-4 text-center text-xs text-muted-foreground">暂无新晋板行业池</div> : null}
            </div>
          </Card>

          <Card>
            <Heading icon={Activity} title="连板标的分时" detail={mode === "intraday" ? "随回放游标更新市场指标" : "复盘日分时快照"} />
            <div className="mt-2 grid grid-cols-1 gap-2 md:grid-cols-2">
              {ladderStocks.map((stock) => (
                <div key={stock.code} className="grid min-w-0 grid-cols-[minmax(0,92px)_1fr] gap-2 rounded-md bg-background/70 p-2">
                  <div className="min-w-0">
                    <div className="flex items-center gap-1"><span className="truncate text-[11px] font-semibold">{stock.name}</span><span className="shrink-0 font-mono text-[9px] text-rose-600">{stock.height}板</span></div>
                    <div className="mt-1 truncate font-mono text-[9px] text-muted-foreground">{stock.code}</div>
                    <div className={cn("mt-2 font-mono text-sm font-semibold", (stock.changePct ?? 0) >= 0 ? "text-rose-600" : "text-emerald-600")}>{signed(stock.changePct)}</div>
                    <div className="mt-1 text-[9px] text-muted-foreground">{stock.industry ?? "行业未知"}</div>
                  </div>
                  <div className="min-w-0">
                    {timeshareLoading[stock.code] ? <Skeleton className="h-16 w-full" /> : <MiniTimeshare points={timeshare[stock.code] ?? []} name={stock.name} currentTimestamp={mode === "intraday" ? currentTimestamp : 0} />}
                  </div>
                </div>
              ))}
              {!ladderStocks.length ? <div className="col-span-full py-6 text-center text-xs text-muted-foreground">当前复盘日暂无连板标的</div> : null}
            </div>
          </Card>

          {mode === "daily" ? (
            <Card>
              <Heading icon={Activity} title="近期开奖阶段" detail={`${phase?.phase ?? "-"} · ${dailyWindowDays}日窗口`} />
              <div className="mt-2 grid grid-cols-1 gap-1.5 md:grid-cols-2">
                {history.slice().reverse().slice(0, 10).map((row) => {
                  const date = String(row.tradeDate ?? row.trade_date ?? "")
                  const up = recordNumber(row, "sealedLimitUpCount", "sealed_limit_up_count") ?? 0
                  const down = recordNumber(row, "sealedLimitDownCount", "sealed_limit_down_count") ?? 0
                  const height = recordNumber(row, "maxHeight", "max_height") ?? 0
                  return <button key={date} type="button" onClick={() => date && onStartMinuteReplay?.(date)} className="grid grid-cols-[76px_1fr_auto] items-center gap-2 rounded bg-background/70 px-2 py-1.5 text-left text-[10px] hover:bg-muted" title="启动该交易日分钟回放"><span className="font-mono text-muted-foreground">{date}</span><span className="truncate">涨停 {up} · 跌停 {down} · {height}板</span><span className="text-primary">回放</span></button>
                })}
                {!history.length ? <div className="py-3 text-center text-xs text-muted-foreground">暂无已物化的日级历史</div> : null}
              </div>
            </Card>
          ) : null}

          <Card>
            <Heading icon={mode === "intraday" ? TrendingUp : TrendingDown} title={mode === "intraday" ? "分钟回放联动" : "日级生态摘要"} detail={mode === "intraday" ? `截至 ${asOf.slice(11, 16)}` : cursorDate} />
            <Breadth value={visibleBreadth} />
            <div className="mt-2 grid grid-cols-2 gap-2 text-[10px] md:grid-cols-4">
              <span>涨停 {limitUp ?? "-"}</span><span>跌停 {limitDown ?? "-"}</span><span>炸板率 {pct(breakRate)}</span><span>指标事件 {minute?.events.length ?? realEventCount}</span>
            </div>
          </Card>
        </>
      )}
    </div>
  )
}
