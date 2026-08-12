import { useEffect, useMemo, useRef, useState } from "react"
import { Activity, ArrowDownRight, ArrowUpRight, Clock3, Flame, Layers3, Play } from "lucide-react"

import { cn } from "@/lib/utils"
import { notification } from "@/components/ui/notification"
import { fetchMarketPulseHistory, fetchMarketPulseLimitEmotion, type LimitEmotionPayload, type MarketHistoryPoint } from "@/services/market/pulse"
import { fetchMarketSentimentLimitEmotionSummaryHistory, type LimitEmotionSummaryHistoryItem } from "@/services/market/sentiment"
import {
  buildIndustryMarketStateMock,
  buildPoolIntradayMock,
  type MockIndustryDailyState,
  type MockIndustryIntradaySnapshot,
  type MockLadderLevel,
  type MockPoolStock,
  type RotationPhase,
} from "../lib/market-state-mock"

interface MarketStateReplayPanelProps {
  tradeDate: string
  currentTimestamp: number
  industryName?: string | null
  mode: "intraday" | "daily"
  dailyWindowDays: number
  onDailyWindowDaysChange: (days: number) => void
  onStartMinuteReplay: (tradeDate: string) => void
  realEventCount?: number
}

function signed(value: number, digits = 2) {
  return `${value > 0 ? "+" : ""}${value.toFixed(digits)}%`
}

function amountWan(value: number) {
  const absolute = Math.abs(value)
  const sign = value < 0 ? "-" : "+"
  if (absolute >= 10_000) return `${sign}${(absolute / 10_000).toFixed(2)} 亿`
  return `${sign}${Math.round(absolute).toLocaleString("zh-CN")} 万`
}

function MiniLine({
  values,
  visibleCount = values.length,
  tone = "up",
}: {
  values: number[]
  visibleCount?: number
  tone?: "up" | "information"
}) {
  const width = 320
  const height = 64
  const min = Math.min(...values, 0)
  const max = Math.max(...values, 1)
  const range = Math.max(max - min, 0.0001)
  const points = values
    .slice(0, Math.max(1, visibleCount))
    .map((value, index) => {
      const x = values.length <= 1 ? 0 : (index / (values.length - 1)) * width
      const y = height - ((value - min) / range) * (height - 8) - 4
      return `${x.toFixed(1)},${y.toFixed(1)}`
    })
    .join(" ")
  const area = points ? `0,${height} ${points} ${(Math.max(0, visibleCount - 1) / Math.max(1, values.length - 1)) * width},${height}` : ""

  return (
    <svg
      viewBox={`0 0 ${width} ${height}`}
      preserveAspectRatio="none"
      className={cn("h-full w-full", tone === "up" ? "text-rose-600" : "text-information")}
      aria-hidden="true"
    >
      <line x1="0" x2={width} y1={height / 2} y2={height / 2} stroke="currentColor" strokeOpacity="0.12" strokeDasharray="3 4" />
      {area ? <polygon points={area} fill="currentColor" opacity="0.07" /> : null}
      {points ? <polyline points={points} fill="none" stroke="currentColor" strokeWidth="2" strokeLinejoin="round" strokeLinecap="round" vectorEffect="non-scaling-stroke" /> : null}
    </svg>
  )
}

function Metric({ label, value, tone }: { label: string; value: string; tone?: "up" | "down" | "warning" }) {
  return (
    <div className="min-w-0 bg-background/75 px-2 py-1.5">
      <div className="truncate text-[9px] text-muted-foreground">{label}</div>
      <div className={cn("mt-0.5 truncate font-mono text-[11px] font-medium tabular-nums", tone === "up" && "text-rose-600", tone === "down" && "text-emerald-600", tone === "warning" && "text-amber-600")}>{value}</div>
    </div>
  )
}

function PanelHeading({ icon: Icon, title, detail }: { icon: typeof Activity; title: string; detail: string }) {
  return (
    <div className="flex min-w-0 items-center justify-between gap-2">
      <div className="flex min-w-0 items-center gap-1.5">
        <Icon className="size-3.5 shrink-0 text-muted-foreground" aria-hidden="true" />
        <span className="truncate text-[11px] font-semibold">{title}</span>
      </div>
      <span className="shrink-0 text-[9px] text-muted-foreground">{detail}</span>
    </div>
  )
}

function BreadthBar({ snapshot }: { snapshot: Pick<MockIndustryIntradaySnapshot, "advancingCount" | "decliningCount" | "flatCount"> }) {
  const total = Math.max(1, snapshot.advancingCount + snapshot.decliningCount + snapshot.flatCount)
  return (
    <div>
      <div className="flex h-2 overflow-hidden rounded-full bg-muted">
        <div className="bg-rose-500/75" style={{ width: `${(snapshot.advancingCount / total) * 100}%` }} />
        <div className="bg-muted-foreground/25" style={{ width: `${(snapshot.flatCount / total) * 100}%` }} />
        <div className="bg-emerald-500/75" style={{ width: `${(snapshot.decliningCount / total) * 100}%` }} />
      </div>
      <div className="mt-1 flex items-center justify-between font-mono text-[9px] tabular-nums">
        <span className="text-rose-600">上涨 {snapshot.advancingCount}</span>
        <span className="text-muted-foreground">平 {snapshot.flatCount}</span>
        <span className="text-emerald-600">下跌 {snapshot.decliningCount}</span>
      </div>
    </div>
  )
}

function IntradayView({
  data,
  current,
  visibleCount,
  compact,
  spacious,
}: {
  data: ReturnType<typeof buildIndustryMarketStateMock>
  current: MockIndustryIntradaySnapshot
  visibleCount: number
  compact: boolean
  spacious: boolean
}) {
  return (
    <div className={cn("grid h-full min-h-0 gap-2", compact ? "grid-cols-2" : "grid-cols-12")}>
      <section className={cn("min-h-0 overflow-hidden rounded-md bg-muted/30 p-2", compact ? "col-span-1" : "col-span-5")}>
        <PanelHeading icon={Activity} title="行业轮动" detail={`${current.timeLabel} · ${current.phase}`} />
        <div className={cn("mt-1.5 grid gap-2", compact ? "grid-cols-[1fr_74px]" : "grid-cols-[minmax(0,1fr)_102px]")}>
          <div className={cn("min-h-0", spacious ? "h-20" : "h-14")}>
            <MiniLine values={data.intraday.map((item) => item.strengthScore)} visibleCount={visibleCount} tone="information" />
          </div>
          <div className="grid content-start gap-1">
            <Metric label="强度" value={current.strengthScore.toFixed(1)} />
            <Metric label="全市场排名" value={`${current.rank} / 56`} />
            {!compact ? <Metric label="相对大盘" value={signed(current.relativeChangePct)} tone={current.relativeChangePct >= 0 ? "up" : "down"} /> : null}
          </div>
        </div>
      </section>

      <section className={cn("min-h-0 overflow-hidden rounded-md bg-muted/30 p-2", compact ? "col-span-1" : "col-span-4")}>
        <PanelHeading icon={Flame} title="广度与封板" detail={`并集 ${data.coverage.unionCount} 只`} />
        <div className="mt-2"><BreadthBar snapshot={current} /></div>
        <div className={cn("mt-2 grid gap-1", compact ? "grid-cols-3" : "grid-cols-3")}>
          <Metric label="涨停 / 触板" value={`${current.limitUpCount} / ${current.touchedLimitUpCount}`} tone="up" />
          <Metric label="炸板" value={String(current.brokenLimitUpCount)} tone={current.brokenLimitUpCount >= 4 ? "warning" : undefined} />
          <Metric label="封板率" value={`${current.sealRate.toFixed(1)}%`} />
          {!compact ? <Metric label="中位涨幅" value={signed(current.medianChangePct)} tone={current.medianChangePct >= 0 ? "up" : "down"} /> : null}
          {!compact ? <Metric label="涨幅 >5%" value={`${current.above5PctCount} 只`} /> : null}
          {!compact ? <Metric label="跌停" value={`${current.limitDownCount} 只`} tone={current.limitDownCount ? "down" : undefined} /> : null}
        </div>
      </section>

      {!compact ? (
        <section className="col-span-3 min-h-0 overflow-hidden rounded-md bg-muted/30 p-2">
          <PanelHeading icon={Layers3} title="连板梯队" detail={`晋级 ${current.promotionRate.toFixed(1)}%`} />
          <div className="mt-2 flex items-end justify-between gap-2">
            <div><div className="font-mono text-xl font-semibold tabular-nums">{current.maxHeight}<span className="ml-0.5 text-[10px] font-normal text-muted-foreground">板</span></div><div className="text-[9px] text-muted-foreground">当前最高高度</div></div>
            <div className="text-right text-[9px] text-muted-foreground">盘中口径<br />收盘后确认断板</div>
          </div>
          <div className="mt-2 space-y-1">
            {current.ladder.slice(0, spacious ? 5 : 4).map((level) => (
              <div key={level.height} className="grid grid-cols-[26px_1fr_auto] items-center gap-1 text-[9px]">
                <span className="font-mono text-foreground">{level.height}板</span>
                <div className="h-1.5 overflow-hidden rounded-full bg-background"><div className="h-full rounded-full bg-rose-500/65" style={{ width: `${Math.min(100, level.count * 18)}%` }} /></div>
                <span className="font-mono text-muted-foreground">{level.count}</span>
                {spacious ? <span className="col-start-2 col-end-4 truncate text-[8px] text-muted-foreground">{level.leaders.join("、")}</span> : null}
              </div>
            ))}
          </div>
        </section>
      ) : null}
    </div>
  )
}

