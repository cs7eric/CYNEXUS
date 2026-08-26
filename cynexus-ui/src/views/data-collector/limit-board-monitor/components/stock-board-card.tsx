import { useEffect, useRef, useState } from "react"

import { Skeleton } from "@/components/ui/skeleton"
import type { LimitBoardSessionSummary } from "@/services/market/indicator"

import { amount, calendarDay, duration, parseTrendFeatures, percent, sealedTurnoverRatio, sessionAxisRange, sessionMinute, time, trendLabel, trendPoints } from "../lib/format"
import { loadTimeshare, type TimesharePoint } from "../lib/timeshare"
import { OverlayTimeshareChart } from "./overlay-timeshare-chart"

function useEnteredViewport() {
  const ref = useRef<HTMLElement>(null)
  const [visible, setVisible] = useState(false)

  useEffect(() => {
    const node = ref.current
    if (!node || visible) return
    const observer = new IntersectionObserver(([entry]) => {
      if (entry.isIntersecting) setVisible(true)
    }, { rootMargin: "280px 0px" })
    observer.observe(node)
    return () => observer.disconnect()
  }, [visible])

  return { ref, visible }
}

function useTimeshare(code: string, tradeDate: string, enabled: boolean) {
  const [points, setPoints] = useState<TimesharePoint[] | null>(null)

  useEffect(() => {
    if (!enabled || !code || !tradeDate) return
    let active = true
    void loadTimeshare(code, tradeDate)
      .then((next) => { if (active) setPoints(next) })
      .catch(() => { if (active) setPoints([]) })
    return () => { active = false }
  }, [code, enabled, tradeDate])

  return { points, loading: enabled && points == null }
}

function Metric({ label, value }: { label: string; value: string }) {
  return (
    <div className="min-w-0">
      <div className="text-[10px] text-muted-foreground">{label}</div>
      <div className="mt-0.5 truncate font-mono text-[11px] font-medium tabular-nums text-foreground">{value}</div>
    </div>
  )
}

export function StockBoardCard({
  row,
  tradeDate,
}: {
  row: LimitBoardSessionSummary
  tradeDate: string
}) {
  const features = parseTrendFeatures(row.trend_features)
  const sealed = trendPoints(row)
  const day = calendarDay(row.trade_date) || tradeDate
  const axis = sessionAxisRange(row.trade_session)
  const partial = Boolean(features.duration_is_partial || (row.gap_count ?? 0) > 0)
  const down = row.direction === "down"
  const { ref, visible } = useEnteredViewport()
  const { points, loading } = useTimeshare(row.code, day, visible)
  const timeshare = (points ?? []).filter((point) => {
    const minute = sessionMinute(point.at)
    return minute >= axis.min && minute <= axis.max
  })
  const closeLabel = row.is_sealed_at_close ? "收盘封板" : row.closing_status === "opened" ? "曾封后开板" : "触板"
  const hasChart = timeshare.length > 0 || sealed.length > 0

  return (
    <article ref={ref} className="flex min-w-0 flex-col rounded-lg border border-border/60 bg-card p-3">
      <div className="flex items-start justify-between gap-2">
        <div className="min-w-0">
          <h3 className="truncate text-sm font-semibold">{row.name || row.code}</h3>
          <div className="mt-0.5 font-mono text-[11px] tabular-nums text-muted-foreground">{row.code} {row.market || ""}</div>
        </div>
        <div className="flex shrink-0 flex-wrap justify-end gap-1">
          <span className={down
            ? "rounded-full bg-emerald-50 px-1.5 py-0.5 text-[10px] text-emerald-700 dark:bg-emerald-950/40 dark:text-emerald-300"
            : "rounded-full bg-rose-50 px-1.5 py-0.5 text-[10px] text-rose-700 dark:bg-rose-950/40 dark:text-rose-300"}
          >
            {down ? "跌停" : "涨停"}
          </span>
          <span className="rounded-full bg-muted px-1.5 py-0.5 text-[10px] text-muted-foreground">{closeLabel}</span>
        </div>
      </div>

      <div className="mt-3 grid grid-cols-2 gap-2">
        <Metric label="最大封单" value={amount(row.max_sealed_amount)} />
        <Metric label="封成比" value={percent(sealedTurnoverRatio(row))} />
        <Metric label="封板时长" value={duration(row.sealed_duration_seconds)} />
        <Metric label="开板 / 回封" value={`${row.open_count ?? 0}/${row.reseal_count ?? 0}`} />
      </div>

      <div className="mt-3 min-w-0 rounded-md bg-muted/25 px-1.5 pb-1 pt-1.5">
        <div className="flex items-center justify-between px-1 text-[10px] text-muted-foreground">
          <span>分时 + 封单额</span>
          <span className="flex items-center gap-2">
            <span className="inline-flex items-center gap-1">
              <span className={down ? "inline-block h-0.5 w-2.5 bg-emerald-600" : "inline-block h-0.5 w-2.5 bg-rose-600"} />
              分时
            </span>
            <span className="inline-flex items-center gap-1">
              <span className="inline-block h-0.5 w-2.5 bg-information" />
              封单
            </span>
          </span>
        </div>
        {!visible || loading ? (
          <Skeleton className="mt-1 h-[148px] w-full" />
        ) : !hasChart ? (
          <div className="flex h-[148px] items-center justify-center text-xs text-muted-foreground">
            暂无分时或封单额采样
          </div>
        ) : (
          <OverlayTimeshareChart
            timeshare={timeshare}
            sealed={sealed}
            session={row.trade_session}
            down={down}
            label={row.name || row.code}
          />
        )}
      </div>

      <div className="mt-2 flex flex-wrap items-center gap-x-3 gap-y-1 text-[11px] text-muted-foreground">
        <span className="text-foreground">{trendLabel(row)}</span>
        <span>成交 {amount(row.last_turnover_amount)}</span>
        <span>覆盖 {percent(row.coverage_ratio)}</span>
        <span>峰值 {time(row.peak_sealed_amount_at)}</span>
        {partial ? <span className="text-amber-700 dark:text-amber-300">观测缺口</span> : null}
      </div>
    </article>
  )
}
