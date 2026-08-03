import { useCallback, useEffect, useMemo, useState } from "react"
import { fetchPortfolioPerformance } from "@/services/market/portfolio/portfolio"
import type { PortfolioPerformance, PortfolioPerformanceRange } from "@/services/market/portfolio/types"

export interface PortfolioPerformanceState {
  range: PortfolioPerformanceRange
  setRange: (range: PortfolioPerformanceRange) => void
  periodLabel: string | null
  canGoPrevious: boolean
  canGoNext: boolean
  goPrevious: () => void
  goNext: () => void
  performance: PortfolioPerformance | null
  loading: boolean
  error: string | null
}

export function usePortfolioPerformance(): PortfolioPerformanceState {
  const [range, setRange] = useState<PortfolioPerformanceRange>("daily")
  const [periodOffset, setPeriodOffset] = useState(0)
  const [performance, setPerformance] = useState<PortfolioPerformance | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const loadPerformance = useCallback(async (cancelledRef: { current: boolean }) => {
    setLoading(true)
    setError(null)
    try {
      const window = buildPeriodWindow(range, periodOffset)
      const result = await fetchPortfolioPerformance(range, window?.from, window?.to)
      if (!cancelledRef.current) setPerformance(result)
    } catch (cause) {
      if (!cancelledRef.current) setError(cause instanceof Error ? cause.message : String(cause))
    } finally {
      if (!cancelledRef.current) setLoading(false)
    }
  }, [periodOffset, range])

  useEffect(() => {
    const cancelledRef = { current: false }
    const timer = window.setTimeout(() => { void loadPerformance(cancelledRef) }, 0)
    return () => { cancelledRef.current = true; window.clearTimeout(timer) }
  }, [loadPerformance])

  const periodWindow = useMemo(() => buildPeriodWindow(range, periodOffset), [periodOffset, range])
  const updateRange = useCallback((nextRange: PortfolioPerformanceRange) => {
    setPeriodOffset(0)
    setRange(nextRange)
  }, [])

  return {
    range,
    setRange: updateRange,
    periodLabel: periodWindow?.label ?? null,
    canGoPrevious: range !== "daily" && (!periodWindow || !performance || performance.from <= periodWindow.from),
    canGoNext: range !== "daily" && periodOffset < 0,
    goPrevious: () => setPeriodOffset((value) => value - 1),
    goNext: () => setPeriodOffset((value) => Math.min(value + 1, 0)),
    performance,
    loading,
    error,
  }
}

const PERIOD_WINDOW_SIZE: Record<Exclude<PortfolioPerformanceRange, "daily" | "custom">, number> = {
  weekly: 12,
  monthly: 12,
  yearly: 5,
}

function buildPeriodWindow(range: PortfolioPerformanceRange, offset: number) {
  if (range === "daily" || range === "custom") return null
  const today = new Date()
  const currentStart = shiftPeriod(startOfPeriod(today, range), range, offset)
  const windowStart = shiftPeriod(currentStart, range, -(PERIOD_WINDOW_SIZE[range] - 1))
  const windowEnd = new Date(shiftPeriod(currentStart, range, 1))
  windowEnd.setDate(windowEnd.getDate() - 1)
  return {
    from: toDateString(windowStart),
    to: toDateString(windowEnd),
    label: periodLabel(range === "weekly" ? shiftPeriod(new Date(), range, offset) : currentStart, range),
  }
}

function startOfPeriod(value: Date, range: Exclude<PortfolioPerformanceRange, "daily" | "custom">) {
  const result = new Date(value.getFullYear(), value.getMonth(), value.getDate())
  if (range === "weekly") {
    const mondayOffset = (result.getDay() + 6) % 7
    result.setDate(result.getDate() - mondayOffset)
  } else if (range === "monthly") {
    result.setDate(1)
  } else {
    result.setMonth(0, 1)
  }
  return result
}

function shiftPeriod(value: Date, range: Exclude<PortfolioPerformanceRange, "daily" | "custom">, amount: number) {
  const result = new Date(value)
  if (range === "weekly") result.setDate(result.getDate() + amount * 7)
  if (range === "monthly") result.setMonth(result.getMonth() + amount, 1)
  if (range === "yearly") result.setFullYear(result.getFullYear() + amount, 0, 1)
  return result
}

function toDateString(value: Date) {
  return `${value.getFullYear()}-${String(value.getMonth() + 1).padStart(2, "0")}-${String(value.getDate()).padStart(2, "0")}`
}

function periodLabel(value: Date, range: Exclude<PortfolioPerformanceRange, "daily" | "custom">) {
  if (range === "weekly") {
    const end = new Date(value)
    end.setDate(end.getDate() + 6)
    return `${compactDate(value)}-${compactDate(end)}`
  }
  if (range === "monthly") return `${value.getFullYear()}年${String(value.getMonth() + 1).padStart(2, "0")}月`
  return `${value.getFullYear()}年`
}

function compactDate(value: Date) {
  return `${String(value.getMonth() + 1).padStart(2, "0")}${String(value.getDate()).padStart(2, "0")}`
}