function FlowBars({ rows }: { rows: MockIndustryDailyState[] }) {
  const max = Math.max(...rows.map((row) => Math.abs(row.mainNetAmountWan)), 1)
  return (
    <div className="flex h-12 items-center gap-1 border-y border-border/50 py-1">
      {rows.map((row) => {
        const height = Math.max(8, Math.abs(row.mainNetAmountWan) / max * 100)
        return <div key={row.tradeDate} className="flex h-full min-w-0 flex-1 items-center"><div className={cn("w-full rounded-sm", row.mainNetAmountWan >= 0 ? "bg-rose-500/65" : "bg-emerald-500/65")} style={{ height: `${height}%` }} title={`${row.tradeDate} · ${amountWan(row.mainNetAmountWan)}`} /></div>
      })}
    </div>
  )
}

function PoolIntradayMiniChart({ stock, tradeDate }: { stock: MockPoolStock; tradeDate: string }) {
  const points = useMemo(() => buildPoolIntradayMock(tradeDate, stock), [stock, tradeDate])
  const width = 132
  const height = 38
  const min = Math.min(...points.map((point) => point.changePct), 0)
  const max = Math.max(...points.map((point) => point.changePct), 0)
  const span = Math.max(max - min, 0.5)
  const domainMin = min - span * 0.08
  const domainMax = max + span * 0.08
  const volumeMax = Math.max(...points.map((point) => point.volume), 1)
  const x = (index: number) => index / Math.max(1, points.length - 1) * width
  const y = (value: number) => 2 + (domainMax - value) / Math.max(0.001, domainMax - domainMin) * (height * 0.7 - 3)
  const zeroY = y(0)
  const averagePath = points.map((point, index) => `${index ? "L" : "M"}${x(index).toFixed(1)},${y(point.averagePct).toFixed(1)}`).join(" ")
  const latest = points.at(-1)!

  return (
    <div className="min-w-0">
      <svg viewBox={`0 0 ${width} ${height}`} preserveAspectRatio="none" className="h-9 w-full" aria-label={`${stock.name} ${tradeDate} 分时`}>
        <line x1="0" x2={width} y1={zeroY} y2={zeroY} stroke="currentColor" strokeOpacity="0.16" strokeDasharray="2 2" />
        {points.map((point, index) => {
          const previous = points[index - 1]?.changePct ?? 0
          const barHeight = Math.max(1, point.volume / volumeMax * height * 0.2)
          return <rect key={`${point.timestamp}-v`} x={x(index)} y={height - barHeight} width={Math.max(0.45, width / points.length - 0.15)} height={barHeight} fill={point.changePct >= previous ? "#dc2626" : "#16a34a"} fillOpacity="0.45" />
        })}
        <path d={averagePath} fill="none" stroke="#d97706" strokeWidth="0.8" strokeDasharray="2 2" vectorEffect="non-scaling-stroke" />
        {points.slice(1).map((point, index) => {
          const previous = points[index]
          return <line key={`${point.timestamp}-p`} x1={x(index)} y1={y(previous.changePct)} x2={x(index + 1)} y2={y(point.changePct)} stroke={point.changePct >= 0 ? "#dc2626" : "#16a34a"} strokeWidth="1.1" vectorEffect="non-scaling-stroke" />
        })}
      </svg>
      <div className="flex items-center justify-between gap-1 font-mono text-[8px] tabular-nums">
        <span className={latest.changePct >= 0 ? "text-rose-600" : "text-emerald-600"}>{signed(latest.changePct)}</span>
        <span className="text-muted-foreground">今日分时</span>
      </div>
    </div>
  )
}

function PoolStockCard({
  stock,
  tradeDate,
  context,
}: {
  stock: MockPoolStock
  tradeDate: string
  context: "ladder" | "limit-up" | "limit-down"
}) {
  const contextLabel = context === "ladder" ? `${stock.height}板` : context === "limit-up" ? "涨停" : "跌停"
  return (
    <div className="min-w-0 rounded bg-background/75 px-1.5 py-1">
      <div className="flex min-w-0 items-center justify-between gap-1">
        <span className="truncate text-[9px] font-medium">{stock.name}</span>
        <span className={cn("shrink-0 font-mono text-[8px]", context === "limit-down" ? "text-emerald-600" : "text-rose-600")}>{contextLabel}</span>
      </div>
      <div className="mt-0.5 flex min-w-0 items-center justify-between gap-1 font-mono text-[7px] text-muted-foreground">
        <span className="truncate">{stock.code} · {context === "limit-down" ? "跌停" : "首封"} {stock.firstLimitTime}</span>
        <span className={context === "limit-down" ? "text-emerald-600" : "text-rose-600"}>{signed(stock.changePct)}</span>
      </div>
      <div className="mt-0.5 h-8"><PoolIntradayMiniChart stock={stock} tradeDate={tradeDate} /></div>
    </div>
  )
}

void PoolIntradayMiniChart

function average(rows: MockIndustryDailyState[], select: (row: MockIndustryDailyState) => number) {
  if (!rows.length) return 0
  return rows.reduce((sum, row) => sum + select(row), 0) / rows.length
}

function marketDate(timestamp: number, fallback: string) {
  if (!timestamp) return fallback
  return new Intl.DateTimeFormat("en-CA", {
    timeZone: "Asia/Shanghai",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).format(new Date(timestamp))
}

function DailyView({
  data,
  compact,
  spacious,
  currentTimestamp,
  tradeDate,
  rangeDays,
  onRangeDaysChange,
}: {
  data: ReturnType<typeof buildIndustryMarketStateMock>
  compact: boolean
  spacious: boolean
  currentTimestamp: number
  tradeDate: string
  rangeDays: number
  onRangeDaysChange: (days: number) => void
  onStartMinuteReplay: (tradeDate: string) => void
}) {
  const [customDays, setCustomDays] = useState(String(rangeDays))
  const cursorDate = marketDate(currentTimestamp, tradeDate)
  const availableRows = data.daily.filter((row) => row.tradeDate <= cursorDate)
  const available = availableRows.length ? availableRows : data.daily.slice(0, 1)
  const selected = available.slice(-rangeDays)
  const recent = available.slice(-5)
  const latest = available.at(-1)!
  const rangeLimitUp = selected.reduce((sum, row) => sum + row.limitUpCount, 0)
  const rangeTouched = selected.reduce((sum, row) => sum + row.touchedLimitUpCount, 0)
  const rangeBroken = selected.reduce((sum, row) => sum + row.brokenLimitUpCount, 0)
  const rangeFailed = selected.reduce((sum, row) => sum + row.failedPromotionCount, 0)
  const rangePeakHeight = Math.max(...selected.map((row) => row.maxHeight), 0)
  const recentPeakHeight = Math.max(...recent.map((row) => row.maxHeight), 0)
  const fiveDayFlow = recent.reduce((sum, item) => sum + item.mainNetAmountWan, 0)
  const applyCustomDays = () => {
    const parsed = Number.parseInt(customDays, 10)
    const next = Number.isFinite(parsed) ? Math.max(5, Math.min(240, parsed)) : 60
    setCustomDays(String(next))
    onRangeDaysChange(next)
  }
  const rangeControls = (
    <div className="flex items-center rounded bg-background/80 p-0.5">
      {([20, 60, 120] as const).map((value) => (
        <button
          key={value}
          type="button"
          className={cn("rounded px-1 py-0.5 font-mono text-[8px] tabular-nums", rangeDays === value ? "bg-muted text-foreground" : "text-muted-foreground hover:text-foreground")}
          onClick={() => {
            setCustomDays(String(value))
            onRangeDaysChange(value)
          }}
        >
          {value}日
        </button>
      ))}
      <span className="mx-0.5 h-3 w-px bg-border" />
      <input
        type="number"
        min={5}
        max={240}
        value={customDays}
        onChange={(event) => setCustomDays(event.target.value)}
        onBlur={applyCustomDays}
        onKeyDown={(event) => {
          if (event.key === "Enter") event.currentTarget.blur()
        }}
        className="h-4 w-10 rounded bg-muted/50 px-1 text-right font-mono text-[8px] tabular-nums outline-none focus-visible:ring-1 focus-visible:ring-ring/50"
        aria-label="自定义日级回放交易日数量"
        title="输入 5 至 240 个交易日"
      />
      <span className="pr-0.5 text-[8px] text-muted-foreground">日</span>
    </div>
  )

  if (compact) {
    return (
      <div className="grid h-full min-h-0 grid-cols-2 gap-2">
        <section className="min-h-0 overflow-hidden rounded-md bg-muted/30 p-2">
          <PanelHeading icon={Flame} title="当日广度与封板" detail={latest.tradeDate.slice(5)} />
          <div className="mt-1.5"><BreadthBar snapshot={latest} /></div>
          <div className="mt-1.5 grid grid-cols-3 gap-1">
            <Metric label="涨停封板" value={String(latest.limitUpCount)} tone="up" />
            <Metric label="触及涨停" value={String(latest.touchedLimitUpCount)} tone="up" />
            <Metric label="炸板" value={String(latest.brokenLimitUpCount)} tone="warning" />
            <Metric label="跌停" value={String(latest.limitDownCount)} tone={latest.limitDownCount ? "down" : undefined} />
            <Metric label="封板率" value={`${(latest.touchedLimitUpCount ? latest.limitUpCount / latest.touchedLimitUpCount * 100 : 0).toFixed(1)}%`} />
            <Metric label="涨幅 >5%" value={String(latest.above5PctCount)} />
          </div>
        </section>
        <section className="min-h-0 overflow-hidden rounded-md bg-muted/30 p-2">
          <div className="flex items-center justify-between gap-2"><PanelHeading icon={Layers3} title="区间与近期" detail={`截至 ${latest.tradeDate.slice(5)}`} />{rangeControls}</div>
          <div className="mt-1.5 grid grid-cols-3 gap-1">
            <Metric label="区间最高" value={`${rangePeakHeight} 板`} />
            <Metric label="区间炸板" value={String(rangeBroken)} tone="warning" />
            <Metric label="区间断板" value={String(rangeFailed)} tone="warning" />
            <Metric label="近期最高" value={`${recentPeakHeight} 板`} />
            <Metric label="近期平均广度" value={`${average(recent, (row) => row.advancingPct).toFixed(1)}%`} />
            <Metric label="近期晋级率" value={`${average(recent, (row) => row.promotionRate).toFixed(1)}%`} />
          </div>
        </section>
      </div>
    )
  }

  return (
    <div className="grid h-full min-h-0 grid-cols-12 gap-2">
      <section className="col-span-4 min-h-0 overflow-hidden rounded-md bg-muted/30 p-2">
        <PanelHeading icon={Flame} title="当日广度与封板生态" detail={`${latest.tradeDate.slice(5)} 收盘`} />
        <div className="mt-1.5"><BreadthBar snapshot={latest} /></div>
        <div className="mt-1.5 grid grid-cols-3 gap-1">
          <Metric label="涨停封板" value={String(latest.limitUpCount)} tone="up" />
          <Metric label="触及涨停" value={String(latest.touchedLimitUpCount)} tone="up" />
          <Metric label="炸板" value={String(latest.brokenLimitUpCount)} tone="warning" />
          <Metric label="跌停" value={String(latest.limitDownCount)} tone={latest.limitDownCount ? "down" : undefined} />
          <Metric label="封板率" value={`${(latest.touchedLimitUpCount ? latest.limitUpCount / latest.touchedLimitUpCount * 100 : 0).toFixed(1)}%`} />
          <Metric label="涨幅 >5%" value={`${latest.above5PctCount} 只`} />
          <Metric label="中位涨幅" value={signed(latest.medianChangePct)} tone={latest.medianChangePct >= 0 ? "up" : "down"} />
          <Metric label="最高连板" value={`${latest.maxHeight} 板`} />
          <Metric label="当日晋级率" value={`${latest.promotionRate.toFixed(1)}%`} />
        </div>
      </section>

      <section className="col-span-4 min-h-0 overflow-hidden rounded-md bg-muted/30 p-2">
        <div className="flex items-center justify-between gap-2">
          <PanelHeading icon={Activity} title="所选区间统计" detail={`${selected.length}日 · ${selected[0]?.tradeDate.slice(5)} — ${latest.tradeDate.slice(5)}`} />
          {rangeControls}
        </div>
        <div className={cn("mt-1", spacious ? "h-12" : "h-9")}><MiniLine values={selected.map((item) => item.strengthScore)} tone="information" /></div>
        <div className="grid grid-cols-4 gap-1">
          <Metric label="区间最高" value={`${rangePeakHeight} 板`} />
          <Metric label="涨停合计" value={String(rangeLimitUp)} tone="up" />
          <Metric label="触板合计" value={String(rangeTouched)} />
          <Metric label="炸板合计" value={String(rangeBroken)} tone="warning" />
          <Metric label="断板合计" value={String(rangeFailed)} tone="warning" />
          <Metric label="平均广度" value={`${average(selected, (row) => row.advancingPct).toFixed(1)}%`} />
          <Metric label="平均晋级" value={`${average(selected, (row) => row.promotionRate).toFixed(1)}%`} />
          <Metric label="5日资金" value={amountWan(fiveDayFlow)} tone={fiveDayFlow >= 0 ? "up" : "down"} />
        </div>
        {spacious ? <div className="mt-1"><FlowBars rows={selected.slice(-10)} /></div> : null}
      </section>

      <section className="col-span-4 min-h-0 overflow-hidden rounded-md bg-muted/30 p-2">
        <PanelHeading icon={Layers3} title="连板梯度、断板与近期" detail="最新 / 近期5日" />
        <div className="mt-1.5 grid grid-cols-[minmax(0,1fr)_104px] gap-2">
          <div className="space-y-1">
            {latest.ladder.slice(0, spacious ? 5 : 4).map((level) => (
              <div key={level.height} className="grid grid-cols-[26px_1fr_auto] items-center gap-1 text-[9px]">
                <span className="font-mono">{level.height}板</span>
                <div className="h-1.5 overflow-hidden rounded-full bg-background"><div className="h-full rounded-full bg-rose-500/65" style={{ width: `${Math.min(100, level.count * 18)}%` }} /></div>
                <span className="font-mono text-muted-foreground">{level.count}</span>
                {spacious ? <span className="col-start-2 col-end-4 truncate text-[8px] text-muted-foreground">{level.leaders.join("、") || "梯队扩散"}</span> : null}
              </div>
            ))}
          </div>
          <div className="grid content-start gap-1">
            <Metric label="当日断板" value={`${latest.failedPromotionCount} 只`} tone="warning" />
            <Metric label="近期最高" value={`${recentPeakHeight} 板`} />
            <Metric label="近期炸板" value={String(recent.reduce((sum, row) => sum + row.brokenLimitUpCount, 0))} tone="warning" />
            <Metric label="近期断板" value={String(recent.reduce((sum, row) => sum + row.failedPromotionCount, 0))} tone="warning" />
          </div>
        </div>
        <div className="mt-1.5 grid grid-cols-5 gap-1">
          {recent.map((row) => (
            <div key={row.tradeDate} className="min-w-0 bg-background/75 px-1.5 py-1 text-center">
              <div className="font-mono text-[8px] text-muted-foreground">{row.tradeDate.slice(5)}</div>
              <div className="mt-0.5 font-mono text-[9px]">{row.maxHeight}板 · {row.limitUpCount}涨停</div>
            </div>
          ))}
        </div>
        {spacious && latest.brokenStocks.length ? <div className="mt-1 truncate text-[8px] text-muted-foreground">断板标的：{latest.brokenStocks.join("、")}</div> : null}
      </section>
    </div>
  )
}

void DailyView

type RealDailySnapshot = {
  tradeDate: string
  advancingCount: number | null
  decliningCount: number | null
  flatCount: number | null
  advancingPct: number | null
  medianChangePct: number | null
  above5PctCount: number | null
  limitUpCount: number | null
  touchedLimitUpCount: number | null
  brokenLimitUpCount: number | null
  failedPromotionCount: number | null
  limitDownCount: number | null
  sealRate: number | null
  maxHeight: number | null
  promotionRate: number | null
  ladder: MockLadderLevel[]
  brokenStocks: string[]
  source?: string | null
  dataStatus?: string | null
}

type RealDailyData = {
  requestedDate: string
  effectiveDate: string
  latest: RealDailySnapshot
  rows: RealDailySnapshot[]
  partial: boolean
  error?: string
}

function percentageValue(value: number | null | undefined) {
  if (value == null || !Number.isFinite(value)) return null
  return Math.abs(value) <= 1 ? value * 100 : value
}

function summarySnapshot(item: LimitEmotionSummaryHistoryItem): RealDailySnapshot {
  const breakRate = percentageValue(item.breakBoardRate)
  return {
    tradeDate: item.tradeDate,
    advancingCount: null,
    decliningCount: null,
    flatCount: null,
    advancingPct: null,
    medianChangePct: null,
    above5PctCount: null,
    limitUpCount: item.limitUpCount,
    touchedLimitUpCount: item.touchedCount,
    brokenLimitUpCount: item.brokenCount,
    failedPromotionCount: null,
    limitDownCount: item.limitDownCount,
    sealRate: item.touchedCount ? Math.max(0, 100 - (breakRate ?? 0)) : null,
    maxHeight: null,
    promotionRate: null,
    ladder: [],
    brokenStocks: [],
    source: "market-sentiment",
  }
}

function enrichBreadth(rows: RealDailySnapshot[], breadthRows: MarketHistoryPoint[]) {
  const byDate = new Map(breadthRows.map((item) => [item.date, item]))
  return rows.map((row) => {
    const breadth = byDate.get(row.tradeDate)
    if (!breadth) return row
    const advancingCount = breadth.risingCount
    const decliningCount = breadth.fallingCount
    const flatCount = breadth.flatCount
    const total = [advancingCount, decliningCount, flatCount].every((value) => value != null)
      ? Math.max(1, (advancingCount ?? 0) + (decliningCount ?? 0) + (flatCount ?? 0))
      : null
    return { ...row, advancingCount, decliningCount, flatCount, advancingPct: total ? ((advancingCount ?? 0) / total) * 100 : null, limitUpCount: row.limitUpCount ?? breadth.limitUpCount, limitDownCount: row.limitDownCount ?? breadth.limitDownCount, source: row.source ?? breadth.source }
  })
}

function payloadSnapshot(payload: LimitEmotionPayload): RealDailySnapshot | null {
  if (!payload.tradeDate) return null
  const distribution = payload.streak?.distribution ?? []
  const grouped = new Map<number, string[]>()
  for (const stock of payload.limitUp?.stocks ?? []) {
    const height = stock.streak ?? 0
    if (height <= 0) continue
    const leaders = grouped.get(height) ?? []
    if (leaders.length < 3) leaders.push(stock.name || stock.code)
    grouped.set(height, leaders)
  }
  const ladder = (distribution.length ? distribution.map((level) => ({ height: level.height ?? level.streak, count: level.count, leaders: (level.stocks ?? []).slice(0, 3).map((stock) => stock.name || stock.code) })) : Array.from(grouped.entries()).map(([height, leaders]) => ({ height, count: leaders.length, leaders }))).filter((level) => level.height > 0 && level.count > 0).sort((a, b) => b.height - a.height)
  const breakRate = percentageValue(payload.breakBoard?.rate)
  const touched = payload.breakBoard?.touchedCount ?? payload.limitUp?.count ?? null
  const broken = payload.breakBoard?.brokenCount ?? payload.breakBoard?.brokenStocks?.length ?? null
  return {
    tradeDate: payload.tradeDate,
    advancingCount: payload.advancingCount ?? null,
    decliningCount: payload.decliningCount ?? null,
    flatCount: payload.flatCount ?? null,
    advancingPct: payload.advancingCount != null && payload.decliningCount != null && payload.flatCount != null
      ? payload.advancingCount / Math.max(1, payload.advancingCount + payload.decliningCount + payload.flatCount) * 100
      : null,
    medianChangePct: payload.medianChangePct ?? null,
    above5PctCount: payload.above5PctCount ?? null,
    limitUpCount: payload.limitUp?.count ?? null,
    touchedLimitUpCount: touched,
    brokenLimitUpCount: broken,
    failedPromotionCount: payload.streak?.broken?.count ?? null,
    limitDownCount: payload.limitDown?.count ?? null,
    sealRate: breakRate == null ? null : Math.max(0, 100 - breakRate),
    maxHeight: payload.streak?.maxHeight ?? ladder[0]?.height ?? null,
    promotionRate: percentageValue(payload.streak?.promotion?.overallRate),
    ladder,
    brokenStocks: (payload.streak?.broken?.stocks ?? payload.breakBoard?.brokenStocks ?? []).slice(0, 12).map((stock) => stock.name || stock.code),
    source: "market-pulse",
    dataStatus: payload.dataStatus,
  }
}

function formatNullable(value: number | null, suffix = "") {
  return value == null ? "—" : `${value.toFixed(1)}${suffix}`
}

function poolRate(numerator: number, denominator: number) {
  return denominator > 0 ? (numerator / denominator) * 100 : 0
}

type DailyTrendAssessment = {
  stage: RotationPhase
  headline: string
  detail: string
  signals: string[]
}

function assessDailyTrend(current: MockIndustryDailyState, previous?: MockIndustryDailyState | null): DailyTrendAssessment {
  const breadthDelta = previous ? current.advancingPct - previous.advancingPct : 0
  const breakRate = poolRate(current.brokenLimitUpCount, current.touchedLimitUpCount)
  const failedRate = poolRate(current.failedPromotionCount, current.limitUpCount + current.failedPromotionCount)
  const stage: RotationPhase = current.strengthScore >= 76 && breadthDelta >= 0
    ? "高潮"
    : current.strengthScore >= 62 && breadthDelta > 1.5
      ? "加速"
      : current.strengthScore <= 42 && breadthDelta < -1
        ? "退潮"
        : breadthDelta < -2 || breakRate >= 30 || failedRate >= 42
          ? "分歧"
          : previous && breadthDelta > 1 && current.mainNetAmountWan > 0
            ? "回流"
            : "启动"
  const stageDetail: Record<RotationPhase, string> = {
    启动: "广度开始扩张，涨停池由点到面，观察首板是否继续增厚。",
    加速: "上涨家数、连板高度和资金同步抬升，追涨拥挤度正在增加。",
    高潮: "高位梯队与封板情绪处于强势区，重点防范午后炸板和晋级分化。",
    分歧: "广度或封板率回落，高位晋级变慢，等待回流方向确认。",
    回流: "资金重新回到核心梯队，弱转强标的开始修复，但持续性仍待确认。",
    退潮: "涨跌结构转弱、断板增加，连板高度和资金承接同时收缩。",
  }
  const flow = amountWan(current.mainNetAmountWan)
  return {
    stage,
    headline: `${stage} · 强度 ${current.strengthScore.toFixed(1)} · 资金 ${flow}`,
    detail: stageDetail[stage],
    signals: [
      `广度 ${current.advancingPct.toFixed(1)}%${breadthDelta ? ` (${breadthDelta >= 0 ? "+" : ""}${breadthDelta.toFixed(1)})` : ""}`,
      `封板 ${poolRate(current.limitUpCount, current.touchedLimitUpCount).toFixed(1)}%`,
      `炸板 ${breakRate.toFixed(1)}% · 断板 ${failedRate.toFixed(1)}%`,
      `晋级 ${current.promotionRate.toFixed(1)}% · 最高 ${current.maxHeight} 板`,
    ],
  }
}

function RealDailyView({
  data,
  mockData,
  compact,
  rangeDays,
  onRangeDaysChange,
  onStartMinuteReplay,
}: {
  data: RealDailyData
  mockData: ReturnType<typeof buildIndustryMarketStateMock>
  compact: boolean
  rangeDays: number
  onRangeDaysChange: (days: number) => void
  onStartMinuteReplay: (tradeDate: string) => void
}) {
  const statusRangeDays = Math.max(5, Math.min(240, rangeDays || 60))
  const [customDays, setCustomDays] = useState(String(statusRangeDays))
  const rows = data.rows.filter((row) => row.tradeDate <= data.effectiveDate)
  const selected = rows.slice(-statusRangeDays)
  const latest = data.latest
  const mockRows = mockData.daily.filter((row) => row.tradeDate <= data.effectiveDate)
  const exactMockDayIndex = mockRows.findIndex((row) => row.tradeDate === latest.tradeDate)
  const mockDayIndex = exactMockDayIndex >= 0 ? exactMockDayIndex : mockRows.length - 1
  const currentPoolDay = mockRows[mockDayIndex] ?? null
  const currentLadderPool = currentPoolDay?.ladder ?? []
  const previousPoolDay = mockDayIndex > 0 ? mockRows[mockDayIndex - 1] : null
  const previousLimitPool = previousPoolDay?.limitUpPool ?? []
  const previousLimitDownPool = previousPoolDay?.limitDownPool ?? []
  const currentTrend = currentPoolDay ? assessDailyTrend(currentPoolDay, mockDayIndex > 0 ? mockRows[mockDayIndex - 1] : null) : null
  const rangePeakHeight = Math.max(...selected.map((row) => row.maxHeight ?? 0), 0)
  const sum = (selector: (row: RealDailySnapshot) => number | null) => selected.reduce((total, row) => total + (selector(row) ?? 0), 0)
  const avg = (selector: (row: RealDailySnapshot) => number | null) => {
    const values = selected.map(selector).filter((value): value is number => value != null)
    return values.length ? values.reduce((total, value) => total + value, 0) / values.length : null
  }
  const applyCustomDays = () => {
    const parsed = Number.parseInt(customDays, 10)
    const next = Number.isFinite(parsed) ? Math.max(5, Math.min(240, parsed)) : 60
    setCustomDays(String(next))
    onRangeDaysChange(next)
  }
  const rangeControls = (
    <div className="flex items-center rounded bg-background/80 p-0.5">
      {([20, 60, 120] as const).map((value) => (
        <button key={value} type="button" className={cn("rounded px-1 py-0.5 font-mono text-[8px] tabular-nums", statusRangeDays === value ? "bg-muted text-foreground" : "text-muted-foreground hover:text-foreground")} onClick={() => { setCustomDays(String(value)); onRangeDaysChange(value) }}>{value}日</button>
      ))}
      <span className="mx-0.5 h-3 w-px bg-border" />
      <input type="number" min={5} max={240} value={customDays} onChange={(event) => setCustomDays(event.target.value)} onBlur={applyCustomDays} onKeyDown={(event) => { if (event.key === "Enter") event.currentTarget.blur() }} className="h-4 w-10 rounded bg-muted/50 px-1 text-right font-mono text-[8px] outline-none focus-visible:ring-1 focus-visible:ring-ring/50" aria-label="自定义日级市场状态窗口" />
      <span className="pr-0.5 text-[8px] text-muted-foreground">日</span>
    </div>
  )
  const breadth = latest.advancingCount != null && latest.decliningCount != null && latest.flatCount != null
    ? { advancingCount: latest.advancingCount, decliningCount: latest.decliningCount, flatCount: latest.flatCount }
    : null

  return (
    <div className={cn("grid h-full min-h-0 gap-2", compact ? "grid-cols-2" : "grid-cols-12")}>
      <section className={cn("min-h-0 overflow-hidden rounded-md bg-muted/30 p-2", compact ? "col-span-1" : "col-span-3")}>
        <PanelHeading icon={Flame} title="当日完整生态" detail={latest.tradeDate.slice(5)} />
        {breadth ? <div className="mt-1.5"><BreadthBar snapshot={breadth} /></div> : <div className="mt-2 rounded bg-background/60 px-2 py-1 text-[9px] text-muted-foreground">涨跌家数暂不可用</div>}
        <div className="mt-1.5 grid grid-cols-3 gap-1">
          <Metric label="涨停封板" value={latest.limitUpCount == null ? "—" : String(latest.limitUpCount)} tone="up" />
          <Metric label="触及涨停" value={latest.touchedLimitUpCount == null ? "—" : String(latest.touchedLimitUpCount)} tone="up" />
          <Metric label="炸板" value={latest.brokenLimitUpCount == null ? "—" : String(latest.brokenLimitUpCount)} tone="warning" />
          <Metric label="跌停" value={latest.limitDownCount == null ? "—" : String(latest.limitDownCount)} tone={latest.limitDownCount ? "down" : undefined} />
          <Metric label="封板率" value={formatNullable(latest.sealRate, "%")} />
          <Metric label="涨幅 >5%" value={latest.above5PctCount == null ? "—" : `${latest.above5PctCount} 只`} />
          <Metric label="最高连板" value={latest.maxHeight == null ? "—" : `${latest.maxHeight} 板`} />
          <Metric label="当日断板" value={latest.failedPromotionCount == null ? "—" : `${latest.failedPromotionCount} 只`} tone="warning" />
          <Metric label="晋级率" value={formatNullable(latest.promotionRate, "%")} />
          <Metric label="炸板率" value={formatNullable(poolRate(latest.brokenLimitUpCount ?? 0, latest.touchedLimitUpCount ?? 0), "%")} tone="warning" />
          <Metric label="断板率" value={formatNullable(poolRate(latest.failedPromotionCount ?? 0, (latest.limitUpCount ?? 0) + (latest.failedPromotionCount ?? 0)), "%")} tone="warning" />
        </div>
      </section>

      <section className={cn("min-h-0 overflow-hidden rounded-md bg-muted/30 p-2", compact ? "col-span-1" : "col-span-3")}>
        <div className="flex items-center justify-between gap-2"><PanelHeading icon={Activity} title="区间统计" detail={`${selected.length} 个交易日`} />{rangeControls}</div>
        <div className="mt-1.5 grid grid-cols-4 gap-1">
          <Metric label="区间最高" value={rangePeakHeight ? `${rangePeakHeight} 板` : "—"} />
          <Metric label="涨停合计" value={String(sum((row) => row.limitUpCount))} tone="up" />
          <Metric label="触板合计" value={String(sum((row) => row.touchedLimitUpCount))} />
          <Metric label="炸板合计" value={String(sum((row) => row.brokenLimitUpCount))} tone="warning" />
          <Metric label="断板合计" value={String(sum((row) => row.failedPromotionCount))} tone="warning" />
          <Metric label="平均广度" value={formatNullable(avg((row) => row.advancingPct), "%")} />
          <Metric label="平均晋级" value={formatNullable(avg((row) => row.promotionRate), "%")} />
          <Metric label="区间日期" value={`${selected[0]?.tradeDate.slice(5) ?? "—"}–${latest.tradeDate.slice(5)}`} />
        </div>
        <button type="button" className="mt-2 flex h-6 w-full items-center justify-center gap-1 rounded bg-foreground px-2 text-[9px] font-medium text-background hover:bg-foreground/90" onClick={() => onStartMinuteReplay(latest.tradeDate)}>
          <Play className="size-3" />启动 {latest.tradeDate.slice(5)} 分钟回放
        </button>
      </section>

      <section className={cn("min-h-0 overflow-hidden rounded-md bg-muted/30 p-2", compact ? "col-span-2" : "col-span-6")}>
        <div className="flex items-center justify-between gap-2"><PanelHeading icon={Layers3} title="连板池与昨日涨跌停反馈" detail={`${latest.tradeDate.slice(5)} · 池子 Mock`} /><span className="text-[8px] text-muted-foreground">当日梯度 · 昨日反馈</span></div>
        <div className="mt-1.5 grid min-h-0 grid-cols-[0.9fr_1.1fr] gap-2">
          <div className="min-w-0 space-y-1 overflow-hidden">
            {currentLadderPool.slice(0, 5).map((level) => (
              <div key={level.height} className="grid grid-cols-[34px_minmax(0,1fr)] items-start gap-1 rounded bg-background/70 px-1.5 py-1">
                <span className="font-mono text-[9px] font-semibold text-rose-600">{level.height}板 <span className="text-[8px] font-normal text-muted-foreground">{level.count}</span></span>
                <div className="grid min-w-0 grid-cols-2 gap-1">
                  {(level.members ?? []).slice(0, 2).map((stock) => <PoolStockCard key={stock.code} stock={stock} tradeDate={latest.tradeDate} context="ladder" />)}
                  {level.count > 4 ? <span className="self-center text-[8px] text-muted-foreground">另有 {level.count - 4} 只</span> : null}
                </div>
              </div>
            ))}
            {!currentLadderPool.length ? <div className="rounded bg-background/60 px-2 py-3 text-center text-[9px] text-muted-foreground">当前交易日暂无连板池 Mock</div> : null}
            <div className="truncate px-1 text-[8px] text-muted-foreground">断板：{latest.brokenStocks.join("、") || "暂无"}</div>
          </div>
          <div className="min-w-0">
            <div className="mb-1 flex items-center justify-between gap-1 text-[8px] text-muted-foreground"><span>{previousPoolDay?.tradeDate.slice(5) ?? "昨日"} 反馈</span><span>{latest.tradeDate.slice(5)} 分时</span></div>
            <div className="grid grid-cols-2 gap-1">
              {previousLimitPool.slice(0, 2).map((stock) => <PoolStockCard key={`up-${stock.code}`} stock={stock} tradeDate={latest.tradeDate} context="limit-up" />)}
              {previousLimitDownPool.slice(0, 2).map((stock) => <PoolStockCard key={`down-${stock.code}`} stock={stock} tradeDate={latest.tradeDate} context="limit-down" />)}
            </div>
            {!previousLimitPool.length && !previousLimitDownPool.length ? <div className="flex h-20 items-center justify-center rounded bg-background/60 text-[9px] text-muted-foreground">上一交易日暂无涨跌停池</div> : null}
          </div>
        </div>
        {currentTrend ? <div className="mt-1.5 grid grid-cols-[auto_minmax(0,1fr)] items-center gap-2 rounded bg-background/70 px-1.5 py-1"><span className="rounded-full bg-information/10 px-1.5 py-0.5 text-[9px] font-semibold text-information">{currentTrend.stage}</span><div className="min-w-0"><div className="truncate text-[9px] font-medium">{currentTrend.headline}</div><div className="truncate text-[8px] text-muted-foreground">{currentTrend.detail}</div></div></div> : null}
      </section>
    </div>
  )
}

export function MarketStateReplayPanel({
  tradeDate,
  currentTimestamp,
  industryName,
  mode,
  dailyWindowDays,
  onDailyWindowDaysChange,
  onStartMinuteReplay,
  realEventCount = 0,
}: MarketStateReplayPanelProps) {
  const rootRef = useRef<HTMLDivElement | null>(null)
  const [size, setSize] = useState({ width: 0, height: 0 })
  const [viewMode, setViewMode] = useState(mode)
  const [dailyData, setDailyData] = useState<RealDailyData | null>(null)
  const [dailyLoading, setDailyLoading] = useState(false)
  const [dailyError, setDailyError] = useState<string | null>(null)
  useEffect(() => {
    const node = rootRef.current
    if (!node) return
    const observer = new ResizeObserver(([entry]) => {
      setSize({ width: entry.contentRect.width, height: entry.contentRect.height })
    })
    observer.observe(node)
    return () => observer.disconnect()
  }, [])

  const cursorDate = marketDate(currentTimestamp, tradeDate)
  useEffect(() => {
    if (viewMode !== "daily") return
    let cancelled = false
    const end = new Date(`${cursorDate}T00:00:00Z`)
    const start = new Date(end.getTime() - Math.max(90, dailyWindowDays * 2 + 30) * 86400000)
    const startDate = start.toISOString().slice(0, 10)
    const pulseRange = dailyWindowDays <= 20 ? "20d" : dailyWindowDays <= 60 ? "60d" : dailyWindowDays <= 120 ? "120d" : "1y"
    queueMicrotask(() => {
      if (!cancelled) {
        setDailyLoading(true)
        setDailyError(null)
      }
    })
    Promise.allSettled([
      fetchMarketPulseLimitEmotion(cursorDate),
      fetchMarketSentimentLimitEmotionSummaryHistory(startDate, cursorDate),
      fetchMarketPulseHistory(pulseRange),
    ]).then(([emotionResult, summaryResult, breadthResult]) => {
      if (cancelled) return
      const rawEmotion = emotionResult.status === "fulfilled" ? payloadSnapshot(emotionResult.value) : null
      const emotion = rawEmotion && rawEmotion.tradeDate <= cursorDate ? rawEmotion : null
      const summaries = summaryResult.status === "fulfilled" && summaryResult.value.ok ? summaryResult.value.items.map(summarySnapshot) : []
      const breadth = breadthResult.status === "fulfilled" && breadthResult.value.ok ? breadthResult.value.items : []
      const byDate = new Map(summaries.map((row) => [row.tradeDate, row]))
      if (emotion) byDate.set(emotion.tradeDate, emotion)
      const enriched = enrichBreadth(Array.from(byDate.values()).sort((a, b) => a.tradeDate.localeCompare(b.tradeDate)), breadth)
      const latest = emotion ?? enriched.at(-1) ?? null
      if (!latest) {
        setDailyData(null)
        setDailyError("市场状态暂无可用数据")
        setDailyLoading(false)
        return
      }
      const rows = enriched.filter((row) => row.tradeDate <= latest.tradeDate)
      setDailyData({ requestedDate: cursorDate, effectiveDate: latest.tradeDate, latest, rows, partial: !emotion || summaries.length === 0 })
      setDailyLoading(false)
    }).catch((error) => {
      if (cancelled) return
      setDailyData(null)
      setDailyError(error instanceof Error ? error.message : "加载市场状态失败")
      setDailyLoading(false)
    })
    return () => { cancelled = true }
  }, [cursorDate, dailyWindowDays, viewMode])

  const data = useMemo(
    () => buildIndustryMarketStateMock(tradeDate, industryName || "电子元件"),
    [industryName, tradeDate],
  )
  const currentIndex = Math.max(0, data.intraday.findLastIndex((item) => item.timestamp <= currentTimestamp))
  const current = data.intraday[currentIndex] ?? data.intraday[0]
  const visibleEvents = data.events.filter((event) => event.timestamp <= currentTimestamp)
  const narrow = size.width > 0 && size.width < 760
  const compact = size.width > 0 && (size.width < 760 || size.height < 165)
  const spacious = size.width >= 980 && size.height >= 235
  const showEventRail = size.height === 0 || size.height >= 145
  const latestEvent = visibleEvents.at(-1)

  return (
    <div ref={rootRef} className="h-full min-h-0 overflow-hidden p-2">
      <div className="grid h-full min-h-0 grid-rows-[auto_minmax(0,1fr)_auto] gap-1.5">
        <header className="flex min-w-0 items-center justify-between gap-3 px-1 text-[10px]">
          <div className="flex min-w-0 items-center gap-2">
            <span className="truncate font-semibold">{data.industryName}</span>
            <div className="flex shrink-0 items-center rounded bg-muted/60 p-0.5">
              <button
                type="button"
                className={cn("rounded px-1.5 py-0.5 text-[9px] font-medium transition-colors", viewMode === "intraday" ? "bg-background text-information" : "text-muted-foreground hover:text-foreground")}
                onClick={() => setViewMode("intraday")}
              >
                分钟联动
              </button>
              <button
                type="button"
                className={cn("rounded px-1.5 py-0.5 text-[9px] font-medium transition-colors", viewMode === "daily" ? "bg-background text-foreground" : "text-muted-foreground hover:text-foreground")}
                onClick={() => setViewMode("daily")}
              >
                日级历史
              </button>
            </div>
            {viewMode === "daily" ? <span className="shrink-0 rounded bg-muted px-1.5 py-0.5 text-[9px] font-medium text-muted-foreground">{dailyData?.partial ? "部分数据" : "真实数据"}</span> : null}
            {viewMode === "intraday" ? <span className="shrink-0 text-muted-foreground">{current.phase} · {current.timeLabel}</span> : null}
          </div>
          <div className="hidden shrink-0 items-center gap-2 text-muted-foreground min-[720px]:flex">
            <span>THS {data.coverage.thsCount}</span><span>TDX {data.coverage.tdxCount}</span><span>并集 {data.coverage.unionCount}</span><span>重合 {data.coverage.intersectionCount}</span>
          </div>
        </header>

        <div className="min-h-0 overflow-hidden">
          {viewMode === "intraday" ? (
            <IntradayView data={data} current={current} visibleCount={currentIndex + 1} compact={compact} spacious={spacious} />
          ) : dailyLoading ? (
            <div className="grid h-full min-h-0 grid-cols-2 gap-2"><div className="animate-pulse rounded-md bg-muted/40" /><div className="animate-pulse rounded-md bg-muted/40" /><div className="animate-pulse rounded-md bg-muted/40" /><div className="animate-pulse rounded-md bg-muted/40" /></div>
          ) : dailyData ? (
            <RealDailyView data={dailyData} mockData={data} compact={narrow} rangeDays={dailyWindowDays} onRangeDaysChange={onDailyWindowDaysChange} onStartMinuteReplay={onStartMinuteReplay} />
          ) : (
            <div className="flex h-full items-center justify-center rounded-md bg-muted/30 px-3 text-xs text-muted-foreground">{dailyError ?? "暂无市场状态数据"}</div>
          )}
        </div>

        {showEventRail ? (
          <footer className="flex min-w-0 items-center gap-2 overflow-hidden rounded bg-muted/25 px-2 py-1 text-[9px]">
            <Clock3 className="size-3 shrink-0 text-muted-foreground" aria-hidden="true" />
            {viewMode === "intraday" && latestEvent ? (
              <><span className="shrink-0 font-mono text-muted-foreground">{latestEvent.timeLabel}</span><span className={cn("truncate", latestEvent.tone === "up" && "text-rose-600", latestEvent.tone === "down" && "text-emerald-600", latestEvent.tone === "warning" && "text-amber-600")}>{latestEvent.title}</span></>
            ) : viewMode === "daily" ? (
              <span className="truncate text-muted-foreground">日级游标 {marketDate(currentTimestamp, tradeDate)} · 最近 {dailyWindowDays} 个交易日窗口</span>
            ) : (
              <span className="truncate text-muted-foreground">等待游标进入首个行业状态事件</span>
            )}
            <span className="ml-auto shrink-0 text-muted-foreground">{viewMode === "daily" ? `${dailyData?.effectiveDate ?? cursorDate} · ${dailyData?.partial ? "部分数据" : "market pulse"}` : `Indicator ${realEventCount}`}</span>
            {current.relativeChangePct >= 0 ? <ArrowUpRight className="size-3 shrink-0 text-rose-600" /> : <ArrowDownRight className="size-3 shrink-0 text-emerald-600" />}
          </footer>
        ) : null}
      </div>
    </div>
  )
}

/**
 * Compact summary for the chart's lower slot. The complete market-state
 * workspace lives in the right-side "市场状态" tab; the slot only surfaces
 * the cursor's decision-relevant snapshot so it remains readable at small
 * heights.
 */
export function MarketStateReplaySlot({
  tradeDate,
  currentTimestamp,
  industryName,
  mode = "intraday",
  realEventCount = 0,
}: {
  tradeDate: string
  currentTimestamp: number
  industryName?: string | null
  mode?: "intraday" | "daily"
  realEventCount?: number
}) {
  const data = useMemo(
    () => buildIndustryMarketStateMock(tradeDate, industryName || "电子元件"),
    [industryName, tradeDate],
  )
  const cursorDate = marketDate(currentTimestamp, tradeDate)
  const intradayIndex = Math.max(0, data.intraday.findLastIndex((item) => item.timestamp <= currentTimestamp))
  const intraday = data.intraday[intradayIndex] ?? data.intraday[0]
  const daily = data.daily.filter((item) => item.tradeDate <= cursorDate).at(-1) ?? data.daily.at(-1)!
  const isDaily = mode === "daily"
  const lastNotifiedEventKey = useRef<string | null>(null)
  const phase = isDaily ? "收盘快照" : intraday.phase
  const timeLabel = isDaily ? daily.tradeDate.slice(5) : intraday.timeLabel
  const advancing = isDaily ? daily.advancingCount : intraday.advancingCount
  const declining = isDaily ? daily.decliningCount : intraday.decliningCount
  const flat = isDaily ? daily.flatCount : intraday.flatCount
  const limitUp = daily.limitUpCount
  const touched = daily.touchedLimitUpCount
  const broken = daily.brokenLimitUpCount
  const limitDown = daily.limitDownCount
  const maxHeight = daily.maxHeight
  const promotionRate = daily.promotionRate
  const medianChange = isDaily ? daily.medianChangePct : intraday.medianChangePct
  const latestEvent = data.events.filter((event) => event.timestamp <= currentTimestamp).at(-1)
  const total = Math.max(1, advancing + declining + flat)
  const dailyIndex = data.daily.findIndex((item) => item.tradeDate === daily.tradeDate)
  const previousDaily = dailyIndex > 0 ? data.daily[dailyIndex - 1] : null
  const dailyTrend = isDaily ? assessDailyTrend(daily, previousDaily) : null
  const previousLimitUpPool = previousDaily?.limitUpPool ?? []
  const previousLimitDownPool = previousDaily?.limitDownPool ?? []

  useEffect(() => {
    if (!isDaily || !dailyTrend) return
    const eventKey = `daily:${daily.tradeDate}:${dailyTrend.stage}`
    if (lastNotifiedEventKey.current === eventKey) return
    lastNotifiedEventKey.current = eventKey
    notification.info({
      title: `${daily.tradeDate.slice(5)} 日级回放 · ${dailyTrend.stage}`,
      description: `${dailyTrend.headline}。${dailyTrend.detail}`,
      placement: "bottom-right",
      duration: 4200,
    })
  }, [daily.tradeDate, dailyTrend, isDaily])

  if (isDaily) {
    const ladder = daily.ladder.slice(0, 4)
    const feedback = [...previousLimitUpPool.slice(0, 2), ...previousLimitDownPool.slice(0, 2)]
    return (
      <div className="flex h-full min-h-0 flex-col gap-2 overflow-hidden px-3 py-2">
        <div className="flex min-w-0 items-center justify-between gap-3 text-[10px]">
          <div className="flex min-w-0 items-center gap-2">
            <span className="shrink-0 font-semibold">市场状态</span>
            <span className="truncate text-muted-foreground">{data.industryName} · 日级生态</span>
          </div>
          <div className="flex shrink-0 items-center gap-2 text-muted-foreground">
            <span className="rounded bg-information/10 px-1.5 py-0.5 font-medium text-information">{dailyTrend?.stage ?? "—"}</span>
            <span className="font-mono tabular-nums">{daily.tradeDate}</span>
          </div>
        </div>

        <div className="grid min-h-0 flex-1 grid-cols-2 gap-2 md:grid-cols-12">
          <section className="min-w-0 overflow-hidden bg-muted/30 px-2.5 py-2 md:col-span-3">
            <div className="text-[9px] text-muted-foreground">当日 overview</div>
            <div className="mt-1.5 flex h-1.5 overflow-hidden rounded-full bg-muted">
              <div className="bg-rose-500/75" style={{ width: `${advancing / total * 100}%` }} />
              <div className="bg-muted-foreground/25" style={{ width: `${flat / total * 100}%` }} />
              <div className="bg-emerald-500/75" style={{ width: `${declining / total * 100}%` }} />
            </div>
            <div className="mt-1 grid grid-cols-3 gap-1 font-mono text-[8px] tabular-nums">
              <span className="text-rose-600">涨 {advancing}</span><span className="text-muted-foreground">平 {flat}</span><span className="text-emerald-600">跌 {declining}</span>
            </div>
            <div className="mt-1.5 grid grid-cols-3 gap-1">
              <Metric label="涨停" value={String(limitUp)} tone="up" />
              <Metric label="触板" value={String(touched)} tone="up" />
              <Metric label="炸板" value={String(broken)} tone="warning" />
              <Metric label="跌停" value={String(limitDown)} tone={limitDown ? "down" : undefined} />
              <Metric label="封板率" value={`${poolRate(limitUp, touched).toFixed(1)}%`} />
              <Metric label="晋级率" value={`${promotionRate.toFixed(1)}%`} />
              <Metric label="炸板率" value={`${poolRate(broken, touched).toFixed(1)}%`} tone="warning" />
              <Metric label="断板率" value={`${poolRate(daily.failedPromotionCount, daily.limitUpCount + daily.failedPromotionCount).toFixed(1)}%`} tone="warning" />
              <Metric label="最高" value={`${maxHeight}板`} />
            </div>
          </section>

          <section className="min-w-0 overflow-hidden bg-muted/30 px-2.5 py-2 md:col-span-5">
            <div className="flex items-center justify-between gap-2"><span className="text-[9px] text-muted-foreground">连板池 · 梯度</span><span className="font-mono text-[8px] text-muted-foreground">最高 {maxHeight}板</span></div>
            <div className="mt-1 grid min-h-0 grid-cols-2 gap-1">
              {ladder.map((level) => (
                <div key={level.height} className="min-w-0 bg-background/60 px-1.5 py-1">
                  <div className="flex items-center justify-between gap-1"><span className="font-mono text-[9px] font-semibold text-rose-600">{level.height}板</span><span className="font-mono text-[8px] text-muted-foreground">{level.count}只</span></div>
                  <div className="mt-0.5 grid grid-cols-2 gap-1">{(level.members ?? []).slice(0, 2).map((stock) => <PoolStockCard key={stock.code} stock={stock} tradeDate={daily.tradeDate} context="ladder" />)}</div>
                </div>
              ))}
            </div>
          </section>

          <section className="min-w-0 overflow-hidden bg-muted/30 px-2.5 py-2 md:col-span-4">
            <div className="flex items-center justify-between gap-2"><span className="text-[9px] text-muted-foreground">昨日涨停 / 跌停反馈</span><span className="text-[8px] text-muted-foreground">{previousDaily?.tradeDate.slice(5) ?? "昨日"}</span></div>
            <div className="mt-1 grid grid-cols-2 gap-1">{feedback.map((stock) => <PoolStockCard key={stock.code} stock={stock} tradeDate={daily.tradeDate} context={stock.height === 0 ? "limit-down" : "limit-up"} />)}</div>
            {!feedback.length ? <div className="mt-2 text-[9px] text-muted-foreground">昨日反馈池暂无可用标的</div> : null}
            <div className="mt-1 truncate text-[8px] text-muted-foreground">断板：{daily.brokenStocks.join("、") || "暂无"}</div>
          </section>
        </div>

        {dailyTrend ? <div className="grid min-w-0 grid-cols-[auto_minmax(0,1fr)] items-center gap-2 bg-muted/30 px-2.5 py-1.5">
          <span className="rounded-full bg-information/10 px-1.5 py-0.5 text-[9px] font-semibold text-information">{dailyTrend.stage}</span>
          <div className="min-w-0"><div className="truncate text-[9px] font-medium">{dailyTrend.headline}</div><div className="truncate text-[8px] text-muted-foreground">{dailyTrend.detail} · {dailyTrend.signals.slice(0, 2).join(" · ")}</div></div>
        </div> : null}
      </div>
    )
  }

  return (
    <div className="flex h-full min-h-0 flex-col gap-2 overflow-hidden px-3 py-2">
      <div className="flex min-w-0 items-center justify-between gap-3 text-[10px]">
        <div className="flex min-w-0 items-center gap-2">
          <span className="shrink-0 font-semibold">市场状态</span>
          <span className="truncate text-muted-foreground">{data.industryName}</span>
        </div>
        <div className="flex shrink-0 items-center gap-2 text-muted-foreground">
          <span>{phase}</span>
          <span className="font-mono tabular-nums">{timeLabel}</span>
        </div>
      </div>

      <div className="grid min-h-0 flex-1 grid-cols-2 gap-2 lg:grid-cols-4">
        <section className="min-w-0 overflow-hidden bg-muted/30 px-2.5 py-2">
          <div className="text-[9px] text-muted-foreground">市场广度</div>
          <div className="mt-1 flex h-1.5 overflow-hidden rounded-full bg-muted">
            <div className="bg-rose-500/75" style={{ width: `${advancing / total * 100}%` }} />
            <div className="bg-muted-foreground/25" style={{ width: `${flat / total * 100}%` }} />
            <div className="bg-emerald-500/75" style={{ width: `${declining / total * 100}%` }} />
          </div>
          <div className="mt-1 grid grid-cols-3 gap-1 font-mono text-[10px] tabular-nums">
            <span className="text-rose-600">涨 {advancing}</span>
            <span className="text-muted-foreground">平 {flat}</span>
            <span className="text-emerald-600">跌 {declining}</span>
          </div>
        </section>

        <section className="min-w-0 overflow-hidden bg-muted/30 px-2.5 py-2">
          <div className="text-[9px] text-muted-foreground">涨跌停生态</div>
          <div className="mt-1 grid grid-cols-2 gap-x-2 gap-y-1 font-mono text-[10px] tabular-nums">
            <span className="text-rose-600">涨停 {limitUp}</span>
            <span className="text-rose-600">触板 {touched}</span>
            <span className="text-amber-600">炸板 {broken}</span>
            <span className="text-emerald-600">跌停 {limitDown}</span>
          </div>
        </section>

        <section className="min-w-0 overflow-hidden bg-muted/30 px-2.5 py-2">
          <div className="text-[9px] text-muted-foreground">连板结构</div>
          <div className="mt-1 flex items-end justify-between gap-2">
            <span className="font-mono text-lg font-semibold tabular-nums">{maxHeight}<span className="ml-0.5 text-[9px] font-normal">板</span></span>
            <span className="text-right font-mono text-[10px] text-muted-foreground tabular-nums">晋级 {promotionRate.toFixed(1)}%</span>
          </div>
          <div className={cn("mt-1 font-mono text-[10px] tabular-nums", medianChange >= 0 ? "text-rose-600" : "text-emerald-600")}>
            中位涨幅 {signed(medianChange)}
          </div>
        </section>

        <section className="min-w-0 overflow-hidden bg-muted/30 px-2.5 py-2">
          <div className="text-[9px] text-muted-foreground">回放事件</div>
          <div className="mt-1 truncate text-[10px] font-medium">{latestEvent?.title ?? "暂无事件"}</div>
          <div className="mt-1 flex items-center justify-between gap-2 text-[9px] text-muted-foreground">
            <span className="truncate">{latestEvent?.timeLabel ?? "等待游标"}</span>
            <span className="shrink-0 font-mono tabular-nums">Indicator {realEventCount}</span>
          </div>
        </section>
      </div>
    </div>
  )
}
